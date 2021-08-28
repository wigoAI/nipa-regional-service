package com.wigoai.nipa.regional.service.controller.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wigoai.nipa.regional.service.util.ParameterUtil;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.moara.common.util.ExceptionUtil;
import org.moara.common.util.YmdUtil;
import org.moara.keyword.KeywordAnalysis;
import org.moara.keyword.ServiceKeywordAnalysis;
import org.moara.keyword.search.SearchKeyword;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 매체(뉴스) 분석
 * 기자별 분석
 * rest controller
 * api v1
 * @author macle
 */
@Slf4j
@RestController
public class MediaAnalysisController {


    @RequestMapping(value = "/nipars/v1/media/reporter" , method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public String reporter(@RequestBody final String jsonValue) {
        try{

            JSONObject request = new JSONObject(jsonValue);

            long startTime = request.getLong("start_time");
            long endTime = request.getLong("end_time");
            long standardTime = request.getLong("standard_time");
            String startYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(startTime));
            String endYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(endTime-1));

            //날짜정보
            List<String> ymdList = YmdUtil.getYmdList(startYmd,endYmd);

            Map<String, Object> parameterMap = ParameterUtil.makeParameterMap(request);
            ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
            KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();

            String keywordJson = request.getJSONArray("keywords").toString();
            SearchKeyword[] searchKeywords = keywordAnalysis.makeSearchKeywords(new JSONArray(keywordJson));


            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            return gson.toJson(MediaAnalysis.reporter(startTime,endTime,standardTime,searchKeywords,ymdList,parameterMap, request.getString("channel_id")));
        }catch(Exception e){
            log.error(ExceptionUtil.getStackTrace(e));
            return "[]";
        }


    }
}
