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
import com.wigoai.nipa.regional.service.channel.ChannelManager;
import com.wigoai.nipa.regional.service.util.GroupKeyUtil;
import com.wigoai.nipa.regional.service.util.ParameterUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.moara.common.callback.ObjectCallback;
import org.moara.common.config.Config;
import org.moara.common.time.TimeUtil;
import org.moara.common.util.ExceptionUtil;
import org.moara.common.util.YmdUtil;
import org.moara.keyword.KeywordAnalysis;
import org.moara.keyword.ServiceKeywordAnalysis;
import org.moara.keyword.search.SearchKeyword;
import org.moara.keyword.tf.contents.ChannelGroupHas;
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
 * 주제분석
 * @author macle
 */
@RestController
public class SubjectAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(IntegratedAnalysisController.class);

    /**
     * 주제 분석
     * @param jsonValue String json object
     * @return String json object
     */
    @RequestMapping(value = "/nipars/v1/subject/analysis" , method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public String analysis(@RequestBody final String jsonValue) {

        try {


            long analysisStartTime = System.currentTimeMillis();

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


            JSONObject request = new JSONObject(jsonValue);

            long startTime = request.getLong("start_time");
            long endTime = request.getLong("end_time");
            long standardTime = request.getLong("standard_time");
            long analysisMaxTime = Config.getLong(ServiceConfig.ANALYSIS_MAX_TIME.key(), (long)ServiceConfig.ANALYSIS_MAX_TIME.defaultValue());


            final KeywordAnalysis.Module [] modules = new KeywordAnalysis.Module[3];
            modules[0] = KeywordAnalysis.Module.TF_CONTENTS;
            modules[1] = KeywordAnalysis.Module.TF_CLASSIFY;
            modules[2] = KeywordAnalysis.Module.TF_WORD_CONTENTS;

            NipaRegionalAnalysis nipaRegionalAnalysis = NipaRegionalAnalysis.getInstance();

            Map<KeywordAnalysis.Module,Properties> moduleProperties = new HashMap<>();

            Properties properties = new Properties();


            ChannelManager channelManager = NipaRegionalAnalysis.getInstance().getChannelManager();
            ChannelGroup[] groups = new ChannelGroup[2];
            groups[0] =  channelManager.getGroupFromId("media");
            groups[1] =  channelManager.getGroupFromId("community");

            ChannelGroupHas[] channelGroups = new ChannelGroupHas[2];
            channelGroups[0] = groups[0];
            channelGroups[1] = groups[1];


            properties.put("channel_groups", channelGroups);
            moduleProperties.put(KeywordAnalysis.Module.TF_CONTENTS, properties);


            properties = new Properties();
            String [] emotionCodes = nipaRegionalAnalysis.getEmotionCodes();
            StringBuilder emotionBuilder = new StringBuilder();
            for(String emotionCode : emotionCodes){
                emotionBuilder.append(",").append(emotionCode);
            }
            properties.put("in_codes", emotionBuilder.substring(1));
            properties.put("is_trend", true);
            moduleProperties.put(KeywordAnalysis.Module.TF_CLASSIFY, properties);

            properties = new Properties();

            JSONArray selectors = new JSONArray();
            JSONObject positiveSelector = new JSONObject();
            positiveSelector.put("id", "positive_keywords");
            positiveSelector.put("type", "CATEGORY_ARRAY_WORD");
            positiveSelector.put("value", Config.getConfig(ServiceConfig.POSITIVE_CODE.key(),(String) ServiceConfig.POSITIVE_CODE.defaultValue()));

            JSONObject negativeSelector = new JSONObject();
            negativeSelector.put("id", "negative_keywords");
            negativeSelector.put("type", "CATEGORY_ARRAY_WORD");
            negativeSelector.put("value", Config.getConfig(ServiceConfig.NEGATIVE_CODE.key(),(String) ServiceConfig.NEGATIVE_CODE.defaultValue()));

            selectors.put(positiveSelector);
            selectors.put(negativeSelector);
            properties.put("selectors", selectors.toString());

            if(request.has("emotion_keyword_count")){
                properties.put("count", request.getInt("emotion_keyword_count"));
            }else{
                properties.put("count", 50);
            }

            properties.put("is_trend", false);
            moduleProperties.put(KeywordAnalysis.Module.TF_WORD_CONTENTS, properties);

            String keywordJson = request.getJSONArray("keywords").toString();

            Map<String, Object> parameterMap = ParameterUtil.makeParameterMap(request);

            ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
            KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();

            String startYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(startTime));
            String endYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(endTime-1));

            List<String> ymdList = YmdUtil.getYmdList(startYmd,endYmd);
            String [][] keysArray = GroupKeyUtil.makeKeysArray(ymdList, groups);

            SearchKeyword[] searchKeywords = keywordAnalysis.makeSearchKeywords(new JSONArray(keywordJson));
            final String messageId = keywordAnalysis.analysis(startTime, endTime, standardTime, searchKeywords, keysArray, modules, moduleProperties, parameterMap, endCallback);

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
                }else if(module == KeywordAnalysis.Module.TF_CLASSIFY){
                    resultObj.put("emotion_classifies_trend", messageObj.getJSONObject("message"));
                } else {
                    JSONObject emotionObj = messageObj.getJSONObject("message");
                    resultObj.put("negative_keywords", emotionObj.getJSONArray("negative_keywords"));

                    resultObj.put("positive_keywords", emotionObj.getJSONArray("positive_keywords"));
                }
            }

            int cloudKeywordCount;
            if(request.has("cloud_keyword_count")){
                cloudKeywordCount = request.getInt("cloud_keyword_count");
            }else{
                cloudKeywordCount = 100;
            }

            Properties snaProperties = null;
            if(request.has("sna_use_count")){

                snaProperties = new Properties();

                snaProperties.put("use_count", request.getInt("sna_use_count"));
            }
            if(request.has("sna_source_count")){
                if(snaProperties == null){
                    snaProperties = new Properties();
                }
                snaProperties.put("source_count", request.getInt("sna_source_count"));
            }
            if(request.has("sna_target_count")){
                if(snaProperties == null){
                    snaProperties = new Properties();
                }
                snaProperties.put("target_count", request.getInt("sna_target_count"));
            }

            isAnalysis.set(false);
            keyword(resultObj, endCallback, groups, 0, startTime, endTime, standardTime, searchKeywords, parameterMap, keywordAnalysis, ymdList, cloudKeywordCount, snaProperties);
            try {
                long analysisTime = System.currentTimeMillis() - analysisStartTime;
                //최대 대기 시간
                Thread.sleep(analysisMaxTime - analysisTime);
            }catch (InterruptedException ignore){}

            if(!isAnalysis.get()){
                logger.error("time out: " + jsonValue);
                return "{}";
            }


            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            JsonObject jsonObj = gson.fromJson(resultObj.toString(), JsonObject.class);
            jsonObj.add("media_analysis", MediaAnalysis.analysis(startTime, endTime, standardTime, searchKeywords, ymdList, parameterMap));

            String result =  gson.toJson(jsonObj);
            logger.debug("analysis second: " + jsonValue +":  "+ TimeUtil.getSecond(System.currentTimeMillis() - analysisStartTime));
            return result;


        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
            return "{}";
        }
    }


    private void keyword(final JSONObject resultObj, final ObjectCallback callback, final ChannelGroup[] groups, final int groupIndex
            , final long startTime, final long endTime, final long standardTime, final SearchKeyword[] searchKeywords,  Map<String, Object> parameterMap, final KeywordAnalysis keywordAnalysis, final List<String> ymdList
            , final int cloudKeywordCount ,  Properties snaProperties
    ){

        ObjectCallback endCallback  = obj -> {

            try {

                if(obj == null){
                    //검색 결과가 없을때
                    if(groupIndex >= groups.length-1){
                        callback.callback(null);
                        return;
                    }
                    keyword(resultObj, callback, groups, groupIndex +1, startTime, endTime, standardTime, searchKeywords, parameterMap, keywordAnalysis, ymdList, cloudKeywordCount, snaProperties);
                    return ;
                }

                DisposableMessage disposableMessage = (DisposableMessage)obj;
                String [] messages = disposableMessage.getMessages();

                for(String message : messages){
                    JSONObject messageObj = new JSONObject(message);
                    KeywordAnalysis.Module module = KeywordAnalysis.Module.valueOf(messageObj.get("type").toString());
                    if (module == KeywordAnalysis.Module.TF_WORD_CONTENTS) {
                        resultObj.put(groups[groupIndex].getId() + "_keywords", messageObj.getJSONObject("message").getJSONArray("keywords"));
                    } else {
                        //릴레이션 일때
                        resultObj.put(groups[groupIndex].getId() +"_networks", messageObj.getJSONArray("message"));
                    }
                }
                if(groupIndex >= groups.length-1){
                    callback.callback(null);
                    return;
                }

                keyword(resultObj, callback, groups, groupIndex +1, startTime, endTime, standardTime, searchKeywords, parameterMap, keywordAnalysis, ymdList, cloudKeywordCount, snaProperties);
            }catch(Exception e){

                if(obj == null){
                    logger.error("keyword analysis fail");
                    callback.callback(null);
                    return ;
                }

                logger.error(ExceptionUtil.getStackTrace(e));
            }
        };

        String [][] keysArray = GroupKeyUtil.makeKeysArray(ymdList,  groups[groupIndex]);

        KeywordAnalysis.Module [] modules = new KeywordAnalysis.Module[2];
        modules[0] = KeywordAnalysis.Module.TF_WORD_CONTENTS;
        modules[1] = KeywordAnalysis.Module.SNA_LITE;



        Map<KeywordAnalysis.Module, Properties> moduleProperties = new HashMap<>();
        Properties properties = new Properties();
        properties.put("selectors","[{\"id\":\"keywords\",\"type\":\"WORD_CLASS\",\"value\":\"NOUN\"}]");
        properties.put("count", cloudKeywordCount);
        properties.put("is_trend",false);
        moduleProperties.put(KeywordAnalysis.Module.TF_WORD_CONTENTS, properties);

        if(snaProperties != null){
            moduleProperties.put(KeywordAnalysis.Module.SNA_LITE, snaProperties);
        }


        keywordAnalysis.analysis(startTime, endTime, standardTime, searchKeywords, keysArray, modules, moduleProperties, parameterMap, endCallback);

    }

}
