/*
 * Copyright (C) 2020 Wigo Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wigoai.nipa.regional.service.controller.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.wigoai.nipa.regional.service.NipaRegionalAnalysis;
import com.wigoai.nipa.regional.service.ServiceConfig;
import com.wigoai.nipa.regional.service.channel.ChannelGroup;
import com.wigoai.nipa.regional.service.util.GroupKeyUtil;
import com.wigoai.nipa.regional.service.util.parameterUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.moara.common.callback.ObjectCallback;
import org.moara.common.config.Config;
import org.moara.common.time.TimeUtil;
import org.moara.common.util.ExceptionUtil;
import org.moara.common.util.YmdUtil;
import org.moara.keyword.KeywordAnalysis;
import org.moara.keyword.ServiceKeywordAnalysis;
import org.moara.message.disposable.DisposableMessage;
import org.moara.message.disposable.DisposableMessageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 통합분석
 * 분야별 분석
 * api version 1
 * @author macle
 */
@RestController
public class IntegratedAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(IntegratedAnalysisController.class);

    /**
     * 통합분석 초기 결과 얻기
     * @param jsonValue String json object
     * @return String json object
     */
    @RequestMapping(value = "/nipars/v1/integrated/analysis" , method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public String analysis(@RequestBody final String jsonValue) {

        try {

            long analysisStartTime = System.currentTimeMillis();

            JSONObject request = new JSONObject(jsonValue);

            long startTime = request.getLong("start_time");
            long endTime = request.getLong("end_time");
            long standardTime = request.getLong("standard_time");

            long analysisMaxTime = Config.getLong(ServiceConfig.ANALYSIS_MAX_TIME.key(), (long)ServiceConfig.ANALYSIS_MAX_TIME.defaultValue());

            final Thread currentThread = Thread.currentThread();

            AtomicBoolean isAnalysis = new AtomicBoolean(false);

            ObjectCallback endCallback = obj -> {

                try {
                    isAnalysis.set(true);
                    currentThread.interrupt();
                }catch(Exception e){
                    logger.error(ExceptionUtil.getStackTrace(e));
                }
            };

            int keywordCount;
            if(request.has("keyword_count")){
                keywordCount = request.getInt("keyword_count");
            }else{
                keywordCount = 30;
            }


            final KeywordAnalysis.Module [] modules = new KeywordAnalysis.Module[3];
            modules[0] = KeywordAnalysis.Module.TF_CONTENTS;
            modules[1] = KeywordAnalysis.Module.TF_CLASSIFY;
            modules[2] = KeywordAnalysis.Module.TF_CLASSIFY_TARGET;

            ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
            KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();

            NipaRegionalAnalysis nipaRegionalAnalysis = NipaRegionalAnalysis.getInstance();
            ChannelGroup[] groups = nipaRegionalAnalysis.getGroups();

            String startYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(startTime));
            String endYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(endTime-1));

            //날짜정보
            List<String> ymdList = YmdUtil.getYmdList(startYmd,endYmd);
            String [][] keysArray = GroupKeyUtil.makeKeysArray(ymdList, groups);


            Map<KeywordAnalysis.Module,Properties> moduleProperties = new HashMap<>();
            Properties properties = new Properties();
            StringBuilder channelIdBuilder = new StringBuilder();
            for(ChannelGroup channelGroup : groups){
                channelIdBuilder.append(",").append(channelGroup.getId());
            }
            properties.put("channels", channelIdBuilder.substring(1));
            moduleProperties.put(KeywordAnalysis.Module.TF_CONTENTS, properties);


            String [] emotionCodes = nipaRegionalAnalysis.getEmotionCodes();

            StringBuilder emotionBuilder = new StringBuilder();
            for(String emotionCode : emotionCodes){
                emotionBuilder.append(",").append(emotionCode);
            }

            properties = new Properties();
            properties.put("in_codes", emotionBuilder.substring(1));
            properties.put("is_trend", false);
            moduleProperties.put(KeywordAnalysis.Module.TF_CLASSIFY, properties);



            StringBuilder sourceBuilder =new StringBuilder();
            if(request.has("classify_names") ){
                JSONArray array = request.getJSONArray("classify_names");
                for (int i = 0; i <array.length() ; i++) {
                    String code = nipaRegionalAnalysis.getFieldCode(array.getString(i));
                    if(code == null){
                        logger.error("field classify code search fail: " + array.getString(i));
                        continue;
                    }
                    sourceBuilder.append(",").append(code);
                }
            }else{
                String [] fieldCodes = nipaRegionalAnalysis.getFieldCodes();
                for(String code : fieldCodes){
                    sourceBuilder.append(",").append(code);
                }

            }


            properties = new Properties();
            properties.put("source_codes", sourceBuilder.substring(1));
            properties.put("target_codes", emotionBuilder.substring(1));
            moduleProperties.put(KeywordAnalysis.Module.TF_CLASSIFY_TARGET, properties);


            String keywordJson = request.getJSONArray("keywords").toString();

            Map<String, Object> parameterMap = parameterUtil.makeParameterMap(request);

            String messageId = keywordAnalysis.keywordAnalysis(startTime, endTime, standardTime, keywordJson, keysArray, modules, moduleProperties, parameterMap, endCallback);

            try {
                long analysisTime = System.currentTimeMillis() - analysisStartTime;
                //최대 대기 시간
                Thread.sleep(analysisMaxTime - analysisTime);
            }catch (InterruptedException ignore){}

            if(!isAnalysis.get()){
                logger.error("time out: " + jsonValue);
                return "{}";
            }

            DisposableMessageManager disposableMessageManager = DisposableMessageManager.getInstance();
            String responseMessage =  disposableMessageManager.getMessages(messageId);

            JSONObject responseObj = new JSONObject(responseMessage);
            JSONArray messageArray =  responseObj.getJSONArray("messages");
            JSONObject resultObj = new JSONObject();

            for (int i = 0; i <messageArray.length() ; i++) {

                JSONObject messageObj = new JSONObject(messageArray.getString(i));
                KeywordAnalysis.Module module = KeywordAnalysis.Module.valueOf(messageObj.get("type").toString());
                if (module == KeywordAnalysis.Module.TF_CONTENTS) {
                    resultObj.put("channel_count", messageObj.getJSONObject("message"));
                }else if(module == KeywordAnalysis.Module.TF_CLASSIFY_TARGET){
                    resultObj.put("field_emotion_classifies", messageObj.getJSONArray("message"));
                } else {
                    resultObj.put("emotion_classifies", messageObj.getJSONObject("message").getJSONArray("classifies"));
                }
            }

            //2번째 결과 호출
            isAnalysis.set(false);

            keyword(resultObj, endCallback, groups, 0, startTime, endTime, standardTime, keywordJson, parameterMap, keywordAnalysis, ymdList, sourceBuilder.substring(1), keywordCount);


            try {
                long analysisTime = System.currentTimeMillis() - analysisStartTime;
                //최대 대기 시간
                Thread.sleep(analysisMaxTime - analysisTime);
            }catch (InterruptedException ignore){}

            if(!isAnalysis.get()){
                logger.error("time out: " + jsonValue);
                return "{}";
            }

            //파싱 및 변환에 대한 속도차이는 없는것으로 보임
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String result =  gson.toJson(gson.fromJson(resultObj.toString(), JsonObject.class));
            logger.debug("analysis second: " + jsonValue +":  "+ TimeUtil.getSecond(System.currentTimeMillis() - analysisStartTime));
            return result;

        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
            return "{}";
        }

    }

    private void keyword(final JSONObject resultObj, final ObjectCallback callback, final ChannelGroup[] groups, final int groupIndex
            , final long startTime, final long endTime, final long standardTime, final String keywordJson,  Map<String, Object> parameterMap, final KeywordAnalysis keywordAnalysis, final List<String> ymdList
            , final String inCodesValue
            , final int keywordCount
    ){

        ObjectCallback endCallback  = obj -> {

            try {

                if(obj == null){
                    //검색 결과가 없을때
//                    resultObj.put(groups[groupIndex].getId() + "_classifies", "[]");
//                    resultObj.put(groups[groupIndex].getId() + "_keywords", "[]");
//                    logger.error("keyword analysis fail");

                    if(groupIndex >= groups.length-1){
                        callback.callback(null);
                        return;
                    }
                    keyword(resultObj, callback, groups, groupIndex +1, startTime, endTime, standardTime, keywordJson, parameterMap, keywordAnalysis, ymdList, inCodesValue, keywordCount);
                    return ;
                }

                DisposableMessage disposableMessage = (DisposableMessage)obj;
                String [] messages = disposableMessage.getMessages();

                for(String message : messages){
                    JSONObject messageObj = new JSONObject(message);
                    KeywordAnalysis.Module module = KeywordAnalysis.Module.valueOf(messageObj.get("type").toString());
                    if (module == KeywordAnalysis.Module.TF_CLASSIFY) {
                        resultObj.put(groups[groupIndex].getId() + "_classifies", messageObj.getJSONObject("message").getJSONArray("classifies"));
                    } else {
                        resultObj.put(groups[groupIndex].getId() +"_keywords", messageObj.getJSONObject("message"));
                    }
                }
                if(groupIndex >= groups.length-1){
                    callback.callback(null);
                    return;
                }

                keyword(resultObj, callback, groups, groupIndex +1, startTime, endTime, standardTime, keywordJson, parameterMap, keywordAnalysis, ymdList, inCodesValue, keywordCount);
            }catch(Exception e){

                if(obj == null){
                    logger.error("keyword analysis fail");
                    callback.callback(null);
                    return ;
                }
                logger.error(ExceptionUtil.getStackTrace(e));
            }
        };

        //그룹 정보 변경
        String [][] keysArray = GroupKeyUtil.makeKeysArray(ymdList,  groups[groupIndex].getId());

        KeywordAnalysis.Module [] modules;
        modules = new KeywordAnalysis.Module[2];
        modules[0] = KeywordAnalysis.Module.TF_WORD_CONTENTS;
        modules[1] = KeywordAnalysis.Module.TF_CLASSIFY;


        Map<KeywordAnalysis.Module, Properties> moduleProperties = new HashMap<>();
        Properties properties = new Properties();
        properties.put("in_codes", inCodesValue);
        properties.put("is_trend", false);
        moduleProperties.put(KeywordAnalysis.Module.TF_CLASSIFY, properties);

        properties = new Properties();
        properties.put("selectors","[{\"id\":\"keywords\",\"type\":\"WORD_CLASS\",\"value\":\"NOUN\"}]");
        properties.put("count", keywordCount);

        moduleProperties.put(KeywordAnalysis.Module.TF_WORD_CONTENTS, properties);

        keywordAnalysis.keywordAnalysis(startTime, endTime, standardTime, keywordJson, keysArray, modules, moduleProperties, parameterMap, endCallback);

    }

}
