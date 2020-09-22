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

package com.wigoai.nipa.regional.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.moara.ara.datamining.statistics.count.WordCount;
import org.moara.ara.datamining.textmining.document.Document;
import org.moara.common.code.CharSet;
import org.moara.common.config.Config;
import org.moara.common.data.database.jdbc.JdbcObjects;
import org.moara.common.data.file.FileUtil;
import org.moara.common.service.Service;
import org.moara.common.util.ExceptionUtil;
import org.moara.engine.MoaraEngine;
import org.moara.engine.console.EngineConsole;
import org.moara.keyword.KeywordAnalysis;
import org.moara.keyword.KeywordConfig;
import org.moara.keyword.ServiceKeywordAnalysis;
import org.moara.keyword.index.IndexData;
import org.moara.keyword.index.IndexDataMake;
import org.moara.keyword.index.IndexUtil;
import org.moara.keyword.index.KeywordJsonIndex;
import org.moara.keyword.search.ContentsGroup;
import org.moara.keyword.search.ContentsIndexData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 키워드 분석용 수집 서비스
 * @author macle
 */
public class KeywordAnalysisCollectService extends Service {

    private static final Logger logger = LoggerFactory.getLogger(KeywordAnalysisCollectService.class);
    

    private final EngineConfig engineConfig;

    /**
     * 생성자
     */
    public KeywordAnalysisCollectService(){
        super();
        MoaraEngine moaraEngine = MoaraEngine.getInstance();

        if(moaraEngine == null){
            String errorMessage = "engine null";
            logger.error(errorMessage);
            throw new RuntimeException(errorMessage);

        }

        engineConfig = new EngineConfig();
        engineConfig.engineCode = moaraEngine.getCode();
        String lastNumValue = EngineConsole.getConfig(engineConfig.engineCode, ServiceConfig.CONTENTS_LAST_NUM.key());

        if(lastNumValue == null){
            engineConfig.key = ServiceConfig.CONTENTS_LAST_NUM.key();
            engineConfig.value = "0";
            engineConfig.updateTime = System.currentTimeMillis();
            JdbcObjects.insertOrUpdate(engineConfig, false);

            lastNum = 0L;
        }else{
            lastNum = Long.parseLong(lastNumValue);
        }
    }

    @Override
    public void work() {

        for(;;){
            if(!collectToMakeIndex()){
                break;
            }
        }
    }

    private long lastNum ;

    /**
     * 데이터 수집 하여 index 정보 생성
     * @return boolean is data
     */
    boolean collectToMakeIndex(){

        //500개씩 가져오기
        try(Connection conn = NipaRegionalAnalysis.getInstance().getDataSource().getConnection()) {
            List<NipaRsContents> nipaContentsList = JdbcObjects.getObjList(conn, NipaRsContents.class, "CONTENTS_NO > " + lastNum + " ORDER BY CONTENTS_NO ASC LIMIT 0, 500");


            if (nipaContentsList.size() == 0) {
                logger.debug("data size 0 sleep");
                return false;
            }

            logger.debug("data size: " + nipaContentsList.size());



            lastNum = nipaContentsList.get(nipaContentsList.size() - 1).contentsNum;


            //인 메모리 데이터 생성
            KeywordAnalysis keywordAnalysis = ServiceKeywordAnalysis.getInstance().getKeywordAnalysis();

            Map<String, Map<String, IndexDataInfo>> ymdIdMap = new HashMap<>();

            long time = System.currentTimeMillis();

            int maxLength  = Config.getInteger(ServiceConfig.CONTENTS_MAX_LENGTH.key(),(int)ServiceConfig.CONTENTS_MAX_LENGTH.defaultValue());

            NipaRegionalAnalysis nipaRegionalAnalysis = NipaRegionalAnalysis.getInstance();

            for(NipaRsContents nipaContents : nipaContentsList){

                if(nipaContents.contents != null && nipaContents.contents.length() > maxLength) {
                    continue;
                }

                if(nipaContents.postTime == null){
                    nipaContents.postTime = time;
                }

                Document document = NipaRegionalAnalysis.makeDocument(nipaContents);

                IndexData indexData = IndexDataMake.getIndexDataDefault(document, Config.getInteger(KeywordConfig.MIN_SYLLABLE_LENGTH.key(),(int)KeywordConfig.MIN_SYLLABLE_LENGTH.defaultValue()));
//                indexData.putCustomData("title", nipaContents.title);

                WordCount[] wordCounts = indexData.getWordCounts();
                if(wordCounts == null || wordCounts.length == 0){
                    continue;
                }


                String ymd = new SimpleDateFormat("yyyyMMdd").format(new Date(nipaContents.postTime));
                String [] keys = new String[2];

                keys[0] = ymd;

                ChannelGroup channelGroup = nipaRegionalAnalysis.getChannelGroup(nipaContents.channelId);
                if(channelGroup == null){
                    keys[1] = Config.getConfig(ServiceConfig.CHANNEL_DEFAULT.key(), (String)ServiceConfig.CHANNEL_DEFAULT.defaultValue());
                }else{
                    keys[1] = channelGroup.id;
                }



                ContentsGroup contentsGroup = keywordAnalysis.getGroup(keys);

                if(contentsGroup == null){
                    contentsGroup = keywordAnalysis.newGroup(keys);
                }

                ContentsIndexData contentsIndexData = new ContentsIndexData();
                contentsIndexData.setType(ContentsIndexData.Type.ADD);
                contentsIndexData.setData(indexData);
                //메로리에 정보추가
                contentsGroup.addIndex(contentsIndexData);

                //파일 데이터 저장용 생성
                Map<String, IndexDataInfo> idMap = ymdIdMap.computeIfAbsent(ymd, k -> new HashMap<>());

                JSONArray keyArray = new JSONArray();
                for(String indexKey : keys){
                    keyArray.put(indexKey);
                }

                String id = Long.toString(nipaContents.contentsNum);

                IndexDataInfo info = new IndexDataInfo();
                info.indexData = indexData;
                info.keyArray = keyArray;
                idMap.put(id, info);
            }
            nipaContentsList.clear();

            Map<String,Integer> lastFileNumberMap = new HashMap<>();
            Set<String> ymdSet = ymdIdMap.keySet();

            for(String keyYmd : ymdSet){
                Collection<IndexDataInfo> indexDataColl = ymdIdMap.get(keyYmd).values();
                if(indexDataColl.size() == 0){
                    continue;
                }

                Integer lastNumber = lastFileNumberMap.get(keyYmd);
                if(lastNumber == null){
                    lastNumber = -1;
                }




                String filePath = IndexUtil.getWriteFilePath(keyYmd, lastNumber);


                File file = new File(filePath);
                String fileName = file.getName();
                lastFileNumberMap.put(keyYmd, IndexUtil.getFileNumber(fileName));

                StringBuilder sb = new StringBuilder();
                for(IndexDataInfo data : indexDataColl){
                    data.indexData.setIndexFileName(fileName);
                    JSONObject jsonObj = new JSONObject();
                    data.indexData.setJSONObject(jsonObj);
                    jsonObj.put(KeywordJsonIndex.INDEX_KEYS, data.keyArray);
                    sb.append(jsonObj.toString()).append("\n");
                }
                FileUtil.fileOutput(sb.toString(), CharSet.UTF8, filePath, true);
                indexDataColl.clear();
            }
            lastFileNumberMap.clear();
            ymdIdMap.clear();


            engineConfig.key = ServiceConfig.CONTENTS_LAST_NUM.key();
            engineConfig.value = Long.toString(lastNum);
            engineConfig.updateTime = System.currentTimeMillis();
            JdbcObjects.insertOrUpdate(engineConfig, false);

            return true;

        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
            return false;
        }
    }

    private static class IndexDataInfo{

        IndexData indexData;
        JSONArray keyArray;
    }
}
