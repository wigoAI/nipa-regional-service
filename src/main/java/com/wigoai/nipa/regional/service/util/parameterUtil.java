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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 공통 parameter 관련 유틸
 * @author macle
 */
public class parameterUtil {

    /**
     * parameterMap 생성
     * @param request JSONObject
     * @return Map
     */
    public static Map<String, Object> makeParameterMap(JSONObject request){

        Map<String, Object> parameterMap = null;
        if(request.has("stopwords")){
            Object obj = request.get("stopwords");
            if(obj != null){
                JSONArray stopwordArray = (JSONArray) obj;
                if(stopwordArray.length() > 0) {

                    Set<String> stopwordSet = new HashSet<>();

                    for (int i = 0; i < stopwordArray.length(); i++) {
                        stopwordSet.add(stopwordArray.getString(i));
                    }
                    //옵션이 지금은 하나 이므로 여기에서 생성 나중에는 null일때만 생성하게 변경해야함
                    parameterMap = new HashMap<>();
                    parameterMap.put("stopwordSyllableSet", stopwordSet);
                }
            }
        }

        return parameterMap;
    }

}
