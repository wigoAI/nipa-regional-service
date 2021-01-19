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

package com.wigoai.nipa.regional.service.dev;

import com.seomse.commons.utils.FileUtil;
import com.seomse.commons.utils.string.highlight.StringHighlight;
import org.json.JSONObject;
import org.moara.common.callback.Callback;
import org.moara.common.config.ConfigSet;
import org.moara.engine.MoaraEngine;
import org.moara.keyword.KeywordAnalysis;
import org.moara.keyword.ServiceKeywordAnalysis;
import org.moara.keyword.index.IndexData;

/**
 * @author macle
 */
public class HighlightErrorCheck {

    public static void main(String[] args) {

//        final String id = "44104396";
        final String id = "42942513";

//        obj.addProperty("highlight", SearchHighlight.highlight(data, analysisContents, highlightKeywords, preTag, postTag, maxLength).replace('\n', ' '));
        
        
        final String [] keywords = {
                "강원도"
                , "춘천시"
        };

        final String pre = "<span class=\"point\">";
        final String post = "</span>";
        final int length = 160;

        ConfigSet.setPath("config/config.xml");
        MoaraEngine.newInstance("macle");

        ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();

        Callback initCallback = () -> {
            KeywordAnalysis keywordAnalysis = ServiceKeywordAnalysis.getInstance().getKeywordAnalysis();
            IndexData indexData = keywordAnalysis.search(id);
            JSONObject obj =  new JSONObject(FileUtil.getLine(indexData.getIndexFileName(), indexData.getIndexFileLine())).getJSONObject(IndexData.Keys.DETAIL.key());

            String[] words = indexData.getWordsValue().split(" ");
            int[] wordPositions = indexData.getWordPositions();

            String contents = obj.getString("analysis_contents");

            System.out.println("contents length:: " + contents.length());

            String text = StringHighlight.make(contents, words, wordPositions, indexData.getSentencePositions(), keywords, pre, post, length);
            System.out.println(text);

            System.exit(0);
        };

        serviceKeywordAnalysis.addInitCallback(initCallback);


    }


}
