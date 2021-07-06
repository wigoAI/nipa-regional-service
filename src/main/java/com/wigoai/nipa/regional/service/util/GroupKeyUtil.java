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

package com.wigoai.nipa.regional.service.util;


import com.wigoai.nipa.regional.service.channel.Channel;
import com.wigoai.nipa.regional.service.channel.ChannelGroup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * contents group key 생성 유틸
 * @author macle
 */
public class GroupKeyUtil {



    public static String [][] makeKeysArray(List<String> ymdList,  ChannelGroup group){


        Channel[] channels = group.getChannels();
        String [] channelIds = new String[channels.length];

        for (int i = 0; i <channels.length ; i++) {
            channelIds[i] = channels[i].getId();
        }



        return makeChannelKeysArray(ymdList, channelIds);
    }


    /**
     * keys 생성
     * @param ymdList List
     * @param groups ChannelGroup []
     * @return String [] []
     */
    public static String [][] makeKeysArray(List<String> ymdList,  ChannelGroup[] groups){

        Set<String> overlapCheck = new HashSet<>();

        for(ChannelGroup channelGroup : groups){
            Channel[] channels = channelGroup.getChannels();
            for(Channel channel : channels){
                overlapCheck.add(channel.getId());
            }
        }

        return makeChannelKeysArray(ymdList, overlapCheck.toArray(new String[0]));
    }

    /**
     * keys 생성
     * @param ymdList List
     * @param channelIds 채널 아이디 배열
     * @return String [] []
     */
    public static String [][] makeChannelKeysArray(List<String> ymdList,  String [] channelIds){
        int size = ymdList.size()*channelIds.length;
        String [][] keysArray = new String[size][2];

        int index = 0;
        for(String ymd : ymdList){
            for (String channelId : channelIds) {
                String[] key = new String[2];
                key[0] = ymd;
                key[1] = channelId;
                keysArray[index++] = key;
            }
        }

        return keysArray;
    }

}
