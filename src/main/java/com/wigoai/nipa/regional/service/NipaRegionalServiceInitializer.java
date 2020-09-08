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

import org.moara.MoaraInitializer;
import org.moara.common.annotation.Priority;
import org.moara.common.callback.Callback;
import org.moara.common.config.Config;
import org.moara.common.service.Service;
import org.moara.keyword.ServiceKeywordAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Moara Engine 이 구동될 떄 같이 실행 됨
 * @author macle
 */
@SuppressWarnings("unused")
@Priority(seq =Integer.MAX_VALUE)
public class NipaRegionalServiceInitializer implements MoaraInitializer {

    private static final Logger logger = LoggerFactory.getLogger(NipaRegionalServiceInitializer.class);

    @Override
    public void init() {

        if(Config.getBoolean(ServiceConfig.SERVICE_FLAG.key(),(Boolean)ServiceConfig.SERVICE_FLAG.defaultValue())){
            //싱글톤 생성
            //noinspection ResultOfMethodCallIgnored
            NipaRegionalAnalysis.getInstance();
            ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();

            Callback initCallback = new Callback() {

                public void callback(){
                    KeywordAnalysisCollectService keywordAnalysisCollectService = new KeywordAnalysisCollectService();
                    keywordAnalysisCollectService.setSleepTime(Config.getLong(ServiceConfig.COLLECT_SLEEP_SECOND.key(), ((long)ServiceConfig.COLLECT_SLEEP_SECOND.defaultValue()))* 1000L);
                    keywordAnalysisCollectService.setState(Service.State.START);
                    keywordAnalysisCollectService.start();
                    logger.info(this.getClass().getName() + " start complete");
                }
            };

            serviceKeywordAnalysis.addInitCallback(initCallback);
        }
    }

}
