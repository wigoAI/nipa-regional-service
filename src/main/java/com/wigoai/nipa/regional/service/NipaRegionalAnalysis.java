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

import com.seomse.cypto.LoginCrypto;
import com.wigoai.nipa.regional.service.channel.ChannelManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.moara.ara.datamining.textmining.TextMining;
import org.moara.ara.datamining.textmining.document.Document;
import org.moara.common.code.LangCode;
import org.moara.common.config.Config;
import org.moara.sync.SynchronizerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Nipa Regional Service Analysis
 * nipa 분석
 * real time 용 파일 데이터 생성
 * @author macle
 */
public class NipaRegionalAnalysis {

    private static final Logger logger = LoggerFactory.getLogger(NipaRegionalAnalysis.class);

    private static class Singleton {
        private static final NipaRegionalAnalysis instance = new NipaRegionalAnalysis();
    }

    /**
     * 인스턴스 얻기
     * @return NipaRegionalCollect
     */
    public static NipaRegionalAnalysis getInstance(){
        return Singleton.instance;
    }

    private final DataSource dataSource;


    private final ClassifyManager classifyCodeManager = new ClassifyManager();
    private final ChannelManager channelManager = new ChannelManager();

    /**
     * 생성자
     */
    private NipaRegionalAnalysis(){


        String dbUrl = Config.getConfig(ServiceConfig.COLLECT_DB_URL.key());

        String encId = Config.getConfig(ServiceConfig.COLLECT_DB_USER.key());
        String encPassword = Config.getConfig(ServiceConfig.COLLECT_DB_PASSWORD.key());

        if(dbUrl == null){
            throw new RuntimeException(initError(ServiceConfig.COLLECT_DB_URL.key()));
        }
        if(encId == null){
            throw new RuntimeException(initError(ServiceConfig.COLLECT_DB_USER.key()));
        }
        if(encPassword == null){
            throw new RuntimeException(initError(ServiceConfig.COLLECT_DB_PASSWORD.key()));
        }

        String [] loginInfos = LoginCrypto.decryption(encId, encPassword);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(loginInfos[0]);
        config.setPassword(loginInfos[1]);
        config.setAutoCommit(true);
        config.setMaximumPoolSize(3);
        dataSource =  new HikariDataSource(config);
        
        classifyCodeManager.sync();
        SynchronizerManager.getInstance().add(classifyCodeManager);

    }

    /**
     * 생성자 호출 이후에 필요한 이벤트
     * NipaRegionalServiceInitializer 에서 같이 실행 됨
     */
    void init(){

        //채널 리뉴얼
        channelManager.sync();
        SynchronizerManager.getInstance().add(channelManager);


    }

    /**
     * init error
     * @param key String config key
     * @return String error message
     */
    String initError(String key){
        String errorMessage = "crawling database set error " + key + " check";
        logger.error(errorMessage);
        return errorMessage;
    }


    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * document 생성
     * @param nipaContents NipaRsContents
     * @return Document
     */
    public static Document makeDocument(NipaRsContents nipaContents){
        Document document = new Document();
        document.setId(Long.toString(nipaContents.contentsNum));
        document.setTitle(nipaContents.title);
        document.setContents(nipaContents.contents);
        document.setLangCode(LangCode.KO);
        document.setRegDataDateTime( nipaContents.postTime);
        TextMining.mining(document);

        return document;
    }

    /**
     * 감성분석에서 사용 하는 코드 목록 (im memory)
     * @return String []
     */
    public String [] getEmotionCodes(){
        return classifyCodeManager.emotionCodes;
    }

    /**
     * 분야별 분석에서 사용 하는 코드 목록 (im memory)
     * @return String []
     */
    public String [] getFieldCodes(){
        return classifyCodeManager.fieldCodes;
    }


    /**
     * 이름으로 코드 얻기
     * @param name String
     * @return String
     */
    public String getFieldCode(String name){return classifyCodeManager.getFieldCode(name);}

    /**
     * 채널 매니저 얻기
     * 채널과 그룹정보 관리
     * @return 채널 매니저
     */
    public ChannelManager getChannelManager() {
        return channelManager;
    }
}
