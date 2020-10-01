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

package com.wigoai.nipa.regional.service.controller.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.seomse.commons.utils.FileUtil;
import com.wigoai.nipa.regional.service.ChannelGroup;
import com.wigoai.nipa.regional.service.NipaRegionalAnalysis;
import com.wigoai.nipa.regional.service.ServiceConfig;
import com.wigoai.nipa.regional.service.util.GroupKeyUtil;
import org.json.JSONObject;
import org.moara.ara.datamining.data.CodeName;
import org.moara.common.config.Config;
import org.moara.common.util.ExceptionUtil;
import org.moara.common.util.YmdUtil;
import org.moara.keyword.KeywordAnalysis;
import org.moara.keyword.ServiceKeywordAnalysis;
import org.moara.keyword.index.IndexData;
import org.moara.keyword.index.IndexUtil;
import org.moara.keyword.search.data.SearchData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 데이터 검색
 * @author macle
 */
@RestController
public class DataSearchDataController {

    private static final Logger logger = LoggerFactory.getLogger(DataSearchDataController.class);

    private final String nullResult = "{\n" +
            "  \"total\": 0,\n" +
            "  \"data_array\": []\n" +
            "}";


    /**
     * 타임라인 결과 얻기
     * @param jsonValue String json object
     * @return String json object
     */
    @RequestMapping(value = "/nipars/v1/search/timeline" , method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public String timeLine(@RequestBody final String jsonValue){

        try {
            JSONObject request = new JSONObject(jsonValue);
            SearchData searchData = search(request);
            if(searchData == null){
                return nullResult;
            }


            IndexData [] searchArray = searchData.getDataArray();


            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject response = new JsonObject();
            response.addProperty("total", searchArray.length);
            String classifyCode = Config.getConfig(ServiceConfig.FIELD_CLASSIFY.key());


            IndexData[] subArray = IndexUtil.subData(searchArray, request.getInt("start") , request.getInt("end"));
            JsonArray dataArray = new JsonArray();
            for(IndexData data : subArray){
                JsonObject obj =  makeObj(gson, data);
                JsonArray classifies = new JsonArray();
                CodeName [] codeNames = data.getClassifies();
                for(CodeName codeName : codeNames){
                    if(!codeName.getCode().startsWith(classifyCode)){
                        continue;
                    }
                    JsonObject classifyObj = new JsonObject();
                    classifyObj.addProperty("code", codeName.getCode());
                    classifyObj.addProperty("name", codeName.getName());
                    classifies.add(classifyObj);

                }



                obj.add("classifies", classifies);
                dataArray.add(obj);
            }

            response.add("data_array", dataArray);
            return gson.toJson(response);
        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
            return nullResult;
        }

    }

    /**
     * 키워드 검색 결과 얻기
     * @param jsonValue String json object
     * @return String json object
     */
    @RequestMapping(value = "/nipars/v1/search/contents" , method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public String contents(@RequestBody final String jsonValue){
        try {
            JSONObject request = new JSONObject(jsonValue);
            SearchData searchData = search(request);
            if(searchData == null){
                return nullResult;
            }

            IndexData [] searchArray = searchData.getDataArray();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject response = new JsonObject();
            response.addProperty("total", searchArray.length);


            IndexData[] subArray = IndexUtil.subData(searchArray, request.getInt("start") , request.getInt("end"));
            JsonArray dataArray = new JsonArray();

            for(IndexData data : subArray){
                JsonObject obj =  makeObj(gson, data);

                dataArray.add(obj);
            }

            response.add("data_array", dataArray);
            return gson.toJson(response);

        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
            return nullResult;
        }
    }

    private JsonObject makeObj(Gson gson, IndexData data){

        JsonObject obj =  gson.fromJson(FileUtil.getLine(data.getDetailFilePath(), data.getDetailLine()),JsonObject.class);
        obj.addProperty("id", data.getId());
//        obj.remove("analysis_contents");

        return obj;
    }


    /**
     * 데이터 검색겱과 얻기
     * @param request JSONObject
     * @return  IndexData[]
     */
    public SearchData search(final JSONObject request){
        long startTime = request.getLong("start_time");
        long endTime = request.getLong("end_time");
        long standardTime = request.getLong("standard_time");
        long analysisMaxTime = Config.getLong(ServiceConfig.ANALYSIS_MAX_TIME.key(), (long)ServiceConfig.ANALYSIS_MAX_TIME.defaultValue());

        ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
        KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();

        String startYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(startTime));
        String endYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(endTime-1));

        NipaRegionalAnalysis nipaRegionalAnalysis = NipaRegionalAnalysis.getInstance();
        ChannelGroup[] groups = nipaRegionalAnalysis.getGroups();

        List<String> ymdList = YmdUtil.getYmdList(startYmd,endYmd);
        String [][] keysArray = GroupKeyUtil.makeKeysArray(ymdList, groups);
        return  keywordAnalysis.dataSearch(startTime, endTime, standardTime, request.getJSONArray("keywords").toString(),keysArray , analysisMaxTime);

    }

}