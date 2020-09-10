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

import org.moara.common.annotation.Priority;
import org.moara.common.config.Config;
import org.moara.common.data.database.jdbc.JDBCUtil;
import org.moara.common.string.StringArray;
import org.moara.sync.Synchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * nipa 서비스에서 사용하는 분류 관리
 * @author macle
 */
@Priority(seq = Integer.MAX_VALUE )
public class ClassifyManager implements Synchronizer {

    private static final Logger logger = LoggerFactory.getLogger(ClassifyManager.class);

    String [] emotionCodes = StringArray.EMPTY_STRING_ARRAY;

    String [] fieldCodes = StringArray.EMPTY_STRING_ARRAY;

//    Map<String, String> fieldNameMap = new HashMap<>();

    Map<String, String> fieldNameCodeMap = new HashMap<>();


    @Override
    public void sync() {

        List<String> emotionCodeList = JDBCUtil.getList("SELECT CD_CATEGORY FROM TB_MOARA_CATEGORY WHERE CD_CATEGORY LIKE '" + Config.getConfig(ServiceConfig.EMOTION_CLASSIFY.key()) +"%' AND FG_PARENTS ='N' AND FG_DEL='N'");

        if(emotionCodeList.size() == 0){
            throw new RuntimeException("emotion code set error: " + Config.getConfig(ServiceConfig.EMOTION_CLASSIFY.key()));
        }

        String [] codes = emotionCodeList.toArray(new String[0]);

        if(!Arrays.equals(emotionCodes, codes)){
            emotionCodes = codes;
            logger.debug("emotion classify update: " + emotionCodes.length);

        }
        emotionCodeList.clear();

        List<Map<String,String>> fieldList = JDBCUtil.getResultList("SELECT CD_CATEGORY, NM_CATEGORY FROM TB_MOARA_CATEGORY WHERE CD_CATEGORY LIKE '" + Config.getConfig(ServiceConfig.FIELD_CLASSIFY.key()) +"%' AND FG_PARENTS ='N' AND FG_DEL='N'");
        if(fieldList.size() == 0){
            throw new RuntimeException("field code set error: " + Config.getConfig(ServiceConfig.FIELD_CLASSIFY.key()));
        }

        codes = new String [fieldList.size()];

        for (int i = 0; i <codes.length ; i++) {
            Map<String,String> data = fieldList.get(i);
            codes[i] = data.get("CD_CATEGORY");
//            fieldNameMap.put(data.get("CD_CATEGORY"), data.get("NM_CATEGORY"));
            fieldNameCodeMap.put(data.get("NM_CATEGORY"), data.get("CD_CATEGORY"));
        }

        if(!Arrays.equals(fieldCodes, codes)){
            fieldCodes = codes;
            logger.debug("field classify update: " + fieldCodes.length);
        }

        fieldList.clear();
    }


    public String getFieldCode(String name){
        return fieldNameCodeMap.get(name);
    }


}
