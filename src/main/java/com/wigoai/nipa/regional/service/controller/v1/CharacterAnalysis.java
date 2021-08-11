package com.wigoai.nipa.regional.service.controller.v1;

import com.google.gson.JsonArray;
import com.wigoai.nipa.regional.service.NipaRegionalAnalysis;
import com.wigoai.nipa.regional.service.ServiceConfig;
import com.wigoai.nipa.regional.service.channel.ChannelManager;
import com.wigoai.nipa.regional.service.util.GroupKeyUtil;
import com.wigoai.nipa.regional.service.util.ParameterUtil;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.moara.ara.datamining.textmining.dictionary.word.WordDictionary;
import org.moara.ara.datamining.textmining.dictionary.word.element.Word;
import org.moara.common.callback.ObjectCallback;
import org.moara.common.config.Config;
import org.moara.common.util.ExceptionUtil;
import org.moara.common.util.YmdUtil;
import org.moara.keyword.KeywordAnalysis;
import org.moara.keyword.ServiceKeywordAnalysis;
import org.moara.message.disposable.DisposableMessageManager;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 인물 분석
 * 소스가 너무 길어져서 나눈 클래스
 * @author macle
 */
@Slf4j
public class CharacterAnalysis {

    public static JsonArray ner(JSONObject request) {
        JsonArray keywords = new JsonArray();

        long analysisStartTime = System.currentTimeMillis();


        long startTime = request.getLong("start_time");
        long endTime = request.getLong("end_time");
        long standardTime = request.getLong("standard_time");

        long analysisMaxTime = Config.getLong(ServiceConfig.ANALYSIS_MAX_TIME.key(), (long)ServiceConfig.ANALYSIS_MAX_TIME.defaultValue());

        final Thread currentThread = Thread.currentThread();

        AtomicBoolean isAnalysis = new AtomicBoolean(false);

        ObjectCallback endCallback = obj -> {

            try {
                isAnalysis.set(true);
                currentThread.interrupt();
            }catch(Exception e){
                log.error(ExceptionUtil.getStackTrace(e));
            }
        };

        final KeywordAnalysis.Module [] modules = new KeywordAnalysis.Module[1];
        modules[0] =  KeywordAnalysis.Module.TF_WORD_CONTENTS;

        Map<KeywordAnalysis.Module, Properties> moduleProperties = new HashMap<>();
        Properties properties = new Properties();

        JSONArray selectors = new JSONArray();
        JSONObject keywordSelector = new JSONObject();
        keywordSelector.put("id", "ner_keywords");
        keywordSelector.put("type", "CATEGORY_WORD");
        keywordSelector.put("value", Config.getConfig(ServiceConfig.POSITIVE_CODE.key(),(String) ServiceConfig.POSITIVE_CODE.defaultValue()));

        selectors.put(keywordSelector);

        properties.put("selectors", selectors.toString());

        if(!request.isNull("ner_count")){
            properties.put("count", request.getInt("ner_count"));
        }else{
            properties.put("count", 50);
        }

        if(!request.isNull("ner_scope")){
            properties.put("scope", request.getString("ner_scope"));
        }else{
            properties.put("scope", "FULL");
        }

        properties.put("is_trend", false);

        WordDictionary wordDictionary = WordDictionary.getInstance();
        Word characterWord = wordDictionary.getSyllable(request.getString("name")).getDictionaryWord().getWord();

        properties.put("scope_word", characterWord);
        moduleProperties.put(KeywordAnalysis.Module.TF_WORD_CONTENTS, properties);

        ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
        KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();

        ChannelManager channelManager = NipaRegionalAnalysis.getInstance().getChannelManager();
        String [] characterChannelIds = channelManager.getCharacterChannelIds();

        String startYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(startTime));
        String endYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(endTime-1));

        List<String> ymdList = YmdUtil.getYmdList(startYmd,endYmd);
        String [][] keysArray = GroupKeyUtil.makeChannelKeysArray(ymdList,  characterChannelIds);

        Map<String, Object> parameterMap = ParameterUtil.makeParameterMap(request);


        String messageId = keywordAnalysis.analysis(startTime, endTime, standardTime, keywordAnalysis.makeSearchKeywords(CharacterAnalysis.getKeywordJson(request)), keysArray, modules, moduleProperties, parameterMap, endCallback);


        try {
            long analysisTime = System.currentTimeMillis() - analysisStartTime;
            //최대 대기 시간
            long sleepTime = analysisMaxTime - analysisTime;
            if(sleepTime>0) {
                Thread.sleep(sleepTime);
            }
        }catch (InterruptedException ignore){}

        if(!isAnalysis.get()){
            log.error("time out: " + request.toString());
            return keywords;
        }


        DisposableMessageManager disposableMessageManager = DisposableMessageManager.getInstance();
        String responseMessage =  disposableMessageManager.getMessages(messageId);
        //데이터 변환

        JSONObject responseObj = new JSONObject(responseMessage);
        JSONArray messageArray =  responseObj.getJSONArray("messages");




        return keywords;
    }


    public static JSONArray getKeywordJson(JSONObject request){

        JSONArray keywords =new JSONArray();

        if(!request.isNull("infos")){
            String name = request.getString("name");

            JSONArray infos = request.getJSONArray("infos");
            for (int i = 0; i <infos.length() ; i++) {
                JSONObject keyword = new JSONObject();
                keyword.put("keyword", name);
                JSONArray inFilters = new JSONArray();
                inFilters.put(infos.getString(i));
                keyword.put("in_filters", inFilters);
                keywords.put(keyword);
            }


        }else{
            keywords.put(request.getString("name"));
        }

        return keywords;
    }
}
