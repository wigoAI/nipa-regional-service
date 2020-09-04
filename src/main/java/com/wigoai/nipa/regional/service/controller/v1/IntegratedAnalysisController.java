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
import com.wigoai.nipa.regional.service.ChannelGroup;
import com.wigoai.nipa.regional.service.NipaRegionalAnalysis;
import com.wigoai.nipa.regional.service.ServiceConfig;
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
 * 통합분석
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
    @RequestMapping(value = "/nipars/v1/integrated/classify" , method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public String classify(@RequestBody final String jsonValue) {

        try {



            long analysisStartTime = System.currentTimeMillis();

            JSONObject request = new JSONObject(jsonValue);

            long startTime = request.getLong("start_time");
            long endTime = request.getLong("end_time");
            long standardTime = request.getLong("standard_time");

            long analysisMaxTime = Config.getLong(ServiceConfig.ANALYSIS_MAX_TIME.key(), (long)ServiceConfig.ANALYSIS_MAX_TIME.defaultValue());



            final Thread currentThread = Thread.currentThread();




            AtomicBoolean isAnalysis = new AtomicBoolean(false);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            ObjectCallback endCallback = obj -> {

                try {
                    isAnalysis.set(true);
                    currentThread.interrupt();
                }catch(Exception e){
                    logger.error(ExceptionUtil.getStackTrace(e));
                }
            };



            final KeywordAnalysis.Module [] modules = new KeywordAnalysis.Module[1];
            modules[0] = KeywordAnalysis.Module.TF_CONTENTS;
//            modules[1] = KeywordAnalysis.Module.TF_CLASSIFY;


            ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
            KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();


            String startYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(startTime));
            String endYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(endTime));

            List<String> ymdList = YmdUtil.getYmdList(startYmd,endYmd);

            ChannelGroup[] groups = NipaRegionalAnalysis.getInstance().getGroups();

            int size = ymdList.size()*groups.length;

            String [][] keysArray = new String[size][2];

            int index = 0;
            for(String ymd : ymdList){
                for (ChannelGroup group : groups) {
                    String[] key = new String[2];
                    key[0] = ymd;
                    key[1] = group.getId();
                    keysArray[index++] = key;
                }
            }

            Map<String, Object> parameterMap = null;
            if(request.has("stopwords")){
                JSONArray stopwordArray = request.getJSONArray("stopwords");
                if(stopwordArray.length() > 0) {

                    Set<String> stopwordSet = new HashSet<>();

                    for (int i = 0; i < stopwordArray.length(); i++) {
                        stopwordSet.add(stopwordArray.getString(i));
                    }
                    //옵션이 지금은 하나 이므로 여기에서 생성 나중에는 null일때만 생성하게 변경해야함
                    parameterMap = new HashMap<>();
                    parameterMap.put("stopwordSyllableSet", stopwordSet);
                }

            }


            String messageId = keywordAnalysis.keywordAnalysis(startTime, endTime, standardTime, request.getJSONArray("keywords").toString(), keysArray, modules, null, parameterMap, endCallback);


            try {
                long analysisTime = System.currentTimeMillis() - analysisStartTime;
                //최대 대기 시간
                Thread.sleep(analysisMaxTime - analysisTime);
            }catch (InterruptedException ignore){}

//            gson.fromJson()
            if(!isAnalysis.get()){
                logger.error("time out: " + jsonValue);
                return "";
            }

            DisposableMessageManager disposableMessageManager = DisposableMessageManager.getInstance();
            String message =  disposableMessageManager.getMessages(messageId);


            logger.debug("init data second: " + jsonValue +":  "+ TimeUtil.getSecond(System.currentTimeMillis() - analysisStartTime));
            return gson.toJson(message);

        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
            return "{}";
        }

    }





}
