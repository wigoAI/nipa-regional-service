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

import org.json.JSONArray;
import org.json.JSONObject;
import org.moara.ara.datamining.data.CodeName;
import org.moara.common.config.Config;
import org.moara.common.data.file.FileUtil;
import org.moara.keyword.index.IndexData;

import java.io.File;
import java.util.List;
import java.util.Random;

/**
 * 뷴류 더미 데이터 생성
 * @author macle
 */
public class DummyDataClassify {

    public static void main(String[] args) {

        CodeName [] emotionArray = new CodeName[3];
        emotionArray[0] = new CodeName("U716001","긍정");
        emotionArray[1] = new CodeName("U716002","중립");
        emotionArray[2] = new CodeName("U716003","부정");

        CodeName [] classifies = new CodeName[7];
        classifies[0] = new CodeName("U718001","보건위생");
        classifies[1] = new CodeName("U718002","재난안전");
        classifies[2] = new CodeName("U718003","청소환경");
        classifies[3] = new CodeName("U718004","건설교통");
        classifies[4] = new CodeName("U718005","교육");
        classifies[5] = new CodeName("U718006","경제산업");
        classifies[6] = new CodeName("U718007","기타");

        String path = Config.getConfig("keyword.index.path");
        List<File> fileList = FileUtil.getFileList(path,"md");

        Random random = new Random();

        for(File file : fileList){

            System.out.println();

            String contents = FileUtil.getFileContents(file, "UTF-8");

            String [] lineArray = contents.split("\n");

            StringBuilder sb = new StringBuilder();
            for(String line : lineArray){

                JSONArray classifyArray = new JSONArray();

                JSONObject emotionObj = new JSONObject();
                CodeName codeName = emotionArray[random.nextInt(emotionArray.length)];
                emotionObj.put("code", codeName.getCode());
                emotionObj.put("name", codeName.getName());
                classifyArray.put(emotionObj);

                JSONObject classifyObj = new JSONObject();
                codeName = classifies[random.nextInt(classifies.length)];
                classifyObj.put("code", codeName.getCode());
                classifyObj.put("name", codeName.getName());
                classifyArray.put(classifyObj);

                JSONObject jsonObject = new JSONObject(line);
                jsonObject.put(IndexData.Keys.CLASSIFY_ARRAY.key(), classifyArray);
                sb.append("\n").append(jsonObject.toString());
            }

            FileUtil.fileOutput(sb.substring(1),file.getAbsolutePath(),false);



        }


    }

}
