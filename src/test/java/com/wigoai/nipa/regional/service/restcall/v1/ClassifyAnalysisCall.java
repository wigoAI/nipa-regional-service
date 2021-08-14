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

package com.wigoai.nipa.regional.service.restcall.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wigoai.rest.RestCall;

import java.text.SimpleDateFormat;

/**
 * 분야별분석
 * @author macle
 */
public class ClassifyAnalysisCall {


    public static void main(String[] args) throws Exception {

        long analysisStartTime = System.currentTimeMillis();

        long startTime = new SimpleDateFormat("yyyyMMdd HH:mm:ss").parse("20210802 00:00:00").getTime();

        long endTime = new SimpleDateFormat("yyyyMMdd HH:mm:ss").parse("20210812 00:00:00").getTime();

        long standardTime = System.currentTimeMillis();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        
        JsonObject param = new JsonObject();
        param.addProperty("start_time", startTime);
        param.addProperty("end_time", endTime);
        param.addProperty("standard_time", standardTime);

        //단어 최대 건수설정 설정하지 않으면 30
//        param.addProperty("keyword_count", 30);
        String [] classifyNameArray = {
                "보건위생"
                , "재난안전"
                , "건설교통"
                , "기타"

        };

        JsonArray inKeywords= new JsonArray();

        for(String classifyName : classifyNameArray){
            inKeywords.add("#"+classifyName);
        }

        //키워드설정
        JsonArray keywords = new JsonArray();

        JsonObject keyword1 = new JsonObject();
        keyword1.addProperty("keyword", "춘천");
        keyword1.add("in_filters",inKeywords);
        JsonObject keyword2 = new JsonObject();
        keyword2.addProperty("keyword", "서울");
        keyword2.add("in_filters",inKeywords);
        JsonObject keyword3 = new JsonObject();
        keyword3.addProperty("keyword", "환경");
        keyword3.add("in_filters",inKeywords);


        keywords.add(keyword1);
        keywords.add(keyword2);
        keywords.add(keyword3);


        param.add("keywords", keywords);


//        JsonArray keywords = new JsonArray();
//        keywords.add("서울");
//        param.add("keywords", keywords);


        JsonArray classifyNames = new JsonArray();
        for(String classifyName : classifyNameArray){
            classifyNames.add(classifyName);
        }

        param.add("classify_names", classifyNames);



        String request = gson.toJson(param);
//        String responseMessage = RestCall.postJson("http://127.0.0.1:33377/nipars/v1/integrated/analysis",request);
        String responseMessage = RestCall.postJson("http://sc.wigo.ai:10015/nipars/v1/integrated/analysis",request);

        System.out.println("mills second: " + (System.currentTimeMillis() - analysisStartTime));


        System.out.println("request\n " + request +"\n");
        System.out.println("responseMessage\n "+ responseMessage) ;


    }
}
