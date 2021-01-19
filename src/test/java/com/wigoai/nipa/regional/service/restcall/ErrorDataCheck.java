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

package com.wigoai.nipa.regional.service.restcall;

import com.seomse.crawling.core.http.HttpUrl;

/**
 * 하이라이트 오류 데이터 추출
 * @author macle
 */
public class ErrorDataCheck {

    public static void main(String[] args) {

        String id = "42942513";
        String responseMessage = HttpUrl.getScript("http://sc.wigo.ai:10015/nipars/v1/search/data?id=" + id,null);


        System.out.println(responseMessage);

    }
}
