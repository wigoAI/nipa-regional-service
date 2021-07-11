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

import org.moara.common.data.database.Table;
import org.moara.common.data.database.annotation.Column;
import org.moara.common.data.database.annotation.PrimaryKey;
import org.moara.keyword.tf.contents.ChannelGroupHas;

import java.util.HashMap;
import java.util.Map;

/**
 * 데이터 채널 그룹
 * @author macle
 */
@Table(name="T_CRAWLING_CLIENT_CHANNEL_GP")
public class ChannelGroup implements ChannelGroupHas {

    @PrimaryKey(seq = 1)
    @Column(name = "CHANNEL_GP_ID")
    String id;

    @Column(name = "CHANNEL_GP_NM")
    String name;

    private final Map<String, Channel> channelMap =new HashMap<>();
    private Channel [] channels = new Channel[0];

    private boolean isChange = false;

    void addChannel(Channel channel){
        if(channelMap.containsKey(channel.id)){
            return;
        }
        isChange = true;
        channelMap.put(channel.id, channel);
    }

    void removeChannel(String channelId) {
        if(!channelMap.containsKey(channelId)){
            return;
        }
        isChange = true;
        channelMap.remove(channelId);
    }

    void setChannels(){
        if(!isChange){
            return;
        }
        channels = channelMap.values().toArray(new Channel[0]);
        isChange = false;
    }


    public boolean hasChannel(String channelId){
        return channelMap.containsKey(channelId);
    }

    public Channel[] getChannels() {
        return channels;
    }
    /**
     *
     * @return String
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @return String
     */
    public String getName() {
        return name;
    }
}
