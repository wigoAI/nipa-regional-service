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

/**
 * nipa rs 에서 사용하는 설정 정보
 * @author macle
 */
public enum ServiceConfig {
    SERVICE_FLAG("nipa.rs.flag", false)
    , COLLECT_DB_URL("nipa.rs.collect.database.url",null)
    , COLLECT_DB_USER("nipa.rs.collect.database.user",null)
    , COLLECT_DB_PASSWORD("nipa.rs.collect.database.password",null)
    , COLLECT_SLEEP_SECOND("nipa.rs.collect.sleep.second",300L)
    , CONTENTS_LAST_NUM("nipa.rs.contents.last.num", null)
    , CONTENTS_MAX_LENGTH("nipa.rs.contents.max.length",7000)

//    , ELASTICSEARCH_CONTENTS_LAST_NUM("nipa.rs.elasticsearch.contents.last.num", 0L)
//    , ELASTICSEARCH_CONNECT_INFOS ("nipa.rs.elasticsearch.connect.infos", null)

    //
    //분야 분류
    , FIELD_CLASSIFY("nipa.rs.field.classify", null)
    
    //감성 분류
    , EMOTION_CLASSIFY("nipa.rs.emotion.classify", null)

    //객체명 사전
    , NER_CATEGORY_CODE("nipa.rs.ner.category.code", "S601")



    //분석 최대시간
    , ANALYSIS_MAX_TIME("nipa.rs.analysis.max.time",600000L)

    , POSITIVE_CODE("nipa.rs.positive.code" , "U087001")

    , NEGATIVE_CODE("nipa.rs.negative.code" , "U087002")


//    , CHARACTER_CHANNEL_GROUPS("nipa.rs.character.channel.groups" , "84,85,86")
    
    //테스트
    , CHARACTER_CHANNEL_GROUPS("nipa.rs.character.channel.groups" , "media,community")
    ;

    private final String key;
    private final Object defaultValue;


    /**
     * 생성자
     * @param key String
     * @param defaultValue Object, null enable
     */
    ServiceConfig(String key, Object defaultValue){
        this.key = key;
        this.defaultValue = defaultValue;
    }

    /**
     * @return String
     */
    public String key(){return key;}

    /**
     * @return Object
     */
    public Object defaultValue(){return defaultValue;}
}
