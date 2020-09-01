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


import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.moara.ara.datamining.textmining.document.Document;
import org.moara.common.config.Config;
import org.moara.common.data.database.jdbc.JdbcObjects;
import org.moara.common.service.Service;
import org.moara.common.util.ExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 엘라스틱 서치 데이터 관리 서비스
 * @author macle
 */
public class ElasticSearchCollectService extends Service {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchCollectService.class);

    /**
     * 생성자
     */
    public ElasticSearchCollectService(){
        super();
    }

    @Override
    public void work() {


        long lastNum = Config.getLong(ServiceConfig.ELASTICSEARCH_CONTENTS_LAST_NUM.key(), (long) ServiceConfig.ELASTICSEARCH_CONTENTS_LAST_NUM.defaultValue());

        DataSource dataSource =NipaRegionalCollect.getInstance().getDataSource();

        try(
                Connection conn = dataSource.getConnection();
                RestHighLevelClient client = new RestHighLevelClient(
                    RestClient.builder(
                            new HttpHost(Config.getConfig(ServiceConfig.ELASTICSEARCH_HOST_ADDRESS.key(), (String)ServiceConfig.ELASTICSEARCH_HOST_ADDRESS.defaultValue() )
                                    ,Config.getInteger(ServiceConfig.ELASTICSEARCH_PORT.key(), (int)ServiceConfig.ELASTICSEARCH_PORT.defaultValue())
                                    ,"http")))
        ){

            for(;;){

                List<NipaRsContents> nipaContentsList = JdbcObjects.getObjList(conn, NipaRsContents.class, "CONTENTS_NB > " + lastNum + " ORDER BY CONTENTS_NB ASC LIMIT 0, 200");

                if(nipaContentsList.size() == 0){
                    break;
                }

                lastNum = nipaContentsList.get(nipaContentsList.size() - 1).contentsNum;

                BulkRequest request = new BulkRequest();

                for(NipaRsContents nipaRsContents : nipaContentsList){



                    Map<String, Object> jsonMap = new HashMap<>();
                    jsonMap.put("title", nipaRsContents.title);
                    jsonMap.put("contents", nipaRsContents.contents);
                    jsonMap.put("media", nipaRsContents.channelId);


                    String ymd = new SimpleDateFormat("yyyyMMdd").format(new Date(nipaRsContents.postTime));


//                    jsonMap.put("reg_ymd", new SimpleDateFormat("yyyy-MM-dd").format(new Date(obj.getLong("time"))));
//                    jsonMap.put("media","001");
//

                    Document document = NipaRegionalCollect.makeDocument(nipaRsContents);
                    //분류결과 얻기
                    
                    

                    IndexRequest indexRequest = new IndexRequest("media")
                            .id(Long.toString(nipaRsContents.contentsNum)).source(jsonMap);
                    request.add(indexRequest);
                }

                client.bulk(request, RequestOptions.DEFAULT);
                nipaContentsList.clear();

                CommonConfig commonConfig = new CommonConfig();
                commonConfig.key = ServiceConfig.ELASTICSEARCH_CONTENTS_LAST_NUM.key();
                commonConfig.value = Long.toString(lastNum);
                commonConfig.updateTime = System.currentTimeMillis();
                JdbcObjects.update(commonConfig, false);

            }




        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
        }


    }
}
