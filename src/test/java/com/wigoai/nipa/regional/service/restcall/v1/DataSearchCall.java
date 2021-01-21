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

/**
 * @author macle
 */
public class DataSearchCall {
    public static void main(String[] args) {




        String jsonText =" {\n" +
                "  \"start_time\": 1608390000000,\n" +
                "  \"end_time\": 1611021751397,\n" +
                "  \"standard_time\":" +  System.currentTimeMillis() +",\n" +
                "  \"start\": 0,\n" +
                "  \"end\": 6679,\n" +
                "  \"highlight_keyword\": \"강원도 춘천시\",\n" +
                "  \"highlight_max_length\": \"160\",\n" +
                "  \"pre_tag\": \"\\u003cspan class\\u003d\\\"point\\\"\\u003e\",\n" +
                "  \"post_tag\": \"\\u003c/span\\u003e\",\n" +
                "  \"channel_groups\": [\n" +
                "    \"community\"\n" +
                "  ],\n" +
                "  \"keywords\": [\n" +
                "    {\n" +
                "      \"keyword\": \"강원도\",\n" +
                "      \"in_filters\": [\n" +
                "        [\n" +
                "          \"#블로그검색\"\n" +
                "        ],\n" +
                "        [\n" +
                "          \"#블로그검색\"\n" +
                "        ],\n" +
                "        [\n" +
                "          \"#블로그검색\"\n" +
                "        ]\n" +
                "      ],\n" +
                "      \"out_filters\": []\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        String responseMessage = RestCall.postJson("http://127.0.0.1:33377/nipars/v1/search/contents", jsonText);

//        String responseMessage = RestCall.postJson("http://sc.wigo.ai:10015/nipars/v1/search/contents", jsonText);

//        System.out.println(YmdUtil.getTime("20200101"));
//
//        System.out.println(YmdUtil.getTime("20210119"));

        System.out.println("request\n " + jsonText +"\n");
        System.out.println("responseMessage\n "+ responseMessage) ;
    }
}
