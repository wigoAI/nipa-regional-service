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
 * 주제별분석
 * @author macle
 */
public class SubjectAnalysisJsonCall {
    public static void main(String[] args) {



        String jsonText ="{\n" +
                "  \"start_time\": 1610982000000,\n" +
                "  \"end_time\": 1611054466744,\n" +
                "  \"standard_time\":" +  System.currentTimeMillis() +",\n" +
                "  \"emotion_keyword_count\": \"10\",\n" +
                "  \"cloud_keyword_count\": \"15\",\n" +
                "  \"sna_use_count\": \"15\",\n" +
                "  \"sna_source_count\": \"15\",\n" +
                "  \"sna_target_count\": \"5\",\n" +
                "  \"classify_names\": [\n" +
                "    \"보건위생\",\n" +
                "    \"재난안전\",\n" +
                "    \"청소환경\"\n" +
                "  ],\n" +
                "  \"keywords\": [\n" +
                "    {\n" +
                "      \"keyword\": \"강원\",\n" +
                "      \"in_filters\": [\n" +
                "        [\n" +
                "          \"#유튜브\",\n" +
//                "          \"#블로그검색\"\n" +
                "        ],\n" +
                "        [\n" +
                "          \"#재난안전\",\n" +
                "          \"#블로그검색\"\n" +
                "        ],\n" +
                "        [\n" +
                "          \"#청소환경\",\n" +
                "          \"#블로그검색\"\n" +
                "        ]\n" +
                "      ],\n" +
                "      \"out_filters\": []\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        String responseMessage = RestCall.postJson("http://127.0.0.1:33377/nipars/v1/subject/analysis",jsonText);
//        String responseMessage = RestCall.postJson("http://sc.wigo.ai:10015/nipars/v1/subject/analysis", jsonText);

        System.out.println("request\n " + jsonText +"\n");
        System.out.println("responseMessage\n "+ responseMessage) ;
    }
}
