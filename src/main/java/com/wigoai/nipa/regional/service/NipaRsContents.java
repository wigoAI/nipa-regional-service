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
 * contents 정보
 * @author macle
 */
@Table(name="T_CRAWLING_CONTENTS")
public class NipaRsContents {


    @PrimaryKey(seq = 1)
    @Column(name = "CONTENTS_NO")
    long contentsNum;

    @Column(name = "API_CHANNEL_ID")
    String channelId;

    @Column(name = "TITLE")
    String title;

    @Column(name = "CONTENTS")
    String contents;

    @DateTime
    @Column(name = "POST_DT")
    Long postTime;

    @Column(name = "ORIGINAL_URL")
    String originalUrl;

    @Column(name = "SEQ_NO")
    long seqNum;

    /**
     * 컨텐츠 번호 설정
     * 외부 insert 용
     * @param contentsNum long
     */
    public void setContentsNum(long contentsNum) {
        this.contentsNum = contentsNum;
    }

    /**
     * 채널 아이디 설정
     * 외부 insert 용
     * @param channelId String
     */
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    /**
     * 제목 설정
     * 외부 insert 용
     * @param title String
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 본문 설정
     * 외부 insert 용
     * @param contents String
     */
    public void setContents(String contents) {
        this.contents = contents;
    }

    public void setPostTime(Long postTime) {
        this.postTime = postTime;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

}
