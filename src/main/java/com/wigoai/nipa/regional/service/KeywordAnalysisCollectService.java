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

import com.seomse.commons.utils.FileUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.moara.ara.datamining.data.CodeName;
import org.moara.ara.datamining.statistics.count.WordCount;
import org.moara.ara.datamining.textmining.document.Document;
import org.moara.common.code.CharSet;
import org.moara.common.config.Config;
import org.moara.common.data.database.jdbc.JdbcObjects;
import org.moara.common.data.database.jdbc.PrepareStatementData;
import org.moara.common.service.Service;
import org.moara.common.util.ExceptionUtil;
import org.moara.engine.MoaraEngine;
import org.moara.keyword.KeywordAnalysis;
import org.moara.keyword.KeywordConfig;
import org.moara.keyword.ServiceKeywordAnalysis;
import org.moara.keyword.index.IndexData;
import org.moara.keyword.index.IndexDataMake;
import org.moara.keyword.index.IndexUtil;
import org.moara.keyword.index.KeywordJsonIndex;
import org.moara.keyword.search.ContentsGroup;
import org.moara.keyword.search.ContentsIndexData;
import org.moara.meta.MetaDataUtil;
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

    private final Map<String, String> channelNameMap = new HashMap<>();

    /**
     * 생성자
     */
    public KeywordAnalysisCollectService() {
        super();
        MoaraEngine moaraEngine = MoaraEngine.getInstance();

        if (moaraEngine == null) {
            String errorMessage = "engine null";
            logger.error(errorMessage);
            throw new RuntimeException(errorMessage);

        }

        engineConfig = new EngineConfig();
        engineConfig.engineCode = moaraEngine.getCode();



        String lastNumValue = MoaraEngine.getInstance().getConfig(ServiceConfig.CONTENTS_LAST_NUM.key());
//                EngineConsole.getConfig(engineConfig.engineCode, ServiceConfig.CONTENTS_LAST_NUM.key());

        if (lastNumValue == null) {
            engineConfig.key = ServiceConfig.CONTENTS_LAST_NUM.key();
            engineConfig.value = "0";
            engineConfig.updateTime = System.currentTimeMillis();
            JdbcObjects.insertOrUpdate(engineConfig, false);

            lastNum = 0L;
        } else {
            lastNum = Long.parseLong(lastNumValue);
        }



    }

    @Override
    public void work() {

        for (; ; ) {
            if (!collectToMakeIndex()) {
                break;
            }
        }
    }

    private long lastNum;

    private long channelUpdateTime = 0L;

    /**
     * 데이터 수집 하여 index 정보 생성
     *
     * @return boolean is data
     */
    boolean collectToMakeIndex() {

        //개발용 임시소스 (분류모델 생성 전 더미데이터)
        CodeName[] emotionArray = new CodeName[3];
        emotionArray[0] = new CodeName("U716001","긍정");
        emotionArray[1] = new CodeName("U716002","중립");
        emotionArray[2] = new CodeName("U716003","부정");
        CodeName [] classifies = new CodeName[7];
        classifies[0] = new CodeName("U718001","보건위생");
        classifies[1] = new CodeName("U718002","재난안전");
        classifies[2] = new CodeName("U718003","청소환경");
        classifies[3] = new CodeName("U718004","건설교통");
        classifies[4] = new CodeName("U718005","교육");
        classifies[5] = new CodeName("U718006","경제산업");
        classifies[6] = new CodeName("U718007","기타");
        Random random = new Random();

        String emotionClassify = Config.getConfig(ServiceConfig.EMOTION_CLASSIFY.key());

        //500개씩 가져오기
        try (Connection conn = NipaRegionalAnalysis.getInstance().getDataSource().getConnection()) {

            List<CrawlingChannel> channelList;

            if(channelUpdateTime == 0L){
                channelList = JdbcObjects.getObjList(conn, CrawlingChannel.class, "DEL_FG = 'N'");
            }else{
                Map<Integer, PrepareStatementData> prepareStatementDataMap = MetaDataUtil.newTimeMap(channelUpdateTime);
                channelList = JdbcObjects.getObjList(conn, CrawlingChannel.class, null,"UPT_DT > ? AND DEL_FG = 'N'", prepareStatementDataMap);
            }

            if(channelList.size() > 0){
                for(CrawlingChannel crawlingChannel : channelList){
                    channelNameMap.put(crawlingChannel.id, crawlingChannel.name);
                    if(crawlingChannel.updateTime > channelUpdateTime){
                        channelUpdateTime = crawlingChannel.updateTime;
                    }
                }
                channelList.clear();
            }


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

            int maxLength = Config.getInteger(ServiceConfig.CONTENTS_MAX_LENGTH.key(), (int) ServiceConfig.CONTENTS_MAX_LENGTH.defaultValue());

            NipaRegionalAnalysis nipaRegionalAnalysis = NipaRegionalAnalysis.getInstance();

            List<NipaData> addDataList = new ArrayList<>();

            for (NipaRsContents nipaContents : nipaContentsList) {

                if (nipaContents.contents != null && nipaContents.contents.length() > maxLength) {
                    continue;
                }

                if (nipaContents.postTime == null) {
                    nipaContents.postTime = time;
                }

                Document document = NipaRegionalAnalysis.makeDocument(nipaContents);

                IndexData indexData = IndexDataMake.getIndexDataDefault(document, Config.getInteger(KeywordConfig.MIN_SYLLABLE_LENGTH.key(), (int) KeywordConfig.MIN_SYLLABLE_LENGTH.defaultValue()));
                WordCount[] wordCounts = indexData.getWordCounts();
                if (wordCounts == null || wordCounts.length == 0) {
                    continue;
                }

                //개발용 임시소스 (분류모델 생성 전 더미데이터)
                CodeName [] codeNames = new CodeName[2];
                codeNames[0] = emotionArray[random.nextInt(emotionArray.length)];
                codeNames[1] = classifies[random.nextInt(classifies.length)];
                indexData.setClassifies(codeNames);


                String ymd = new SimpleDateFormat("yyyyMMdd").format(new Date(nipaContents.postTime));
                String[] keys = new String[2];

                keys[0] = ymd;
                ChannelGroup channelGroup = nipaRegionalAnalysis.getChannelGroup(nipaContents.channelId);
                if (channelGroup == null) {
                    keys[1] = Config.getConfig(ServiceConfig.CHANNEL_DEFAULT.key(), (String) ServiceConfig.CHANNEL_DEFAULT.defaultValue());
                } else {
                    keys[1] = channelGroup.id;
                }
                indexData.setIndexKeys(keys);

                //파일 데이터 저장용 생성
                Map<String, IndexDataInfo> idMap = ymdIdMap.computeIfAbsent(ymd, k -> new HashMap<>());

                JSONArray keyArray = new JSONArray();
                for (String indexKey : keys) {
                    keyArray.put(indexKey);
                }

                String id = Long.toString(nipaContents.contentsNum);

                IndexDataInfo info = new IndexDataInfo();
                info.indexData = indexData;
                info.keyArray = keyArray;
                idMap.put(id, info);

                NipaData nipaData = new NipaData();
                nipaData.indexData = indexData;
                nipaData.nipaContents = nipaContents;
                nipaData.document = document;
                addDataList.add(nipaData);
            }

            nipaContentsList.clear();




            Map<String, DetailFile> detailFileMap = new HashMap<>();

            for (NipaData nipaData : addDataList) {
                //상세파일저장
                IndexData indexData = nipaData.indexData;

                String[] keys = indexData.getIndexKeys();
                DetailFile detailFile = detailFileMap.get(keys[0]);

                if(detailFile == null){
                    detailFile = new DetailFile();
                    detailFile.filePath = IndexUtil.getDetailFilePath(keys[0]);
                    if(FileUtil.isFile(detailFile.filePath)){
                        detailFile.lineIndex = (int) FileUtil.getLineCount(detailFile.filePath);
                    }else{
                        detailFile.lineIndex = 0;
                    }

                    if(detailFile.lineIndex == 0){
                        detailFile.isFirst = true;
                    }
                    detailFileMap.put(keys[0], detailFile);
                }

                indexData.setDetailFilePath(detailFile.filePath);
                indexData.setDetailLine(detailFile.lineIndex);

                //파일에 쓸 데이터 추가
                JSONObject fileValue = new JSONObject();
                fileValue.put("title", nipaData.nipaContents.title);
                fileValue.put("contents", nipaData.nipaContents.contents);
                fileValue.put("channel_id", nipaData.nipaContents.channelId);
                fileValue.put("channel_name", channelNameMap.get(nipaData.nipaContents.channelId));
                fileValue.put("post_time", nipaData.nipaContents.postTime);
                fileValue.put("post_ymd_hm", new SimpleDateFormat("yyyyMMdd hh:mm").format(new Date(nipaData.nipaContents.postTime)));
                fileValue.put("original_url", nipaData.nipaContents.originalUrl);

                CodeName[] emotionClassifies = indexData.getClassifies();
                CodeName emotionCodeName = null;
                for(CodeName codeName : emotionClassifies){
                    if(codeName.getCode().startsWith(emotionClassify)){
                        emotionCodeName = codeName;
                        break;
                    }
                }

                if(emotionCodeName == null) {
                    fileValue.put("emotion_name", "중립");
                }else{
                    fileValue.put("emotion_name", emotionCodeName.getName());
                }
                fileValue.put("analysis_contents", nipaData.document.getAnalysisContents());


                detailFile.sb.append("\n").append(fileValue.toString());

                //라인 넘버 변경
                detailFile.lineIndex++;
                
                if(detailFile.lineIndex >= Config.getLong(KeywordConfig.DETAIL_FILE_LINE.key(), (long) KeywordConfig.DETAIL_FILE_LINE.defaultValue())){
                    //파일에 내용을 저장하고 새로운 파일로 교체
                    //파일저장
                    if(detailFile.isFirst){
                        FileUtil.fileOutput(detailFile.sb.substring(1),  detailFile.filePath, false);
                    }else{
                        FileUtil.fileOutput(detailFile.sb.toString(),  detailFile.filePath, true);
                    }

                    //새로운 파일로 변경
                    detailFile.filePath = IndexUtil.getDetailFilePath(keys[0], IndexUtil.getFileNumber(new File(detailFile.filePath).getName(),7) + 1);
                    detailFile.isFirst = true;
                    detailFile.lineIndex = 0;
                    detailFile.sb.setLength(0);
                }
            }

            Collection<DetailFile> detailFiles = detailFileMap.values();
            for(DetailFile detailFile : detailFiles){
                if(detailFile.sb.length() > 0 ) {

                    if (detailFile.isFirst) {
                        FileUtil.fileOutput(detailFile.sb.substring(1), detailFile.filePath, false);
                    } else {
                        FileUtil.fileOutput(detailFile.sb.toString(), detailFile.filePath, true);
                    }
                    detailFile.sb.setLength(0);
                }
            }


            Map<String, Integer> lastFileNumberMap = new HashMap<>();
            Set<String> ymdSet = ymdIdMap.keySet();

            for (String keyYmd : ymdSet) {
                Collection<IndexDataInfo> indexDataColl = ymdIdMap.get(keyYmd).values();
                if (indexDataColl.size() == 0) {
                    continue;
                }

                Integer lastNumber = lastFileNumberMap.get(keyYmd);
                if (lastNumber == null) {
                    lastNumber = -1;
                }

                String filePath = IndexUtil.getWriteFilePath(keyYmd, lastNumber);

                File file = new File(filePath);
                String fileName = file.getName();
                lastFileNumberMap.put(keyYmd, IndexUtil.getFileNumber(fileName, 6));

                StringBuilder sb = new StringBuilder();
                for (IndexDataInfo data : indexDataColl) {
                    data.indexData.setIndexFileName(fileName);
                    JSONObject jsonObj = new JSONObject();
                    data.indexData.setJSONObject(jsonObj);
                    jsonObj.put(KeywordJsonIndex.INDEX_KEYS, data.keyArray);
                    sb.append(jsonObj.toString()).append("\n");
                }
                FileUtil.fileOutput(sb.toString(), CharSet.UTF8, filePath, true);
                indexDataColl.clear();
            }


            engineConfig.key = ServiceConfig.CONTENTS_LAST_NUM.key();
            engineConfig.value = Long.toString(lastNum);
            engineConfig.updateTime = System.currentTimeMillis();
            JdbcObjects.insertOrUpdate(engineConfig, false);




            for (NipaData nipaData : addDataList) {
                //메모리 데이터 세팅
                IndexData indexData = nipaData.indexData;
                ContentsGroup contentsGroup = keywordAnalysis.getGroup(indexData.getIndexKeys());

                if (contentsGroup == null) {
                    contentsGroup = keywordAnalysis.newGroup(indexData.getIndexKeys());
                }

                ContentsIndexData contentsIndexData = new ContentsIndexData();
                contentsIndexData.setType(ContentsIndexData.Type.ADD);
                contentsIndexData.setData(indexData);
                //메로리에 정보추가
                contentsGroup.addIndex(contentsIndexData);
            }

            detailFileMap.clear();
            lastFileNumberMap.clear();
            ymdIdMap.clear();
            addDataList.clear();

            return true;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getStackTrace(e));
            return false;
        }
    }

    private static class IndexDataInfo {

        IndexData indexData;
        JSONArray keyArray;
    }

    private static class NipaData{
        NipaRsContents nipaContents;
        IndexData indexData;
        Document document;
    }

    private static class DetailFile {

        String filePath;
        int lineIndex;
        boolean isFirst = false;
        StringBuilder sb = new StringBuilder();


    }

}