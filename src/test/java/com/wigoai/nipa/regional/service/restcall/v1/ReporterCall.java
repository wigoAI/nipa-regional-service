package com.wigoai.nipa.regional.service.restcall.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.seomse.commons.utils.time.Times;
import com.wigoai.rest.RestCall;

import java.text.SimpleDateFormat;

/**
 * 기자별 현황호출 예제
 * @author macle
 */
public class ReporterCall {
    public static void main(String[] args) throws Exception {
        long analysisStartTime = System.currentTimeMillis();


        long startTime = new SimpleDateFormat("yyyyMMdd HH:mm:ss").parse("20190101 00:00:00").getTime();

        long endTime = new SimpleDateFormat("yyyyMMdd HH:mm:ss").parse("20210815 00:00:00").getTime() + (Times.DAY_1 -1);

        //오늘포함
        // endTime = System.currentTimeMillis();

        long standardTime = System.currentTimeMillis();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        JsonObject param = new JsonObject();
        param.addProperty("start_time", startTime);
        param.addProperty("end_time", endTime);
        param.addProperty("standard_time", standardTime);
        param.addProperty("channel_id", "bfly_es_news_063");
        //단어 최대 건수설정 설정하지 않으면 30
//        param.addProperty("keyword_count", 30);
        JsonArray keywords = new JsonArray();
        keywords.add("코로나");
        keywords.add("강원도");
        keywords.add("춘천");
        param.add("keywords", keywords);

        String request = gson.toJson(param);
//        String responseMessage = RestCall.postJson("http://127.0.0.1:33377/nipars/v1/integrated/analysis",request);
        String responseMessage = RestCall.postJson("http://sc.wigo.ai:10015/nipars/v1/media/reporter",request);

        System.out.println("mills second: " + (System.currentTimeMillis() - analysisStartTime));
        System.out.println("request\n " + request +"\n");
        System.out.println("responseMessage\n "+ responseMessage) ;
    }
}
