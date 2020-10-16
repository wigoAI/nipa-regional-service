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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.wigoai.rest.RestCall;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 기본 api사용 예제
 * @author macle
 */
public class TestAnalysis {


    public static void main(String[] args) {

        String url = "http://127.0.0.1:33377";
//        String url = "http://sc.wigo.ai:10014";


        JSONObject param = new JSONObject();
        param.put("start_ymd", "20200701");
        param.put("end_ymd", "20200930");

        JSONArray media = new JSONArray();
//        media.put("media");
        media.put("community");

        JSONArray keywords = new JSONArray();

        keywords.put("환경");

        param.put("keywords", keywords);
        param.put("media", media);


        //모듈을 설정하여 보낼때
        //설정하지 않으면 기본설정을 따름
        //인삭할 수 있는 모듈
        //TF_WORD, TF_CLASSIFY, TF_CONTENTS, SNA, LDA
        JSONArray modules = new JSONArray();
//        modules.put("SNA_LITE");
//        modules.put("TFIDF");
//        modules.put("TF_WORD");
//        modules.put("TF_CLASSIFY");
//        modules.put("TF_CONTENTS");
//        modules.put("SNA");
//        modules.put("LDA");
        //모듈을 따로설정하지 않고 기본설정을 따르는 경우 추가하지 않아도 됨

        JSONObject moduleDetail = new JSONObject();
        moduleDetail.put("module", "SNA_LITE");
        moduleDetail.put("group_percent", 0.80);
        modules.put(moduleDetail);

        param.put("modules", modules);

        JSONArray stopwords = new JSONArray();
//        stopwords.put("침체");
//        stopwords.put("조성");
        stopwords.put("기자");
        stopwords.put("하지");
        param.put("stopwords", stopwords);

        System.out.println(param.toString());

        //분석 아이디 얻기
        String analysisId =  RestCall.postJson(url + "/keyword/analysis",param.toString());

        System.out.println("analysis id: " + analysisId);

        //분석 아이디를 얻은후 관련 메시지 얻기
        boolean isEnd = false;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        for (int i = 0; i <2000 ; i++) {

            String jsonValue = RestCall.postJson(url + "/keyword/message", analysisId);

            JSONObject jsonObject = new JSONObject(jsonValue);

            JSONArray messageArray = jsonObject.getJSONArray("messages");
            for (int j = 0; j <messageArray.length() ; j++) {
                String message = messageArray.getString(j);
                System.out.println("============================================== message ==============================================\n");
                System.out.println(gson.toJson(gson.fromJson(message, JsonObject.class)));
            }

            if(jsonObject.getBoolean("is_end")){
                isEnd = true;
                break;
            }

            try{
                Thread.sleep(100);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        if(!isEnd){
            System.out.println("error time out");
        }

    }

}
