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
import org.moara.common.data.database.jdbc.JdbcObjects;
import org.moara.common.data.database.jdbc.PrepareStatementData;
import org.moara.meta.MetaDataUtil;
import org.moara.sync.Synchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 채널 그룹 관리
 * @author macle
 */
@Priority(seq = Integer.MAX_VALUE)
public class ChannelGroupManager implements Synchronizer {

    private static final Logger logger = LoggerFactory.getLogger(ChannelGroupManager.class);

    private final Map<String, ChannelGroup> groupMap = new HashMap<>();

    private ChannelGroup [] groups = new ChannelGroup[0];

    private final Map<String, ChannelGroup> channelGroupMap = new HashMap<>();

    private ChannelGroup defaultGroup= null;

    private long groupTime = 0L;
    private long mapTime = 0L;



    @Override
    public void sync() {

        List<ChannelGroupUpdate> channelGroupUpdateList;
        if(groupTime == 0L) {
            channelGroupUpdateList = JdbcObjects.getObjList(ChannelGroupUpdate.class);
        }else{
            Map<Integer, PrepareStatementData> prepareStatementDataMap = MetaDataUtil.newTimeMap(groupTime);
            channelGroupUpdateList = JdbcObjects.getObjList(ChannelGroupUpdate.class, "UPT_DT > ?", prepareStatementDataMap);
        }

        if(channelGroupUpdateList.size() > 0){
            logger.debug("ChannelGroupUpdate: " + channelGroupUpdateList.size());

            boolean isChange = false;

            groupTime = channelGroupUpdateList.get(channelGroupUpdateList.size()-1).updateTime;
            for(ChannelGroupUpdate channelGroupUpdate : channelGroupUpdateList){
                if(channelGroupUpdate.isDel){

                    ChannelGroup group = groupMap.remove(channelGroupUpdate.id);
                    if(group != null){
                        isChange = true;
                    }

                    String []  keys = channelGroupMap.keySet().toArray(new String [0]);
                    for(String key: keys){
                        ChannelGroup channelGroup = channelGroupMap.get(key);
                        if(channelGroup.id.equals(channelGroupUpdate.id)){
                            channelGroupMap.remove(channelGroupUpdate.id);
                        }
                    }
                    continue;
                }
                ChannelGroup channelGroup = groupMap.get(channelGroupUpdate.id);
                if(channelGroup == null){
                    isChange = true;
                    channelGroup  = new ChannelGroup();
                    channelGroup.id = channelGroupUpdate.id;
                    groupMap.put(channelGroupUpdate.id, channelGroup);
                }
                channelGroup.name = channelGroupUpdate.name;
            }
            channelGroupUpdateList.clear();

            if(isChange){
                groups = groupMap.values().toArray(new ChannelGroup[0]);
            }

        }

        List<ChannelGroupMapUpdate> channelGroupMapUpdateList;

        if(mapTime == 0L){
            channelGroupMapUpdateList =  JdbcObjects.getObjList(ChannelGroupMapUpdate.class);
        }else{
            Map<Integer, PrepareStatementData> prepareStatementDataMap = MetaDataUtil.newTimeMap(mapTime);
            channelGroupMapUpdateList = JdbcObjects.getObjList(ChannelGroupMapUpdate.class, "UPT_DT > ?", prepareStatementDataMap);
        }


        if(channelGroupMapUpdateList.size() > 0){
            logger.debug("ChannelGroupMapUpdate: " + channelGroupMapUpdateList.size());

            mapTime = channelGroupMapUpdateList.get(channelGroupMapUpdateList.size()-1).updateTime;

            for(ChannelGroupMapUpdate channelGroupMapUpdate : channelGroupMapUpdateList){
                if(channelGroupMapUpdate.isDel){
                    channelGroupMap.remove(channelGroupMapUpdate.channelId);
                    continue;
                }

                ChannelGroup channelGroup = groupMap.get(channelGroupMapUpdate.groupId);
                if(channelGroup == null){
                    logger.error("group null: " + channelGroupMapUpdate.groupId);
                    continue;
                }

                channelGroupMap.put(channelGroupMapUpdate.channelId, channelGroup);
            }
            channelGroupMapUpdateList.clear();
        }


        String defaultId = Config.getConfig(ServiceConfig.DEFAULT_CHANNEL_GROUP.key(), (String)ServiceConfig.DEFAULT_CHANNEL_GROUP.defaultValue());
        for(ChannelGroup channelGroup  : groups){
            if(channelGroup.id.equals(defaultId)){
                defaultGroup = channelGroup;
                break;
            }
        }


        if(defaultGroup == null){
            throw new RuntimeException("default channel group null");
        }


    }

    /**
     * ChannelGroup get
     * @param channelId String
     * @return ChannelGroup
     */
    public ChannelGroup getChannelGroup(String channelId){
        return channelGroupMap.get(channelId);
    }

    /**
     *
     * @return ChannelGroup[]
     */
    public ChannelGroup[] getGroups() {
        return groups;
    }

    /**
     * @return ChannelGroup
     */
    public ChannelGroup getDefaultGroup() {
        return defaultGroup;
    }
}
