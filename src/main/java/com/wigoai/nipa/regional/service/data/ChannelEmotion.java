package com.wigoai.nipa.regional.service.data;

import lombok.Data;

import java.util.Comparator;

/**
 * 채널별 감성정보
 * 정보성객체
 * @author macle
 */
@Data
public class ChannelEmotion {

    public static final Comparator<ChannelEmotion> SORT_DESC = (c1, c2) -> Integer.compare(c2.count, c1.count);

    private String group_id;
    private String group_name;

    private String channel_id;
    private String channel_name;

    private int count = 0;

    private int positive_count = 0;

    private int negative_count = 0;

    private int neutral_count = 0;
}
