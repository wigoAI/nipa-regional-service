package com.wigoai.nipa.regional.service.restcall.channel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.seomse.commons.utils.time.Times;
import com.wigoai.rest.RestCall;

import java.text.SimpleDateFormat;

/**
 * 채널 통합분석 호출
 * @author macle
 */
public class ChannelIntegratedCall {

    public static void main(String[] args) throws Exception {
        long analysisStartTime = System.currentTimeMillis();

        long startTime = new SimpleDateFormat("yyyyMMdd HH:mm:ss").parse("20211120 00:00:00").getTime();

        long endTime = new SimpleDateFormat("yyyyMMdd HH:mm:ss").parse("20211124 00:00:00").getTime() + (Times.DAY_1 -1);


        //오늘포함
        // endTime = System.currentTimeMillis();

        long standardTime = System.currentTimeMillis();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        JsonObject param = new JsonObject();
        param.addProperty("start_time", startTime);
        param.addProperty("end_time", endTime);
        param.addProperty("standard_time", standardTime);

        //단어 최대 건수설정 설정하지 않으면 30
//        param.addProperty("keyword_count", 30);


        JsonArray channels = new JsonArray();
        channels.add("bfly_es_news_117");

        param.add("channels", channels);


        String request = gson.toJson(param);
//        String responseMessage = RestCall.postJson("http://127.0.0.1:33377/nipars/v1/integrated/analysis",request);
        String responseMessage = RestCall.postJson("http://sc.wigo.ai:10014/nipars/v1/channel/integrated",request);

        System.out.println("mills second: " + (System.currentTimeMillis() - analysisStartTime));
        System.out.println("request\n " + request +"\n");
        System.out.println("responseMessage\n "+ responseMessage) ;
    }
}
