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
 * 주제별 분석
 * @author macle
 */
public class SubjectAnalysisCall {
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

        //설정예제
        //긍부정 단어 최대 건수설정 설정하지 않으면 50
        param.addProperty("emotion_keyword_count", 20);
        //클라우드 단어 최대 건수 설정 설정하지 안흐면 100
        param.addProperty("cloud_keyword_count", 20);

        param.addProperty("sna_use_count", 200);
        param.addProperty("sna_source_count", 100);
        param.addProperty("sna_target_count", 30);

        JsonArray inFilters= new JsonArray();
        JsonArray inFilter1 = new JsonArray();
        inFilter1.add("#보건위생");
        inFilter1.add("환경부");
        inFilters.add(inFilter1);
        JsonArray inFilter2 = new JsonArray();
        inFilter2.add("#교육");
        inFilter2.add("환경부");
        inFilters.add(inFilter2);



        JsonArray keywords = new JsonArray();
        keywords.add("춘천");
//        JsonObject keyword1 = new JsonObject();
//        keyword1.addProperty("keyword", "#조승현");
//        keyword1.add("in_filters",inFilters);
//        keywords.add(keyword1);
        param.add("keywords", keywords);



        String request = gson.toJson(param);
        String responseMessage = RestCall.postJson("http://sc.wigo.ai:10015/nipars/v1/subject/analysis",request);

        System.out.println("mills second: " + (System.currentTimeMillis() - analysisStartTime));


        System.out.println("request\n " + request +"\n");
        System.out.println("responseMessage\n "+ responseMessage) ;

//        System.out.println(gson.toJson(gson.fromJson(responseMessage, JsonObject.class).get("media_networks")));
    }
}
