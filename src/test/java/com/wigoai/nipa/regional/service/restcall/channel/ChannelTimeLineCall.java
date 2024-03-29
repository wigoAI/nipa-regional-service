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

package com.wigoai.nipa.regional.service.restcall.channel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.seomse.commons.utils.time.Times;
import com.wigoai.rest.RestCall;

import java.text.SimpleDateFormat;

/**
 * 타임라인 결과 얻기
 * @author macle
 */
public class ChannelTimeLineCall {

    public static void main(String[] args) throws Exception {
        long analysisStartTime = System.currentTimeMillis();

        long startTime = new SimpleDateFormat("yyyyMMdd HH:mm:ss").parse("20211120 00:00:00").getTime();

        long endTime = new SimpleDateFormat("yyyyMMdd HH:mm:ss").parse("20211124 00:00:00").getTime() + (Times.DAY_1 -1);

        //기준시는 반드시 통계 결과에서 사용한 값을 이용해야함.
        long standardTime = System.currentTimeMillis();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        JsonObject param = new JsonObject();
        param.addProperty("start_time", startTime);
        param.addProperty("end_time", endTime);
        param.addProperty("standard_time", standardTime);
        param.addProperty("start", 0);
        param.addProperty("end", 5);

        JsonArray channels = new JsonArray();
        channels.add("bfly_es_news_117");

        param.add("channels", channels);
//        //키워드설정
        JsonArray keywords = new JsonArray();
        keywords.add("#보건위생");
        param.add("keywords", keywords);

        String request = gson.toJson(param);
//        String responseMessage = RestCall.postJson("http://127.0.0.1:33377/nipars/v1/search/timeline",request);
        String responseMessage = RestCall.postJson("http://sc.wigo.ai:10014/nipars/v1/channel/timeline",request);


        System.out.println("mills second: " + (System.currentTimeMillis() - analysisStartTime));


        System.out.println("request\n " + request +"\n");
        System.out.println("responseMessage\n "+ responseMessage) ;
    }
}
