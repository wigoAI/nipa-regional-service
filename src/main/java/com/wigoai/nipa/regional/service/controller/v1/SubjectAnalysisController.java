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
import com.wigoai.nipa.regional.service.ChannelGroup;
import com.wigoai.nipa.regional.service.NipaRegionalAnalysis;
import com.wigoai.nipa.regional.service.ServiceConfig;
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
            modules[2] = KeywordAnalysis.Module.TF_WORD;

            NipaRegionalAnalysis nipaRegionalAnalysis = NipaRegionalAnalysis.getInstance();

            Map<KeywordAnalysis.Module,Properties> moduleProperties = new HashMap<>();

            Properties properties = new Properties();

            ChannelGroup[] groups = nipaRegionalAnalysis.getGroups();
            StringBuilder channelIdBuilder = new StringBuilder();
            for(ChannelGroup channelGroup : groups){
                channelIdBuilder.append(",").append(channelGroup.getId());
            }
            properties.put("channels", channelIdBuilder.substring(1));
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
            positiveSelector.put("type", "CATEGORY_WORD");
            positiveSelector.put("value", Config.getConfig(ServiceConfig.POSITIVE_CODE.key(),(String) ServiceConfig.POSITIVE_CODE.defaultValue()));

            JSONObject negativeSelector = new JSONObject();
            negativeSelector.put("id", "negative_keywords");
            negativeSelector.put("type", "CATEGORY_WORD");
            negativeSelector.put("value", Config.getConfig(ServiceConfig.NEGATIVE_CODE.key(),(String) ServiceConfig.NEGATIVE_CODE.defaultValue()));


            selectors.put(positiveSelector);
            selectors.put(negativeSelector);

            properties.put("selectors", selectors.toString());
            properties.put("count", 100);
            properties.put("is_trend", false);
            moduleProperties.put(KeywordAnalysis.Module.TF_WORD, properties);


            String keywordJson = request.getJSONArray("keywords").toString();

            Map<String, Object> parameterMap = parameterUtil.makeParameterMap(request);

            ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
            KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();


            String startYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(startTime));
            String endYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(endTime-1));

            List<String> ymdList = YmdUtil.getYmdList(startYmd,endYmd);
            String [][] keysArray = GroupKeyUtil.makeKeysArray(ymdList, groups);

            final String messageId = keywordAnalysis.keywordAnalysis(startTime, endTime, standardTime, keywordJson, keysArray, modules, moduleProperties, parameterMap, endCallback);

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
                    resultObj.put("emotion_classifies", messageObj.getJSONObject("message").getJSONArray("classifies"));
                } else {
                    resultObj.put("keywords", messageObj.getJSONArray("message"));
                }
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String result =  gson.toJson(gson.fromJson(resultObj.toString(), JsonObject.class));
            logger.debug("analysis second: " + jsonValue +":  "+ TimeUtil.getSecond(System.currentTimeMillis() - analysisStartTime));
            return result;


        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
            return "{}";
        }

    }
}
