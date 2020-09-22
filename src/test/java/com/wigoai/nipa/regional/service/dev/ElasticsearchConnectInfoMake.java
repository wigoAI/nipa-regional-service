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

package com.wigoai.nipa.regional.service.dev;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * 엘라스틱 서치 연결정보 설정용
 * @author macle
 */
public class ElasticsearchConnectInfoMake {
    public static void main(String[] args) {
        Gson gson = new Gson();

        JsonArray infos = new JsonArray();

        JsonObject info1 = new JsonObject();
        info1.addProperty("host_address","10.10.1.122");
        info1.addProperty("port", 9200);
        infos.add(info1);
//
        JsonObject info2 = new JsonObject();
        info2.addProperty("host_address","10.10.1.123");
        info2.addProperty("port", 9200);
        infos.add(info2);



        System.out.println(gson.toJson(infos));

    }
}
