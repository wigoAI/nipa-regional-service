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
import org.moara.common.data.database.annotation.DateTime;
import org.moara.common.data.database.annotation.FlagBoolean;
import org.moara.common.data.database.annotation.PrimaryKey;

/**
 * 채널 업데이트 용
 * @author macle
 */
@Table(name="T_CRAWLING_CLIENT_CHANNEL")
public class UpdateChannel {


    @PrimaryKey(seq = 1)
    @Column(name = "API_CHANNEL_ID")
    String id;

    @Column(name = "CHANNEL_NM")
    String name;

    @FlagBoolean
    @Column(name = "DEL_FG")
    boolean isDel;

    @DateTime
    @Column(name = "UPT_DT")
    long time;

}
