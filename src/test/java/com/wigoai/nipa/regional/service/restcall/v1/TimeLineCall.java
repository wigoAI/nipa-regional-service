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
 * 타임라인 결과 얻기
 * @author macle
 */
public class TimeLineCall {

    public static void main(String[] args) throws Exception {
        long analysisStartTime = System.currentTimeMillis();

        //7월 20일부터
        long startTime = new SimpleDateFormat("yyyyMMdd HH:mm:ss").parse("20190101 00:00:00").getTime();

        //7월 25일 전까지 (7월24일까지)
        long endTime = new SimpleDateFormat("yyyyMMdd HH:mm:ss").parse("20200930 00:00:00").getTime();

        //기준시는 반드시 통계 결과에서 사용한 값을 이용해야함.
        long standardTime = System.currentTimeMillis();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        JsonObject param = new JsonObject();
        param.addProperty("start_time", startTime);
        param.addProperty("end_time", endTime);
        param.addProperty("standard_time", standardTime);
        param.addProperty("start", 0);
        param.addProperty("end", 5);
        //단어 최대 건수설정 설정하지 않으면 30
//        param.addProperty("keyword_count", 30);


        String [] classifyNameArray = {
                "보건위생"
                , "건설교통"

        };

        JsonArray inKeywords= new JsonArray();

        for(String classifyName : classifyNameArray){
            inKeywords.add("#"+classifyName);
        }

        //키워드설정
        JsonArray keywords = new JsonArray();

        JsonObject keyword1 = new JsonObject();
        keyword1.addProperty("keyword", "고속도로");
//        keyword1.add("in_filters",inKeywords);
        JsonObject keyword2 = new JsonObject();
        keyword2.addProperty("keyword", "서울");
//        keyword2.add("in_filters",inKeywords);
        keywords.add(keyword1);
        keywords.add(keyword2);

        param.add("keywords", keywords);

        JsonArray channelGroups = new JsonArray();
        channelGroups.add("media");

        param.add("channel_groups", channelGroups);
        String request = gson.toJson(param);
//        String responseMessage = RestCall.postJson("http://127.0.0.1:33377/nipars/v1/search/timeline",request);
        String responseMessage = RestCall.postJson("http://sc.wigo.ai:10014/nipars/v1/search/timeline",request);


        System.out.println("mills second: " + (System.currentTimeMillis() - analysisStartTime));


        System.out.println("request\n " + request +"\n");
        System.out.println("responseMessage\n "+ responseMessage) ;
    }
}
