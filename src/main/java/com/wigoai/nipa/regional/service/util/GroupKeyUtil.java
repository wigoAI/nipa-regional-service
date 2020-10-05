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

import com.wigoai.nipa.regional.service.ChannelGroup;

import java.util.List;

/**
 * contents group key 생성 유틸
 * @author macle
 */
public class GroupKeyUtil {


    /**
     * keys 생성
     * @param ymdList List
     * @param groups ChannelGroup []
     * @return String [] []
     */
    public static String [][] makeKeysArray(List<String> ymdList,  ChannelGroup[] groups){
        String [] groupIds = new String[groups.length];

        for (int i = 0; i < groups.length ; i++) {
            groupIds[i] = groups[i].getId();
        }

        return makeKeysArray(ymdList, groupIds);
    }

    /**
     * keys 생성
     * @param ymdList List
     * @param groupIds String []
     * @return String [] []
     */
    public static String [][] makeKeysArray(List<String> ymdList,  String [] groupIds){
        int size = ymdList.size()*groupIds.length;
        String [][] keysArray = new String[size][2];

        int index = 0;
        for(String ymd : ymdList){
            for (String groupId : groupIds) {
                String[] key = new String[2];
                key[0] = ymd;
                key[1] = groupId;
                keysArray[index++] = key;
            }
        }

        return keysArray;
    }


    /**
     * keys 생성
     * @param ymdList List
     * @param groupId Sring
     * @return  String [] []
     */
    public static String [][] makeKeysArray(List<String> ymdList,  String groupId){
        String [][] keysArray = new String[ymdList.size()][1];
        for (int i = 0; i <keysArray.length ; i++) {
            String[] key = new String[2];
            key[0] = ymdList.get(i);
            key[1] = groupId;
            keysArray[i] = key;
        }

        return keysArray;
    }
}
