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

import org.moara.common.config.Config;
import org.moara.common.service.Service;
import org.moara.engine.node.MasterNodeInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 마스터 노드에서 실행 시켜야 할 내용 정의
 *
 * @author macle
 */
@SuppressWarnings("unused")
public class NipaRegionalMasterNode implements MasterNodeInitializer {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchCollectService.class);
    @Override
    public void init() {
        if(Config.getBoolean(ServiceConfig.SERVICE_FLAG.key(),(Boolean)ServiceConfig.SERVICE_FLAG.defaultValue())){
            ElasticSearchCollectService elasticSearchCollectService = new ElasticSearchCollectService();
            elasticSearchCollectService.setSleepTime(Config.getLong(ServiceConfig.COLLECT_SLEEP_SECOND.key(), ((long)ServiceConfig.COLLECT_SLEEP_SECOND.defaultValue() )* 1000L));
            elasticSearchCollectService.setState(Service.State.START);
            elasticSearchCollectService.start();


            logger.info(this.getClass().getName() + " start complete");

        }
    }
}
