/*
 * Copyright (C) 2021 Wigo Inc.
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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wigoai.nipa.regional.service.NipaRegionalAnalysis;
import com.wigoai.nipa.regional.service.ServiceConfig;
import com.wigoai.nipa.regional.service.channel.ChannelGroup;
import com.wigoai.nipa.regional.service.channel.ChannelManager;
import com.wigoai.nipa.regional.service.data.ChannelGroupStatus;
import com.wigoai.nipa.regional.service.data.ChannelStatus;
import com.wigoai.nipa.regional.service.util.GroupKeyUtil;
import com.wigoai.nipa.regional.service.util.ParameterUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.moara.ara.datamining.textmining.dictionary.word.WordDictionary;
import org.moara.ara.datamining.textmining.dictionary.word.element.Word;
import org.moara.common.callback.ObjectCallback;
import org.moara.common.config.Config;
import org.moara.common.time.TimeUtil;
import org.moara.common.util.ExceptionUtil;
import org.moara.common.util.YmdUtil;
import org.moara.keyword.KeywordAnalysis;
import org.moara.keyword.ServiceKeywordAnalysis;
import org.moara.keyword.search.SearchKeyword;
import org.moara.keyword.tf.contents.ChannelGroupHas;
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
 * 인물 분석
 * @author macle
 */
@RestController
public class CharacterAnalysisController {
    private static final Logger logger = LoggerFactory.getLogger(CharacterAnalysisController.class);



    @RequestMapping(value = "/nipars/v1/character/status" , method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public String status(@RequestBody final String jsonValue) {


        try{

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
            NipaRegionalAnalysis nipaRegionalAnalysis = NipaRegionalAnalysis.getInstance();

            ChannelManager channelManager = nipaRegionalAnalysis.getChannelManager();

            String startYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(startTime));
            String endYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(endTime-1));
            String [] characterChannelIds = channelManager.getCharacterChannelIds();
            List<String> ymdList = YmdUtil.getYmdList(startYmd,endYmd);
            String [][] keysArray = GroupKeyUtil.makeChannelKeysArray(ymdList,  characterChannelIds);

            final KeywordAnalysis.Module [] modules = new KeywordAnalysis.Module[5];
            modules[0] =  KeywordAnalysis.Module.TF_CONTENTS;
            modules[1] =  KeywordAnalysis.Module.TF_CONTENTS_GROUP;
            modules[2] = KeywordAnalysis.Module.TF_CLASSIFY;
            modules[3] = KeywordAnalysis.Module.TF_CLASSIFY_TARGET;
            modules[4] = KeywordAnalysis.Module.TF_WORD_CONTENTS;
            WordDictionary wordDictionary = WordDictionary.getInstance();
            Word characterWord = wordDictionary.getSyllable(request.getString("name")).getDictionaryWord().getWord();

            String [] emotionCodes = nipaRegionalAnalysis.getEmotionCodes();

            ChannelGroup[] groups = channelManager.getCharacterChannelGroups();
            Map<KeywordAnalysis.Module, Properties> moduleProperties = new HashMap<>();

            Properties properties = new Properties();
            properties.put("title_word", characterWord);
            properties.put("classify_codes", emotionCodes);

            moduleProperties.put(KeywordAnalysis.Module.TF_CONTENTS, properties);

            properties = new Properties();
            ChannelGroupHas[] channelGroups = new ChannelGroupHas[groups.length];
            //noinspection ManualArrayCopy
            for (int i = 0; i <channelGroups.length ; i++) {
                channelGroups[i] = groups[i];
            }

            properties.put("channel_groups", channelGroups);
            properties.put("classify_codes", emotionCodes);
            moduleProperties.put(KeywordAnalysis.Module.TF_CONTENTS_GROUP, properties);


            StringBuilder emotionBuilder = new StringBuilder();
            for(String emotionCode : emotionCodes){
                emotionBuilder.append(",").append(emotionCode);
            }

            properties = new Properties();
            properties.put("in_codes", emotionBuilder.substring(1));
            properties.put("is_trend", true);
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


            ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
            KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();


            Map<String, Object> parameterMap = ParameterUtil.makeParameterMap(request);

            SearchKeyword[] searchKeywords = keywordAnalysis.makeSearchKeywords(CharacterAnalysis.getKeywordJson(request));

            String messageId = keywordAnalysis.analysis(startTime, endTime, standardTime, searchKeywords, keysArray, modules, moduleProperties, parameterMap, endCallback);

            try {
                long analysisTime = System.currentTimeMillis() - analysisStartTime;
                //최대 대기 시간
                long sleepTime = analysisMaxTime - analysisTime;
                if(sleepTime>0) {
                    Thread.sleep(sleepTime);
                }
            }catch (InterruptedException ignore){}

            if(!isAnalysis.get()){
                logger.error("time out: " + jsonValue);
                return "{}";
            }


            DisposableMessageManager disposableMessageManager = DisposableMessageManager.getInstance();
            String responseMessage =  disposableMessageManager.getMessages(messageId);
            //데이터 변환

            JSONObject responseObj = new JSONObject(responseMessage);
            JSONArray messageArray =  responseObj.getJSONArray("messages");

            JsonObject resultObj = new JsonObject();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            for (int i = 0; i <messageArray.length() ; i++) {

                JSONObject messageObj = new JSONObject(messageArray.getString(i));
                KeywordAnalysis.Module module = KeywordAnalysis.Module.valueOf(messageObj.get("type").toString());


                if (module == KeywordAnalysis.Module.TF_CONTENTS) {
                    messageObj = messageObj.getJSONObject("message");
                    resultObj.addProperty("total", messageObj.getInt("total"));
                    Map<String, ChannelStatus> channelStatusMap = new HashMap<>();

                    JSONObject tfObj = messageObj.getJSONObject("tf_channel");

                    Set<String> keys = tfObj.keySet();
                    for(String key : keys){
                        ChannelStatus channelStatus = new ChannelStatus();
                        channelStatusMap.put(key, channelStatus);

                        channelStatus.setChannel_id(key);
                        channelStatus.setChannel_name(channelManager.getChannel(key).getName());
                        channelStatus.setCount(tfObj.getInt(key));


                        for(ChannelGroup channelGroup : groups){
                            if(channelGroup.hasChannel(key)){
                                channelStatus.setGroup_id(channelGroup.getId());
                                channelStatus.setGroup_name(channelGroup.getName());
                                break;
                            }
                        }

                    }

                    tfObj = messageObj.getJSONObject("title_tf");
                    for(String key : keys){
                        if(tfObj.isNull(key)){
                            continue;
                        }
                        channelStatusMap.get(key).setTitle_count(tfObj.getInt(key));
                    }

                    JSONArray codes = messageObj.getJSONArray("classify_codes");
                    int positiveIndex = 0;
                    int negativeIndex = 0;
                    int neutralIndex = 0;

                    for (int j = 0; j <codes.length() ; j++) {
                        String code = codes.getString(j);
                        if(code.equals(emotionCodes[0])){
                            positiveIndex = j;
                        }else if(code.equals(emotionCodes[1])){
                            negativeIndex = j;
                        }else if(code.equals(emotionCodes[2])){
                            neutralIndex = j;
                        }
                    }

                    tfObj = messageObj.getJSONObject("classify_tf");
                    for(String key : keys){
                        if(tfObj.isNull(key)){
                            continue;
                        }
                        JSONArray counts = tfObj.getJSONArray(key);
                        ChannelStatus channelStatus = channelStatusMap.get(key);
                        channelStatus.setPositive_count(counts.getInt(positiveIndex));
                        channelStatus.setNegative_count(counts.getInt(negativeIndex));
                        channelStatus.setNeutral_count(counts.getInt(neutralIndex));
                    }

                    ChannelStatus [] channelStatusArray = channelStatusMap.values().toArray(new ChannelStatus[0]);

                    Arrays.sort(channelStatusArray, ChannelStatus.SORT_DESC);
                    resultObj.add("channel_status_array", gson.toJsonTree(channelStatusArray));

                    channelStatusMap.clear();
                }else if (module == KeywordAnalysis.Module.TF_CONTENTS_GROUP) {
                    messageObj = messageObj.getJSONObject("message");
                    JSONObject tfObj = messageObj.getJSONObject("tf_channel");

                    Map<String, ChannelGroupStatus> statusMap = new HashMap<>();

                    for(ChannelGroup channelGroup : groups){
                        ChannelGroupStatus channelGroupStatus = new ChannelGroupStatus();
                        channelGroupStatus.setGroup_id(channelGroup.getId());
                        channelGroupStatus.setGroup_name(channelGroup.getName());
                        if(!tfObj.isNull(channelGroup.getId())){
                            channelGroupStatus.setCount(tfObj.getInt(channelGroup.getId()));
                        }
                        statusMap.put(channelGroup.getId(), channelGroupStatus);
                    }

                    JSONArray codes = messageObj.getJSONArray("classify_codes");
                    int positiveIndex = 0;
                    int negativeIndex = 0;
                    int neutralIndex = 0;

                    for (int j = 0; j <codes.length() ; j++) {
                        String code = codes.getString(j);
                        if(code.equals(emotionCodes[0])){
                            positiveIndex = j;
                        }else if(code.equals(emotionCodes[1])){
                            negativeIndex = j;
                        }else if(code.equals(emotionCodes[2])){
                            neutralIndex = j;
                        }
                    }
                    tfObj = messageObj.getJSONObject("classify_tf");
                    for(ChannelGroup channelGroup : groups){
                        if(tfObj.isNull(channelGroup.getId())){
                            continue;
                        }

                        JSONArray counts = tfObj.getJSONArray(channelGroup.getId());
                        ChannelGroupStatus status = statusMap.get(channelGroup.getId());
                        status.setPositive_count(counts.getInt(positiveIndex));
                        status.setNegative_count(counts.getInt(negativeIndex));
                        status.setNeutral_count(counts.getInt(neutralIndex));
                    }
                    ChannelGroupStatus [] statusArray = statusMap.values().toArray(new ChannelGroupStatus[0]);
                    Arrays.sort(statusArray, ChannelGroupStatus.SORT_DESC);
                    resultObj.add("channel_group_status_array", gson.toJsonTree(statusArray));
                }else if (module == KeywordAnalysis.Module.TF_CLASSIFY) {
                    messageObj = messageObj.getJSONObject("message");
                    resultObj.add("emotion_classifies", gson.fromJson(messageObj.toString(), JsonObject.class));
                }else if (module == KeywordAnalysis.Module.TF_CLASSIFY_TARGET) {
                    resultObj.add("field_emotion_classifies", gson.fromJson(messageObj.getJSONArray("message").toString(), JsonArray.class));
                }else if (module == KeywordAnalysis.Module.TF_WORD_CONTENTS) {
                    JSONObject emotionObj = messageObj.getJSONObject("message");
                    resultObj.add("negative_keywords",gson.fromJson(emotionObj.getJSONArray("negative_keywords").toString(), JsonArray.class));
                    resultObj.add("positive_keywords",gson.fromJson(emotionObj.getJSONArray("positive_keywords").toString(), JsonArray.class));
                }
            }
            
            //개체명 인식 분석 결과
            resultObj.add("ner_keywords",CharacterAnalysis.ner(request));
            resultObj.add("media_analysis", MediaAnalysis.analysis(startTime, endTime, standardTime, searchKeywords, ymdList, parameterMap));

            String result = gson.toJson(resultObj);
            logger.debug("analysis second: " + jsonValue +":  "+ TimeUtil.getSecond(System.currentTimeMillis() - analysisStartTime));
            return result;


        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
            return "{}";
        }

    }

    @RequestMapping(value = "/nipars/v1/character/ner" , method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public String ner(@RequestBody final String jsonValue) {
        try{
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            return gson.toJson(CharacterAnalysis.ner(new JSONObject(jsonValue)));
        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
            return "[]";
        }

    }

    @RequestMapping(value = "/nipars/v1/character/trend" , method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public String trend(@RequestBody final String jsonValue) {

        try {
            return CharacterAnalysis.trend(new JSONObject(jsonValue));
        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
            return "{}";
        }
    }

    @RequestMapping(value = "/nipars/v1/character/reporter" , method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public String reporter(@RequestBody final String jsonValue) {
        try{

            JSONObject request = new JSONObject(jsonValue);

            long startTime = request.getLong("start_time");
            long endTime = request.getLong("end_time");
            long standardTime = request.getLong("standard_time");
            String startYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(startTime));
            String endYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(endTime-1));

            //날짜정보
            List<String> ymdList = YmdUtil.getYmdList(startYmd,endYmd);

            Map<String, Object> parameterMap = ParameterUtil.makeParameterMap(request);
            ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
            KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();

            SearchKeyword[] searchKeywords = keywordAnalysis.makeSearchKeywords(CharacterAnalysis.getKeywordJson(request));

            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            return gson.toJson(MediaAnalysis.reporter(startTime,endTime,standardTime,searchKeywords,ymdList,parameterMap, request.getString("channel_id")));
        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
            return "[]";
        }
    }

}