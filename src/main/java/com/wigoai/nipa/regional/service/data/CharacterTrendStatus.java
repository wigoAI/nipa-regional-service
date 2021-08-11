package com.wigoai.nipa.regional.service.data;

import lombok.Data;

/**
 * 인물분석 추이 현황
 * 
 * @author macle
 */
@Data
public class CharacterTrendStatus {

    private int count = 0;
    private int count_previous = 0;
    private int count_change = 0;


    private int title = 0;
    private int title_previous = 0;
    private int title_change = 0;

    private int negative = 0;
    private int negative_previous = 0;
    private int negative_change = 0;

}
