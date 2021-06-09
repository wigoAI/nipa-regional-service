/*
 * Copyright (C) 2021 Wigo Inc.
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

package com.wigoai.nipa.regional.service.channel;

import com.seomse.commons.exception.ReflectiveOperationRuntimeException;
import com.wigoai.nipa.regional.service.NipaRegionalAnalysis;
import org.moara.common.annotation.Priority;
import org.moara.common.data.database.exception.SQLRuntimeException;
import org.moara.common.data.database.jdbc.JdbcObjects;
import org.moara.common.data.database.jdbc.PrepareStatementData;
import org.moara.meta.MetaDataUtil;
import org.moara.sync.Synchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 채널 관리자
 * @author macle
 */
@Priority(seq = Integer.MAX_VALUE - 5)
public class ChannelManager implements Synchronizer {

    private static final Logger logger = LoggerFactory.getLogger(ChannelManager.class);

    private final Map<String, Channel> channelMap = new HashMap<>();
    private final Map<String, ChannelGroup> groupMap = new HashMap<>();
    //그룹명 변경은 불가능함
    private final Map<String, ChannelGroup> groupNameMap = new HashMap<>();

    private long groupTime = 0L;
    private long channelTime = 0L;
    private long mapTime = 0L;

    @Override
    public void sync() {

        //채널그룹
        List<UpdateChannelGroup> groupList = getObjList(UpdateChannelGroup.class, groupTime);
        if(groupList.size() > 0){

            for(UpdateChannelGroup updateChannelGroup : groupList){
                if(updateChannelGroup.isDel){
                    groupMap.remove(updateChannelGroup.id);
                    groupNameMap.remove(updateChannelGroup.name);
                    continue;
                }

                if(groupMap.containsKey(updateChannelGroup.id)){
                    ChannelGroup channelGroup = groupMap.get(updateChannelGroup.id);
                    
                    //이름이 바뀌었으면
                    if(!channelGroup.name.equals(updateChannelGroup.name)){
                        groupNameMap.remove(channelGroup.name);
                        channelGroup.name = updateChannelGroup.name;
                        groupNameMap.put(channelGroup.name, channelGroup);
                    }
                    continue;
                }

                ChannelGroup channelGroup = new ChannelGroup();
                channelGroup.id = updateChannelGroup.id;
                channelGroup.name = updateChannelGroup.name;

                groupMap.put(channelGroup.id, channelGroup);
                groupNameMap.put(channelGroup.name, channelGroup);

            }

            groupTime = groupList.get(groupList.size()-1).time;
            groupList.clear();
        }

        ChannelGroup [] groups = groupMap.values().toArray(new ChannelGroup[0]);

        //채널
        List<UpdateChannel> channelList = getObjList(UpdateChannel.class, channelTime);

        if(channelList.size() > 0){

            for(UpdateChannel updateChannel : channelList){
                if(updateChannel.isDel){
                    //삭제 이면
                    if(!channelMap.containsKey(updateChannel.id)){
                        //등록된 채널이 아니면
                        continue;
                    }

                    channelMap.remove(updateChannel.id);

                    for(ChannelGroup group : groups){
                        group.removeChannel(updateChannel.id);
                    }
                    continue;
                }

                Channel channel = new Channel();
                channel.id = updateChannel.id;;
                channel.name = updateChannel.name;

                channelMap.put(channel.id, channel);
            }

            channelTime = channelList.get(channelList.size()-1).time;
            channelList.clear();
        }

        //매핑
        List<UpdateChannelMap> mapList = getObjList(UpdateChannelMap.class, mapTime);
        if(mapList.size() > 0){
            for(UpdateChannelMap map : mapList){

                ChannelGroup channelGroup = groupMap.get(map.groupId);

                if(channelGroup == null){
                    continue;
                }

                if(map.isDel){
                    channelGroup.removeChannel(map.channelId);
                    continue;
                }

                Channel channel = channelMap.get(map.channelId);
                if(channel == null){
                    continue;
                }

                channelGroup.addChannel(channel);
            }

            mapTime = mapList.get(mapList.size()-1).time;
            mapList.clear();
        }

        for(ChannelGroup group : groups){
            group.setChannels();
        }

        logger.debug("channel update complete");
    }


    public ChannelGroup getGroupFromId(String id){
        return groupMap.get(id);
    }

    public ChannelGroup getGroupFromName(String name){
        return groupNameMap.get(name);
    }

    private <T> List<T> getObjList(Class<T> objClass, long time){
        try(Connection conn = NipaRegionalAnalysis.getInstance().getConnection()){
            if(time == 0L){
                //전체결과 조회
                return JdbcObjects.getObjList(conn,  objClass, null, "DEL_FG ='N'", "UPT_DT ASC", -1, null);
            }else{
                Map<Integer, PrepareStatementData> prepareStatementDataMap = MetaDataUtil.newTimeMap(time);
                return JdbcObjects.getObjList(conn,  objClass, null, "UPT_DT > ?", null, -1, prepareStatementDataMap);
            }
        }catch(SQLException e){
            throw new SQLRuntimeException(e);
        } catch (ReflectiveOperationException e) {
            throw new ReflectiveOperationRuntimeException(e);
        }
    }

}
