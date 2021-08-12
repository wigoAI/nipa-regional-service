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
import com.wigoai.nipa.regional.service.data.CharacterTrendStatus;
import com.wigoai.nipa.regional.service.data.CountTitle;
import com.wigoai.nipa.regional.service.util.GroupKeyUtil;
import com.wigoai.nipa.regional.service.util.ParameterUtil;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.moara.ara.datamining.textmining.dictionary.word.WordDictionary;
import org.moara.ara.datamining.textmining.dictionary.word.element.Word;
import org.moara.category.CategoryDictionary;
import org.moara.category.element.Category;
import org.moara.common.callback.ObjectCallback;
import org.moara.common.config.Config;
import org.moara.common.time.TimeUtil;
import org.moara.common.util.ExceptionUtil;
import org.moara.common.util.YmdUtil;
import org.moara.keyword.KeywordAnalysis;
import org.moara.keyword.ServiceKeywordAnalysis;
import org.moara.keyword.tf.contents.ChannelGroupHas;
import org.moara.message.disposable.DisposableMessageManager;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 인물 분석
 * 소스가 너무 길어져서 나눈 클래스
 * @author macle
 */
@Slf4j
public class CharacterAnalysis {

    public static JsonArray ner(JSONObject request) {
        JsonArray keywords = new JsonArray();

        long analysisStartTime = System.currentTimeMillis();


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
                log.error(ExceptionUtil.getStackTrace(e));
            }
        };

        final KeywordAnalysis.Module [] modules = new KeywordAnalysis.Module[1];
        modules[0] =  KeywordAnalysis.Module.TF_WORD_CONTENTS;

        Map<KeywordAnalysis.Module, Properties> moduleProperties = new HashMap<>();
        Properties properties = new Properties();


        String categoryCode = Config.getConfig(ServiceConfig.NER_CATEGORY_CODE.key(),(String) ServiceConfig.NER_CATEGORY_CODE.defaultValue());

        JSONArray selectors = new JSONArray();
        JSONObject keywordSelector = new JSONObject();
        keywordSelector.put("id", "ner_keywords");
        keywordSelector.put("type", "CATEGORY_WORD");
        keywordSelector.put("value", categoryCode);

        selectors.put(keywordSelector);

        properties.put("selectors", selectors.toString());

        if(!request.isNull("ner_count")){
            properties.put("count", request.getInt("ner_count"));
        }else{
            properties.put("count", 50);
        }

        if(!request.isNull("ner_scope")){
            properties.put("scope", request.getString("ner_scope"));
        }else{
            properties.put("scope", "FULL");
        }

        properties.put("is_trend", false);

        WordDictionary wordDictionary = WordDictionary.getInstance();
        Word characterWord = wordDictionary.getSyllable(request.getString("name")).getDictionaryWord().getWord();

        properties.put("scope_word", characterWord);
        moduleProperties.put(KeywordAnalysis.Module.TF_WORD_CONTENTS, properties);

        ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
        KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();

        ChannelManager channelManager = NipaRegionalAnalysis.getInstance().getChannelManager();
        String [] characterChannelIds = channelManager.getCharacterChannelIds();

        String startYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(startTime));
        String endYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(endTime-1));

        List<String> ymdList = YmdUtil.getYmdList(startYmd,endYmd);
        String [][] keysArray = GroupKeyUtil.makeChannelKeysArray(ymdList,  characterChannelIds);

        Map<String, Object> parameterMap = ParameterUtil.makeParameterMap(request);


        String messageId = keywordAnalysis.analysis(startTime, endTime, standardTime, keywordAnalysis.makeSearchKeywords(CharacterAnalysis.getKeywordJson(request)), keysArray, modules, moduleProperties, parameterMap, endCallback);

        try {
            long analysisTime = System.currentTimeMillis() - analysisStartTime;
            //최대 대기 시간
            long sleepTime = analysisMaxTime - analysisTime;
            if(sleepTime>0) {
                Thread.sleep(sleepTime);
            }
        }catch (InterruptedException ignore){}

        if(!isAnalysis.get()){
            log.error("time out: " + request);
            return keywords;
        }



        DisposableMessageManager disposableMessageManager = DisposableMessageManager.getInstance();
        String responseMessage =  disposableMessageManager.getMessages(messageId);
        //데이터 변환

        JSONObject responseObj = new JSONObject(responseMessage);
        JSONArray messageArray =  responseObj.getJSONArray("messages");
        JSONArray nerKeywords = new JSONObject(messageArray.getString(0)).getJSONObject("message").getJSONArray("ner_keywords");
        Category category = CategoryDictionary.getInstance().getCategory(categoryCode);
        Category [] nerCategories = category.getChildArray();
        for (int i = 0; i <nerKeywords.length() ; i++) {
            JSONObject nerKeyword =  nerKeywords.getJSONObject(i);

            String code = nerKeyword.getString("code");

            JsonObject keyword = new JsonObject();
            keyword.addProperty("code", code);
            keyword.addProperty("syllable", nerKeyword.getString("syllable"));
            keyword.addProperty("count", nerKeyword.getInt("count"));

            JsonArray ner = new JsonArray();
            for(Category nerCategory: nerCategories){
                if(nerCategory.isWordIn(code)){
                    ner.add(nerCategory.getName());
                }
            }
            keyword.add("ner", ner);
            keywords.add(keyword);
        }

        return keywords;
    }


    public static JSONArray getKeywordJson(JSONObject request){

        JSONArray keywords =new JSONArray();

        if(!request.isNull("infos")){
            String name = request.getString("name");

            JSONArray infos = request.getJSONArray("infos");
            for (int i = 0; i <infos.length() ; i++) {
                JSONObject keyword = new JSONObject();
                keyword.put("keyword", name);
                JSONArray inFilters = new JSONArray();
                inFilters.put(infos.getString(i));
                keyword.put("in_filters", inFilters);
                keywords.put(keyword);
            }


        }else{
            keywords.put(request.getString("name"));
        }

        return keywords;
    }


    public static String trend(JSONObject request){

        long analysisStartTime = System.currentTimeMillis();


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
                log.error(ExceptionUtil.getStackTrace(e));
            }
        };

        final KeywordAnalysis.Module [] modules = new KeywordAnalysis.Module[2];
        modules[0] =  KeywordAnalysis.Module.TF_CONTENTS_GROUP;
        modules[1] =  KeywordAnalysis.Module.TF_CLASSIFY;

        NipaRegionalAnalysis nipaRegionalAnalysis = NipaRegionalAnalysis.getInstance();

        String [] emotionCodes = nipaRegionalAnalysis.getEmotionCodes();

        Map<KeywordAnalysis.Module, Properties> moduleProperties = new HashMap<>();

        Properties properties = new Properties();
        StringBuilder emotionBuilder = new StringBuilder();
        for(String emotionCode : emotionCodes){
            emotionBuilder.append(",").append(emotionCode);
        }
        properties.put("in_codes", emotionBuilder.substring(1));
        properties.put("is_trend", true);
        moduleProperties.put(KeywordAnalysis.Module.TF_CLASSIFY, properties);


        properties = new Properties();
        ChannelManager channelManager = nipaRegionalAnalysis.getChannelManager();
        ChannelGroup[] groups = channelManager.getCharacterChannelGroups();

        ChannelGroupHas[] channelGroups = new ChannelGroupHas[groups.length];
        //noinspection ManualArrayCopy
        for (int i = 0; i <channelGroups.length ; i++) {
            channelGroups[i] = groups[i];
        }
        WordDictionary wordDictionary = WordDictionary.getInstance();
        Word characterWord = wordDictionary.getSyllable(request.getString("name")).getDictionaryWord().getWord();
        properties.put("title_word", characterWord);
        properties.put("channel_groups", channelGroups);
        moduleProperties.put(KeywordAnalysis.Module.TF_CONTENTS_GROUP, properties);


        String [] characterChannelIds = channelManager.getCharacterChannelIds();

        String startYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(startTime));
        String endYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(endTime-1));

        List<String> ymdList = YmdUtil.getYmdList(startYmd,endYmd);
        String [][] keysArray = GroupKeyUtil.makeChannelKeysArray(ymdList,  characterChannelIds);

        Map<String, Object> parameterMap = ParameterUtil.makeParameterMap(request);

        ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
        KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();

        String messageId = keywordAnalysis.analysis(startTime, endTime, standardTime, keywordAnalysis.makeSearchKeywords(CharacterAnalysis.getKeywordJson(request)), keysArray, modules, moduleProperties, parameterMap, endCallback);


        try {
            long analysisTime = System.currentTimeMillis() - analysisStartTime;
            //최대 대기 시간
            long sleepTime = analysisMaxTime - analysisTime;
            if(sleepTime>0) {
                Thread.sleep(sleepTime);
            }
        }catch (InterruptedException ignore){}

        if(!isAnalysis.get()){
            log.error("time out: " + request);
            return "{}";
        }

        DisposableMessageManager disposableMessageManager = DisposableMessageManager.getInstance();
        String responseMessage =  disposableMessageManager.getMessages(messageId);
        //데이터 변환

        JSONObject responseObj = new JSONObject(responseMessage);
        JSONArray messageArray =  responseObj.getJSONArray("messages");

        JsonObject resultObj = new JsonObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        CharacterTrendStatus characterTrendStatus = new CharacterTrendStatus();

        for (int i = 0; i <messageArray.length() ; i++) {
            JSONObject messageObj = new JSONObject(messageArray.getString(i));
            KeywordAnalysis.Module module = KeywordAnalysis.Module.valueOf(messageObj.get("type").toString());

            if (module == KeywordAnalysis.Module.TF_CONTENTS_GROUP) {
                messageObj = messageObj.getJSONObject("message");
                characterTrendStatus.setCount(messageObj.getInt("total"));
                resultObj.add("times",gson.fromJson(messageObj.getJSONArray("times").toString(),JsonArray.class));

                JSONObject titleMap = messageObj.getJSONObject("title_tf");
                characterTrendStatus.setTitle(sum(titleMap));

                JSONArray timeTfMaps = messageObj.getJSONArray("time_tf_arrays");
                JSONArray titleTimeMaps = messageObj.getJSONArray("title_time_arrays");

                int length = timeTfMaps.length();

                CountTitle[] titles = new CountTitle[length];

                for (int j = 0; j <length ; j++) {
                    titles[j] = new CountTitle();
                    titles[j].setCount(sum(timeTfMaps.getJSONObject(j).getJSONObject("tf_channel")));
                    titles[j].setTitle(sum(titleTimeMaps.getJSONObject(j)));
                }

                resultObj.add("count_title_trend", gson.toJsonTree(titles));

                JsonArray channelGroupArray = new JsonArray();

                for (int j = 0; j <length ; j++) {
                    JSONObject map = timeTfMaps.getJSONObject(j).getJSONObject("tf_channel");
                    JsonArray groupArray = new JsonArray();
                    for(ChannelGroup group :groups){
                        JsonObject channelGroup = new JsonObject();
                        channelGroup.addProperty("id", group.getId());
                        channelGroup.addProperty("name", group.getName());
                        if(map.isNull(group.getId())){
                            channelGroup.addProperty("count", 0);
                        }else{
                            channelGroup.addProperty("count", map.getInt(group.getId()));
                        }

                        groupArray.add(channelGroup);
                    }

                    channelGroupArray.add(groupArray);

                }
                resultObj.add("channel_group_trend",channelGroupArray);

            }else{
                //감성분류
                messageObj = messageObj.getJSONObject("message");
                resultObj.add("emotion_classify_trend", gson.fromJson(messageObj.getJSONArray("classifies").toString(), JsonArray.class));
            }
        }


        //직전 건수 추출
        resultObj.add("status", gson.toJsonTree(characterTrendStatus));
        String result = gson.toJson(resultObj);
        log.debug("analysis second: " + request +":  "+ TimeUtil.getSecond(System.currentTimeMillis() - analysisStartTime));
        return result;
    }


    private static int sum(JSONObject object){

        int sum =0;

        Set<String> keys = object.keySet();

        for(String key: keys){
            sum+= object.getInt(key);
        }

        return sum;
    }

}
