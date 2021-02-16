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
import org.json.JSONArray;
import org.json.JSONObject;
import org.moara.ara.datamining.data.CodeName;
import org.moara.ara.datamining.statistics.count.WordCount;
import org.moara.ara.datamining.textmining.api.document.DocumentStandardKey;
import org.moara.common.config.Config;
import org.moara.common.util.ExceptionUtil;
import org.moara.common.util.YmdUtil;
import org.moara.keyword.KeywordAnalysis;
import org.moara.keyword.ServiceKeywordAnalysis;
import org.moara.keyword.index.IndexData;
import org.moara.keyword.index.IndexUtil;
import org.moara.keyword.search.data.LikeIndexData;
import org.moara.keyword.search.data.LikeSearchData;
import org.moara.keyword.search.data.SearchData;
import org.moara.keyword.search.data.SearchHighlight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 데이터 검색
 * @author macle
 */
@RestController
public class DataSearchController {

    private static final Logger logger = LoggerFactory.getLogger(DataSearchController.class);

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
            if(searchData == null|| searchData.getDataArray().length == 0){
                return nullResult;
            }

            IndexData [] searchArray = searchData.getDataArray();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject response = new JsonObject();
            response.addProperty("total", searchArray.length);
            String classifyCode = Config.getConfig(ServiceConfig.FIELD_CLASSIFY.key());


            int begin;
            if(request.isNull("begin")){
                begin = request.getInt("start");
            }else{
                begin  = request.getInt("begin");
            }

            IndexData[] subArray = IndexUtil.subData(searchArray, begin, request.getInt("end"));
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
                obj.remove("analysis_contents");

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

            logger.debug(jsonValue);

            JSONObject request = new JSONObject(jsonValue);
            SearchData searchData = search(request);
            if(searchData == null || searchData.getDataArray().length == 0){
                return nullResult;
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject response = new JsonObject();

            String preTag;
            if(request.has("pre_tag")){
                preTag = request.getString("pre_tag");
            }else{
                preTag ="<em>";
            }

            String postTag;
            if(request.has("post_tag")){
                postTag = request.getString("post_tag");
            }else{
                postTag ="</em>";
            }

            int maxLength;
            if(request.has("highlight_max_length")){
                maxLength = request.getInt("highlight_max_length");
            }else{
                maxLength = 120;
            }

            String [] highlightKeywords = request.getString("highlight_keyword").trim().split(" ");

            IndexData [] searchArray = searchData.getDataArray();

            String likeKeyword  = null;

            if(request.has("like_keyword")){
                likeKeyword = request.getString("like_keyword").trim();
            }


            JsonArray dataArray = new JsonArray();

            int start = request.getInt("start");
            int end =  request.getInt("end");

            if(likeKeyword != null && likeKeyword.length() > 0){
                long analysisMaxTime = Config.getLong(ServiceConfig.ANALYSIS_MAX_TIME.key(), (long)ServiceConfig.ANALYSIS_MAX_TIME.defaultValue());

                ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
                KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();

                LikeSearchData likeSearchData = keywordAnalysis.likeSearch(searchData, likeKeyword, analysisMaxTime);

                if(likeSearchData == null ){
                    return nullResult;
                }


                LikeIndexData[] likeDataArray = likeSearchData.getLikeDataArray();
                response.addProperty("total", likeDataArray.length);

                LikeIndexData [] subArray = likeSearchData.subData(start, end);


                for(LikeIndexData likeIndexData : subArray){

                    IndexData data = likeIndexData.getIndexData();

                    JsonObject obj =  makeObj(gson, data);
                    String analysisContents = obj.remove("analysis_contents").getAsString();

                    try {
                        obj.addProperty("highlight", SearchHighlight.highlight(data, analysisContents, highlightKeywords, preTag, postTag, maxLength, likeIndexData.getTextIndexArray(likeKeyword)).replace('\n', ' '));
                    }catch(Exception e){
                        StringBuilder sb = new StringBuilder();
                        for(String keyword : highlightKeywords){
                            sb.append(",").append(keyword);
                        }
                        logger.error("highlight error id: " + data.getId() + " keywords: " + sb.toString() + " length: " + maxLength + ", pre: " + preTag + ", post: " + postTag);

                    }
                    dataArray.add(obj);
                }

            }else{

                response.addProperty("total", searchArray.length);

                IndexData[] subArray = IndexUtil.subData(searchArray, start, end);

                for(IndexData data : subArray){
                    JsonObject obj =  makeObj(gson, data);

                    //분석 정보를 가져와서 하이라이트 정보 생성하기
                    String analysisContents = obj.remove("analysis_contents").getAsString();

                    try {
                        obj.addProperty("highlight", SearchHighlight.highlight(data, analysisContents, highlightKeywords, preTag, postTag, maxLength).replace('\n', ' '));
                    }catch(Exception e){
                        StringBuilder sb = new StringBuilder();
                        for(String keyword : highlightKeywords){
                            sb.append(",").append(keyword);
                        }
                        logger.error("highlight error id: " + data.getId() + " keywords: " + sb.toString() + " length: " + maxLength + ", pre: " + preTag + ", post: " + postTag);

                    }
                    dataArray.add(obj);
                }

            }

            response.add("data_array", dataArray);



            return gson.toJson(response);

        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
            return nullResult;
        }
    }


    private JsonObject makeObj(Gson gson, IndexData data){
        JsonObject obj =  gson.fromJson(FileUtil.getLine(data.getIndexFileName(), data.getIndexFileLine()),JsonObject.class).getAsJsonObject(IndexData.Keys.DETAIL.key());
        obj.remove(IndexData.Keys.WORD_POSITIONS.key());
        obj.remove(IndexData.Keys.SENTENCE_POSITIONS.key());
        obj.remove(DocumentStandardKey.LANG_CODE.key());
        obj.remove(DocumentStandardKey.DOC_TYPE.key());

        String channelGroupId = data.getIndexKeys()[1];
        obj.addProperty("channel_group_id", channelGroupId);
        obj.addProperty("channel_group_nm", NipaRegionalAnalysis.getInstance().getGroup(channelGroupId).getName());
        obj.addProperty("id", data.getId());
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

        String [] groupIds;
        if(request.has("channel_groups")){
            JSONArray ids = request.getJSONArray("channel_groups");
            groupIds = new String[ids.length()];
            for (int i = 0; i <groupIds.length ; i++) {
                groupIds[i] = ids.getString(i);
            }
        }else{
            NipaRegionalAnalysis nipaRegionalAnalysis = NipaRegionalAnalysis.getInstance();
            ChannelGroup[] groups = nipaRegionalAnalysis.getGroups();
            groupIds = new String[groups.length];
            for (int i = 0; i <groupIds.length ; i++) {
                groupIds[i] = groups[i].getId();
            }
        }
        List<String> ymdList = YmdUtil.getYmdList(startYmd,endYmd);
        String [][] keysArray = GroupKeyUtil.makeKeysArray(ymdList, groupIds);

        return  keywordAnalysis.dataSearch(startTime, endTime, standardTime, request.getJSONArray("keywords").toString(), keysArray , analysisMaxTime);

    }


    @RequestMapping(value = "/nipars/v1/search/data" , method = RequestMethod.GET)
    public String search(@RequestParam final String id){
        KeywordAnalysis keywordAnalysis = ServiceKeywordAnalysis.getInstance().getKeywordAnalysis();

        IndexData data = keywordAnalysis.search(id);

        if(data == null){
            return "{}";
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject obj =  makeObj(gson, data);
        JsonArray classifies = new JsonArray();
        CodeName [] codeNames = data.getClassifies();
        for(CodeName codeName : codeNames){
            JsonObject classifyObj = new JsonObject();
            classifyObj.addProperty("code", codeName.getCode());
            classifyObj.addProperty("name", codeName.getName());
            classifies.add(classifyObj);

        }

        obj.remove("analysis_contents");
        obj.add("classifies", classifies);
        JsonArray tokens = new JsonArray();
        WordCount [] wordCounts = data.getWordCounts();
        Arrays.sort(wordCounts, WordCount.SORT_DESC);

        for(WordCount wordCount : wordCounts){
            tokens.add(gson.fromJson(wordCount.toJsonString(),JsonObject.class));
        }
        obj.add("tokens", tokens);

        return gson.toJson(obj);
    }

}