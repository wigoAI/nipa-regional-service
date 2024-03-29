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

import org.moara.common.data.database.Table;
import org.moara.common.data.database.annotation.Column;
import org.moara.common.data.database.annotation.DateTime;
import org.moara.common.data.database.annotation.PrimaryKey;

/**
 * 공통 설정 정보
 * @author macle
 */
@Table(name="TB_MOARA_COMMON_CONFIG")
public class CommonConfig {

    @PrimaryKey(seq = 1)
    @Column(name = "CD_CONFIG")
    String key;
    @Column(name = "VAL_CONFIG")
    String value;
    @DateTime
    @Column(name = "DT_UPT_LAST")
    long updateTime;
}
