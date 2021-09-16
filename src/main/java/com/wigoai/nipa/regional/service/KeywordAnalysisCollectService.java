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
import com.wigoai.nipa.regional.service.channel.Channel;
import com.wigoai.nipa.regional.service.channel.ChannelGroup;
import com.wigoai.nipa.regional.service.channel.ChannelManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.moara.ara.datamining.data.CodeName;
import org.moara.ara.datamining.statistics.count.WordCount;
import org.moara.ara.datamining.textmining.api.document.DocumentStandardKey;
import org.moara.ara.datamining.textmining.document.Document;
import org.moara.common.config.Config;
import org.moara.common.data.database.jdbc.JdbcObjects;
import org.moara.common.service.Service;
import org.moara.common.util.ExceptionUtil;
import org.moara.engine.MoaraEngine;
import org.moara.keyword.KeywordAnalysis;
import org.moara.keyword.KeywordConfig;
import org.moara.keyword.ServiceKeywordAnalysis;
import org.moara.keyword.index.*;
import org.moara.keyword.search.ContentsGroup;
import org.moara.keyword.search.ContentsIndexData;
import org.moara.yido.ner.NamedEntityRecognizer;
import org.moara.yido.ner.NamedEntityRecognizerManager;
import org.moara.yido.ner.entity.NamedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 키워드 분석용 수집 서비스
 *
 * @author macle
 */
public class KeywordAnalysisCollectService extends Service implements ReIndexWait{

    private static final Logger logger = LoggerFactory.getLogger(KeywordAnalysisCollectService.class);

    private final EngineConfig engineConfig;

    ReIndexDetail reIndexDetail;

    private final NamedEntityRecognizer reportRecognizer;



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
        NamedEntityRecognizerManager namedEntityRecognizerManager = NamedEntityRecognizerManager.getInstance();
        reportRecognizer = namedEntityRecognizerManager.getNamedEntityRecognizer("PS_REPORTER");

        String emotionClassify = Config.getConfig(ServiceConfig.EMOTION_CLASSIFY.key());

        final ChannelGroup mediaGroup = NipaRegionalAnalysis.getInstance().getChannelManager().getGroupFromId("media");

        reIndexDetail = (detailObj, document, indexData) -> {

            String [] keys = indexData.getIndexKeys();

            Set<String> tagSet = new HashSet<>();
            tagSet.add(detailObj.getString("channel_name").replace(" ",""));

            if (mediaGroup.hasChannel(keys[1])) {
                //해시 태그 정보 추가
                //            //index data에 데이터 추가
                NamedEntity[] namedEntityArray = reportRecognizer.recognize(document.getContents());

                if(namedEntityArray.length > 0){

                    JSONArray reporterArray = new JSONArray();
                    for(NamedEntity namedEntity : namedEntityArray){
                        tagSet.add(namedEntity.getText());
                        reporterArray.put(namedEntity.getText());
                    }
                    indexData.addData("PS_REPORTER", reporterArray);
                }
            }


            indexData.setTagSet(tagSet);

            CodeName[] emotionClassifies = indexData.getClassifies();
            CodeName emotionCodeName = null;
            for(CodeName codeName : emotionClassifies){
                if(codeName.getCode().startsWith(emotionClassify)){
                    emotionCodeName = codeName;
                    break;
                }
            }

            if(emotionCodeName == null) {
                detailObj.put("emotion_name", "중립");
            }else{
                detailObj.put("emotion_name", emotionCodeName.getName());
            }
        };

        engineConfig = new EngineConfig();
        engineConfig.engineCode = moaraEngine.getCode();

        String lastNumValue = MoaraEngine.getInstance().getConfig(ServiceConfig.CONTENTS_LAST_NUM.key());

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

        for (;;) {
            if (!collectToMakeIndex()) {
                ServiceKeywordAnalysis.getInstance().getKeywordAnalysis().setIndex(null, null);
                break;
            }
        }
    }

    private long lastNum;

    private final Object collectLock = new Object();

    private boolean isCollect = false;

    /**
     * 데이터 수집 하여 index 정보 생성
     *
     * @return boolean is data
     */
    boolean collectToMakeIndex() {

        //개발용 임시소스 (분류모델 생성 전 더미데이터)

        ReIndex reIndex = ReIndex.getInstance();
        try{
            while (reIndex.isRun()) {
                logger.debug("reindex running sleep");
                //noinspection BusyWait
                Thread.sleep(5000);
            }
        }catch (Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
            return false;
        }

        String emotionClassify = Config.getConfig(ServiceConfig.EMOTION_CLASSIFY.key());

        NipaRegionalAnalysis nipaRegionalAnalysis = NipaRegionalAnalysis.getInstance();

        ChannelManager channelManager = nipaRegionalAnalysis.getChannelManager();
        final ChannelGroup mediaGroup = channelManager.getGroupFromId("media");
        //이전 채널 그룹은 사용하지 않게 업데이트 해야함
        //500개씩 가져오기
        try (Connection conn = nipaRegionalAnalysis.getConnection()) {

            List<NipaRsContents> nipaContentsList = JdbcObjects.getObjList(conn, NipaRsContents.class, "SEQ_NO > " + lastNum + " ORDER BY SEQ_NO ASC LIMIT 0, 500");
            if (nipaContentsList.size() == 0) {
                logger.debug("data size 0 sleep");
                return false;
            }

            reIndex = ReIndex.getInstance();
            try{
                if (reIndex.isRun()) {
                    logger.debug("reindex running wait");
                    return false;
                }
            }catch (Exception e){
                logger.error(ExceptionUtil.getStackTrace(e));
                return false;
            }

            //리인덱스 실행중인지체크 로직
            synchronized (collectLock) {
                isCollect = true;
            }

            logger.debug("data size: " + nipaContentsList.size());
            lastNum = nipaContentsList.get(nipaContentsList.size() - 1).seqNum;
            //인 메모리 데이터 생성
            KeywordAnalysis keywordAnalysis = ServiceKeywordAnalysis.getInstance().getKeywordAnalysis();


            long time = System.currentTimeMillis();

            int maxLength = Config.getInteger(ServiceConfig.CONTENTS_MAX_LENGTH.key(), (int) ServiceConfig.CONTENTS_MAX_LENGTH.defaultValue());


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
                IndexWord[] indexWords = indexData.getIndexWords();
                if (indexWords == null || indexWords.length == 0) {
                    continue;
                }

                String ymd = new SimpleDateFormat("yyyyMMdd").format(new Date(nipaContents.postTime));
                String[] keys = new String[2];

                keys[0] = ymd;
                keys[1] = nipaContents.channelId;
                indexData.setIndexKeys(keys);

                Channel channel = channelManager.getChannel(nipaContents.channelId);

                Set<String> tagSet =  new HashSet<>();
                tagSet.add(channel.getName().replace(" ",""));

                if (mediaGroup.hasChannel(nipaContents.channelId)) {
                    //해시 태그 정보 추가
                    //            //index data에 데이터 추가
                    NamedEntity[] namedEntityArray = reportRecognizer.recognize(document.getContents());

                    if(namedEntityArray.length > 0){

                        JSONArray reporterArray = new JSONArray();
                        for(NamedEntity namedEntity : namedEntityArray){
                            tagSet.add(namedEntity.getText());
                            reporterArray.put(namedEntity.getText());
                        }
                        indexData.addData("PS_REPORTER", reporterArray);
                    }
                }

                indexData.setTagSet(tagSet);
                NipaData nipaData = new NipaData();
                nipaData.indexData = indexData;
                nipaData.nipaContents = nipaContents;
                nipaData.document = document;
                nipaData.channel = channel;
                addDataList.add(nipaData);
            }

            nipaContentsList.clear();

            Map<String, IndexFile> indexFileMap = new HashMap<>();

            for (NipaData nipaData : addDataList) {
                //상세파일저장
                IndexData indexData = nipaData.indexData;

                String[] keys = indexData.getIndexKeys();
                IndexFile indexFile = indexFileMap.get(keys[0]);

                if(indexFile == null){
                    indexFile = new IndexFile();
                    indexFile.filePath = IndexUtil.getWriteFilePath(keys[0], -1);
                    if(FileUtil.isFile(indexFile.filePath)){
                        indexFile.lineIndex = (int) FileUtil.getLineCount(indexFile.filePath);
                    }else{
                        indexFile.lineIndex = 0;
                    }

                    if(indexFile.lineIndex == 0){
                        indexFile.isFirst = true;
                    }
                    indexFileMap.put(keys[0], indexFile);
                }

                indexData.setIndexFileName(indexFile.filePath);
                indexData.setIndexFileLine(indexFile.lineIndex);

                JSONObject jsonObj = new JSONObject();
                indexData.setJSONObject(jsonObj);

                JSONObject detailObj = jsonObj.getJSONObject(IndexData.Keys.DETAIL.key());
                detailObj.put(IndexData.Keys.ANALYSIS_CONTENTS.key(), nipaData.document.getAnalysisContents());

                detailObj.put(DocumentStandardKey.TITLE.key(), nipaData.nipaContents.title);
                detailObj.put(DocumentStandardKey.CONTENTS.key(), nipaData.nipaContents.contents);
                detailObj.put(DocumentStandardKey.LANG_CODE.key(), nipaData.document.getLangCode());
                detailObj.put(DocumentStandardKey.DOC_TYPE.key(), nipaData.document.getDocType());

                detailObj.put("channel_id", nipaData.nipaContents.channelId);
                detailObj.put("channel_name",  nipaData.channel.getName());
                detailObj.put("post_time", nipaData.nipaContents.postTime);
                detailObj.put("post_ymd_hm", new SimpleDateFormat("yyyyMMdd HH:mm").format(new Date(nipaData.nipaContents.postTime)));
                detailObj.put("original_url", nipaData.nipaContents.originalUrl);

                CodeName[] emotionClassifies = indexData.getClassifies();
                CodeName emotionCodeName = null;
                for(CodeName codeName : emotionClassifies){
                    if(codeName.getCode().startsWith(emotionClassify)){
                        emotionCodeName = codeName;
                        break;
                    }
                }

                if(emotionCodeName == null) {
                    detailObj.put("emotion_name", "중립");
                }else{
                    detailObj.put("emotion_name", emotionCodeName.getName());
                }


                jsonObj.put(IndexData.Keys.DETAIL.key(), detailObj);

                indexFile.sb.append('\n').append(jsonObj);

                //라인 넘버 변경
                indexFile.lineIndex++;
                
                if(indexFile.lineIndex >= Config.getInteger(KeywordConfig.INDEX_FILE_SPLIT_COUNT.key(), (int) KeywordConfig.INDEX_FILE_SPLIT_COUNT.defaultValue())){
                    //파일에 내용을 저장하고 새로운 파일로 교체
                    //파일저장
                    if(indexFile.isFirst){

                        FileUtil.fileOutput(indexFile.sb.substring(1),  indexFile.filePath, false);
                    }else{
                        FileUtil.fileOutput(indexFile.sb.toString(),  indexFile.filePath, true);
                    }

                    //새로운 파일로 변경
                    indexFile.filePath = IndexUtil.getWriteFilePath(keys[0], IndexUtil.getFileNumber(new File(indexFile.filePath).getName(),6) + 1);
                    indexFile.isFirst = true;
                    indexFile.lineIndex = 0;
                    indexFile.sb.setLength(0);
                }
            }

            Collection<IndexFile> detailFiles = indexFileMap.values();
            for(IndexFile indexFile : detailFiles){
                if(indexFile.sb.length() > 0 ) {

                    if (indexFile.isFirst) {
                        FileUtil.fileOutput(indexFile.sb.substring(1), indexFile.filePath, false);
                    } else {
                        FileUtil.fileOutput(indexFile.sb.toString(), indexFile.filePath, true);
                    }
                    indexFile.sb.setLength(0);
                }
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

            indexFileMap.clear();
            addDataList.clear();

            synchronized (collectLock) {
                isCollect = false;
            }
            return true;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getStackTrace(e));
            isCollect = false;
            return false;
        }
    }

    @Override
    public boolean isWait() {
        synchronized (collectLock) {
           return isCollect;
        }
    }

    private static class NipaData{
        NipaRsContents nipaContents;
        IndexData indexData;
        Document document;
        Channel channel;

    }

    private static class IndexFile {
        String filePath;
        int lineIndex;
        boolean isFirst = false;
        StringBuilder sb = new StringBuilder();
    }

}