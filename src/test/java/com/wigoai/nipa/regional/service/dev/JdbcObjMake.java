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

package com.wigoai.nipa.regional.service.dev;

import com.wigoai.nipa.regional.service.NipaRegionalAnalysis;
import org.moara.common.data.database.jdbc.JdbcObjects;

/**
 * db 연동 개체 생성용 유틸
 * @author macle
 */
public class JdbcObjMake {


    public static void main(String[] args) throws Exception {

        String tableName = "T_CRAWLING_CLIENT_CHANNEL";
        System.out.println(JdbcObjects.makeObjectValue(NipaRegionalAnalysis.getInstance().getConnection(), tableName));

    }
}
