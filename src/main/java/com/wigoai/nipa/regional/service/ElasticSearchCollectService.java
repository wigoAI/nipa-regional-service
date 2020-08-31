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


import org.moara.common.service.Service;
import org.moara.common.util.ExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 엘라스틱 서치 데이터 관리 서비스
 * @author macle
 */
public class ElasticSearchCollectService extends Service {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchCollectService.class);

    @Override
    public void work() {

        DataSource dataSource =NipaRegionalCollect.getInstance().getDataSource();

        try(Connection conn = dataSource.getConnection()){

            try{

            }catch(Exception e){

            }


        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
        }


    }
}
