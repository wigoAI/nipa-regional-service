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
package com.wigoai.nipa.regional.service.restcall.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.seomse.commons.utils.time.Times;
import com.wigoai.rest.RestCall;

import java.text.SimpleDateFormat;
/**
 * 인물분석 개체명 분분석 호출 예제
 * @author macle
 */
public class CharacterNerCall {

    public static void main(String[] args) throws Exception {
        long analysisStartTime = System.currentTimeMillis();

        long startTime = new SimpleDateFormat("yyyyMMdd HH:mm:ss").parse("20210801 00:00:00").getTime();

        long endTime = new SimpleDateFormat("yyyyMMdd HH:mm:ss").parse("20210810 00:00:00").getTime() + (Times.DAY_1 -1);

        long standardTime = System.currentTimeMillis();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        JsonObject param = new JsonObject();
        param.addProperty("start_time", startTime);
        param.addProperty("end_time", endTime);
        param.addProperty("standard_time", standardTime);
        param.addProperty("ner_count", 10);


        param.addProperty("name","문재인");

        JsonArray infos = new JsonArray();
        infos.add("코로나");

        param.add("infos", infos);

        String request = gson.toJson(param);
        String responseMessage = RestCall.postJson("http://sc.wigo.ai:10015/nipars/v1/character/ner",request);

        System.out.println("mills second: " + (System.currentTimeMillis() - analysisStartTime));
        System.out.println("request\n " + request +"\n");
        System.out.println("responseMessage\n "+ responseMessage) ;
    }
}
