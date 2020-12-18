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

package com.wigoai.nipa.regional.service.restcall;

import com.wigoai.rest.RestCall;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author macle
 */
public class SyncAnalysisRestCall {

    public static void main(String[] args) {

        long analysisStartTime = System.currentTimeMillis();


        JSONObject param = new JSONObject();
        param.put("start_ymd", "20201209");
        param.put("end_ymd", "20201225");

        JSONArray media = new JSONArray();
        media.put("community");

        JSONArray keywords = new JSONArray();

        keywords.put("코로나");

        param.put("keywords", keywords);
        param.put("media", media);
        JSONArray modules = new JSONArray();
        modules.put("TF_WORD");
        modules.put("TFIDF");
        param.put("modules", modules);

        JSONArray stopwords = new JSONArray();
        stopwords.put("조성");
        stopwords.put("기자");
        param.put("stopwords", stopwords);

        String responseMessage = RestCall.postJson("http://127.0.0.1:33377/keyword/v1/sync/analysis",param.toString());

        System.out.println("analysis mills second: " + (System.currentTimeMillis() - analysisStartTime));
        System.out.println(responseMessage);

    }
}
