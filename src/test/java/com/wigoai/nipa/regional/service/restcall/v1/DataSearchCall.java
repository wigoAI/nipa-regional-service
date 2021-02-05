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

import com.wigoai.rest.RestCall;
import org.json.JSONObject;

/**
 * @author macle
 */
public class DataSearchCall {
    public static void main(String[] args) {


        String jsonText = "{\n" +
                "  \"start_time\": 1609426800000,\n" +
                "  \"end_time\": 1612105199999,\n" +
                "  \"standard_time\":" +  System.currentTimeMillis() +",\n" +
                "  \"start\": 0,\n" +
                "  \"end\": 10,\n" +
                "  \"like_keyword\": \"아침에\",\n" +
                "  \"highlight_keyword\": \"아침에\",\n" +
                "  \"highlight_max_length\": \"160\",\n" +
                "  \"pre_tag\": \"\\u003cspan class\\u003d\\\"point\\\"\\u003e\",\n" +
                "  \"post_tag\": \"\\u003c/span\\u003e\",\n" +
                "  \"channel_groups\": [\n" +
                "    \"community\"\n" +
                "  ],\n" +
                "  \"keywords\": [\n" +
                "    {\n" +
                "      \"keyword\": \"홍천\",\n" +
                "      \"in_filters\": [\n" +
                "        [\n" +
                "          \"#보건위생\"\n" +
                "        ],\n" +
                "        [\n" +
                "          \"#재난안전\"\n" +
                "        ],\n" +
                "        [\n" +
                "          \"#청소환경\"\n" +
                "        ]\n" +
                "      ],\n" +
                "      \"out_filters\": []\n" +
                "    }\n" +
                "  ]\n" +
                "}";

//        String responseMessage = RestCall.postJson("http://127.0.0.1:10014/nipars/v1/search/contents", jsonText);

        String responseMessage = RestCall.postJson("http://sc.wigo.ai:10014/nipars/v1/search/contents", jsonText);

//        System.out.println(YmdUtil.getTime("20200101"));
//
//        System.out.println(YmdUtil.getTime("20210119"));

        System.out.println("request\n " + jsonText +"\n");
        System.out.println("responseMessage\n "+ responseMessage) ;
    }
}
