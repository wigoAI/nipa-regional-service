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
import com.wigoai.nipa.regional.service.ServiceConfig;
import org.json.JSONObject;
import org.moara.common.callback.ObjectCallback;
import org.moara.common.config.Config;
import org.moara.common.time.TimeUtil;
import org.moara.common.util.ExceptionUtil;
import org.moara.keyword.KeywordAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
    @RequestMapping(value = "/nipars/v1/integrated/init" , method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public String init(@RequestBody final String jsonValue) {

        try {

            long analysisStartTime = System.currentTimeMillis();

            JSONObject request = new JSONObject(jsonValue);

            long startTime = request.getLong("start_time");
            long endTime = request.getLong("end_time");
            long standardTime = request.getLong("standard_time");

            final Thread currentThread = Thread.currentThread();


            AtomicBoolean isAnalysis = new AtomicBoolean(false);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            final JsonObject response = new JsonObject() ;

            ObjectCallback endCallback = obj ->
            {

                try {

                    isAnalysis.set(true);
                    currentThread.interrupt();
                }catch(Exception e){
                    logger.error(ExceptionUtil.getStackTrace(e));
                }
            };



            KeywordAnalysis.Module [] modules = new KeywordAnalysis.Module[2];
            modules[0] = KeywordAnalysis.Module.TF_CONTENTS;
            modules[1] = KeywordAnalysis.Module.TF_CONTENTS;
            try {
                //최대 대기 시간
                Thread.sleep(Config.getLong(ServiceConfig.ANALYSIS_MAX_TIME.key(), (long)ServiceConfig.ANALYSIS_MAX_TIME.defaultValue()));
            }catch (InterruptedException ignore){}



//            gson.fromJson()
            if(isAnalysis.get()){
                logger.debug("init data second: " + jsonValue +":  "+ TimeUtil.getSecond(System.currentTimeMillis() - analysisStartTime));
                return gson.toJson(response);
            }else{
                logger.error("time out: " + jsonValue);
                return "";
            }


        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
            return "{}";
        }

    }


    public static void main(String[] args) {
        JSONObject obj = new JSONObject();
        System.out.println(obj.toString());


    }

}
