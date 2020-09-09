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

package com.wigoai.nipa.regional.service.dev;

import com.wigoai.nipa.regional.service.NipaRegionalAnalysis;
import com.wigoai.nipa.regional.service.NipaRsContents;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.moara.common.data.database.jdbc.JdbcObjects;
import org.moara.common.data.office.excel.ExcelUtil;
import org.moara.common.time.TimeUtil;
import org.moara.common.util.ExceptionUtil;
import org.moara.common.util.YmdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 개발자 피씨에서 실행용
 * 테스트 더미 데이터 생성
 * @author macle
 */
public class DummyDataInsert {


    private static final Logger logger = LoggerFactory.getLogger(DummyDataInsert.class);

    private final ExcelUtil excelUtil = new ExcelUtil();

    private XSSFRow row;

    void make(@SuppressWarnings("SameParameterValue") String excelDirPath){

        List<File> excelList = excelUtil.getExcelFileList(excelDirPath);

        if(excelList.size() == 0){
            logger.error("excel file size 0");
            return;
        }

        //날짜는 아래 날짜로 랜덤 하게 들어감
        final List<String> ymdList = YmdUtil.getYmdList("20200701", "20200930");

        final Random random = new Random();

        final String [] channelIds = {"1","2","3","4","5","6","7","8"};

        final int day = (int)TimeUtil.DAY_TIME;

        long rowNumber = 1;

        List<NipaRsContents> contentsList = new ArrayList<>();

        try(Connection conn  = NipaRegionalAnalysis.getInstance().getDataSource().getConnection()){
            for(File excel : excelList){
                logger.info("excel name: " + excel.getAbsolutePath());

                XSSFWorkbook work = new XSSFWorkbook(new FileInputStream(excel));
                excelUtil.setXSSFWorkbook(work);


                XSSFSheet sheet = work.getSheetAt(0);

                int rowCount =  excelUtil.getRowCount(sheet);


                for(int rowIndex=1 ; rowIndex<rowCount ; rowIndex++) {
                    row = sheet.getRow(rowIndex);

                    if(row == null){
                        continue;
                    }

                    String id = getCellValue(0);
                    if(id == null){
                        continue;
                    }

                    String title = getCellValue(12);
                    String contents = getCellValue(13).replaceAll("\\s{3,}", "\n");


                    long time = new SimpleDateFormat("yyyyMMdd").parse(ymdList.get(random.nextInt(ymdList.size()))).getTime() + (long)random.nextInt(day);


//                    time = time/1000;

                    NipaRsContents nipaRsContents = new NipaRsContents();
                    nipaRsContents.setContentsNum(rowNumber++);
                    nipaRsContents.setChannelId(channelIds[random.nextInt(channelIds.length)]);
                    nipaRsContents.setTitle(title);
                    nipaRsContents.setContents(contents);
                    nipaRsContents.setPostTime(time);
                    nipaRsContents.setOriginalUrl("https://github.com/");

                    contentsList.add(nipaRsContents);

                    //1000개 단위로 insert
                    if(contentsList.size() > 1000){
                        JdbcObjects.insert(conn, contentsList, true);
                        contentsList.clear();
                    }
                }
            }

            if(contentsList.size() > 0){
                JdbcObjects.insert(conn, contentsList, true);

                contentsList.clear();
            }


        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));

        }
    }

    private String getCellValue(int cellNum){
        return excelUtil.getCellValue(row, cellNum);
    }

    public static void main(String[] args){
        //환경부 데이터를 이용해서 만듬 관련데이터는 라이센스가 있는 데이터이므로 레파지토리에 저장하면 안됨
        new DummyDataInsert().make("D:\\moara\\뉴스셈플데이터(환경부)");
    }

}
