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
import com.wigoai.nipa.regional.service.data.ChannelEmotion;
import com.wigoai.nipa.regional.service.data.ChannelGroupStatus;
import com.wigoai.nipa.regional.service.data.ChannelStatus;
import com.wigoai.nipa.regional.service.util.GroupKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.moara.common.callback.ObjectCallback;
import org.moara.common.config.Config;
import org.moara.common.util.ExceptionUtil;
import org.moara.keyword.KeywordAnalysis;
import org.moara.keyword.ServiceKeywordAnalysis;
import org.moara.keyword.search.SearchKeyword;
import org.moara.keyword.tf.contents.ChannelGroupHas;
import org.moara.message.disposable.DisposableMessageManager;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 매체(뉴스) 분석
 * @author macle
 */
@Slf4j
public class MediaAnalysis {

    public static JsonObject analysis(long startTime, long endTime, long standardTime, SearchKeyword[] searchKeywords,  List<String> ymdList, Map<String, Object> parameterMap ) {
        long analysisStartTime = System.currentTimeMillis();
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

        NipaRegionalAnalysis nipaRegionalAnalysis = NipaRegionalAnalysis.getInstance();
        ChannelManager channelManager = nipaRegionalAnalysis.getChannelManager();
        ChannelGroup[] groups = channelManager.getMediaChannelGroups();


        final KeywordAnalysis.Module [] modules = new KeywordAnalysis.Module[2];
        modules[0] =  KeywordAnalysis.Module.TF_CONTENTS;
        modules[1] =  KeywordAnalysis.Module.TF_CONTENTS_GROUP;

        Map<KeywordAnalysis.Module, Properties> moduleProperties = new HashMap<>();
        String [] emotionCodes = nipaRegionalAnalysis.getEmotionCodes();

        Properties properties = new Properties();
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


        String [] channelIds = channelManager.getMediaChannelIds();
        String [][] keysArray = GroupKeyUtil.makeChannelKeysArray(ymdList,  channelIds);
        ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
        KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();
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
            log.error("time out");
            return new JsonObject();
        }

        JsonObject resultObj = new JsonObject();


        DisposableMessageManager disposableMessageManager = DisposableMessageManager.getInstance();
        String responseMessage =  disposableMessageManager.getMessages(messageId);
        //데이터 변환
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JSONObject responseObj = new JSONObject(responseMessage);
        JSONArray messageArray =  responseObj.getJSONArray("messages");
        for (int i = 0; i <messageArray.length() ; i++) {
            JSONObject messageObj = new JSONObject(messageArray.getString(i));
            KeywordAnalysis.Module module = KeywordAnalysis.Module.valueOf(messageObj.get("type").toString());
            if (module == KeywordAnalysis.Module.TF_CONTENTS_GROUP) {

                //매체그룹별 트랜드
                //매체그룹별 긍부정
                messageObj = messageObj.getJSONObject("message");

                resultObj.add("times",gson.fromJson(messageObj.getJSONArray("times").toString(), JsonArray.class));
                JSONArray timeTfMaps = messageObj.getJSONArray("time_tf_arrays");

                JsonArray channelGroupArray = new JsonArray();
                for (int j = 0; j <timeTfMaps.length() ; j++) {
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
                resultObj.add("channel_group_status_array", gson.toJsonTree(statusArray));

            }else{
                messageObj = messageObj.getJSONObject("message");

                Map<String, ChannelEmotion> channelEmotionMap = new HashMap<>();

                JSONObject tfObj = messageObj.getJSONObject("tf_channel");
                Set<String> keys = tfObj.keySet();
                for(String key : keys){
                    ChannelEmotion channelEmotion = new ChannelEmotion();
                    channelEmotionMap.put(key, channelEmotion);

                    channelEmotion.setChannel_id(key);
                    channelEmotion.setChannel_name(channelManager.getChannel(key).getName());
                    channelEmotion.setCount(tfObj.getInt(key));


                    for(ChannelGroup channelGroup : groups){
                        if(channelGroup.hasChannel(key)){
                            channelEmotion.setGroup_id(channelGroup.getId());
                            channelEmotion.setGroup_name(channelGroup.getName());
                            break;
                        }
                    }
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
                    ChannelEmotion channelEmotion = channelEmotionMap.get(key);
                    channelEmotion.setPositive_count(counts.getInt(positiveIndex));
                    channelEmotion.setNegative_count(counts.getInt(negativeIndex));
                    channelEmotion.setNeutral_count(counts.getInt(neutralIndex));
                }

                ChannelEmotion [] channelEmotionArray = channelEmotionMap.values().toArray(new ChannelEmotion[0]);
                Arrays.sort(channelEmotionArray, ChannelEmotion.SORT_DESC);
                resultObj.add("channel_status_array", gson.toJsonTree(channelEmotionArray));
            }
        }


        return resultObj;
    }
}
