package com.wigoai.nipa.regional.service.controller.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.seomse.commons.utils.FileUtil;
import com.wigoai.nipa.regional.service.NipaRegionalAnalysis;
import com.wigoai.nipa.regional.service.ServiceConfig;
import com.wigoai.nipa.regional.service.channel.ChannelGroup;
import com.wigoai.nipa.regional.service.channel.ChannelManager;
import com.wigoai.nipa.regional.service.util.GroupKeyUtil;
import com.wigoai.nipa.regional.service.util.ParameterUtil;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.moara.ara.datamining.data.CodeName;
import org.moara.ara.datamining.textmining.api.document.DocumentStandardKey;
import org.moara.common.callback.ObjectCallback;
import org.moara.common.config.Config;
import org.moara.common.time.TimeUtil;
import org.moara.common.util.ExceptionUtil;
import org.moara.common.util.YmdUtil;
import org.moara.keyword.KeywordAnalysis;
import org.moara.keyword.ServiceKeywordAnalysis;
import org.moara.keyword.index.IndexData;
import org.moara.keyword.index.IndexUtil;
import org.moara.keyword.search.data.SearchData;
import org.moara.message.disposable.DisposableMessageManager;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 채널별 분석
 * 말로하는 제안청원 채널 분석용
 * @author macle
 */
@Slf4j
@RestController
public class ChannelAnalysisController {


    /**
     * 통합분석 초기 결과 얻기
     * @param jsonValue String json object
     * @return String json object
     */
    @RequestMapping(value = "/nipars/v1/channel/integrated" , method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public String integrated(@RequestBody final String jsonValue) {

        try {

            long analysisStartTime = System.currentTimeMillis();

            JSONObject request = new JSONObject(jsonValue);

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

            int keywordCount;
            if(request.has("keyword_count")){
                keywordCount = request.getInt("keyword_count");
            }else{
                keywordCount = 30;
            }

            final KeywordAnalysis.Module [] modules = new KeywordAnalysis.Module[3];
            modules[0] = KeywordAnalysis.Module.TF_CONTENTS;
            modules[1] = KeywordAnalysis.Module.TF_WORD_CONTENTS;
            modules[2] = KeywordAnalysis.Module.TF_CLASSIFY;

            ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
            KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();

            NipaRegionalAnalysis nipaRegionalAnalysis = NipaRegionalAnalysis.getInstance();

            String startYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(startTime));
            String endYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(endTime-1));

            JSONArray channelArray = request.getJSONArray("channels");

            String [] channels = new String[channelArray.length()];

            for (int i = 0; i < channels.length; i++) {
                channels[i] = channelArray.getString(i);
            }

            StringBuilder sourceBuilder =new StringBuilder();
            if(request.has("classify_names") ){
                JSONArray array = request.getJSONArray("classify_names");
                for (int i = 0; i <array.length() ; i++) {
                    String code = nipaRegionalAnalysis.getFieldCode(array.getString(i));
                    if(code == null){
                        log.error("field classify code search fail: " + array.getString(i));
                        continue;
                    }
                    sourceBuilder.append(",").append(code);
                }
            }else{
                String [] fieldCodes = nipaRegionalAnalysis.getFieldCodes();
                for(String code : fieldCodes){
                    sourceBuilder.append(",").append(code);
                }

            }

            //날짜정보
            List<String> ymdList = YmdUtil.getYmdList(startYmd,endYmd);
            String [][] keysArray = GroupKeyUtil.makeChannelKeysArray(ymdList, channels);

            Map<KeywordAnalysis.Module, Properties> moduleProperties = new HashMap<>();
            Properties properties = new Properties();
            properties.put("in_codes", sourceBuilder.substring(1));
            properties.put("is_trend", false);
            moduleProperties.put(KeywordAnalysis.Module.TF_CLASSIFY, properties);

            properties = new Properties();
            properties.put("selectors","[{\"id\":\"keywords\",\"type\":\"WORD_CLASS\",\"value\":\"NOUN\"}]");
            properties.put("count", keywordCount);

            moduleProperties.put(KeywordAnalysis.Module.TF_WORD_CONTENTS, properties);

            String [] emotionCodes = nipaRegionalAnalysis.getEmotionCodes();

            StringBuilder emotionBuilder = new StringBuilder();
            for(String emotionCode : emotionCodes){
                emotionBuilder.append(",").append(emotionCode);
            }

            properties = new Properties();
            properties.put("in_codes", emotionBuilder.substring(1));
            properties.put("is_trend", false);
            moduleProperties.put(KeywordAnalysis.Module.TF_CLASSIFY, properties);

            Map<String, Object> parameterMap = ParameterUtil.makeParameterMap(request);

            String messageId = keywordAnalysis.analysis(startTime, endTime, standardTime, null, keysArray, modules, moduleProperties, parameterMap, endCallback);

            try {
                long analysisTime = System.currentTimeMillis() - analysisStartTime;
                //최대 대기 시간

                long sleepTime = analysisMaxTime - analysisTime;
                if(sleepTime>0) {
                    Thread.sleep(sleepTime);
                }
            }catch (InterruptedException ignore){}

            if(!isAnalysis.get()){
                log.error("time out: " + jsonValue);
                return "{}";
            }

            DisposableMessageManager disposableMessageManager = DisposableMessageManager.getInstance();
            String responseMessage =  disposableMessageManager.getMessages(messageId);

            JSONObject responseObj = new JSONObject(responseMessage);
            JSONArray messageArray =  responseObj.getJSONArray("messages");
            JSONObject resultObj = new JSONObject();

            for (int i = 0; i <messageArray.length() ; i++) {

                JSONObject messageObj = new JSONObject(messageArray.getString(i));
                KeywordAnalysis.Module module = KeywordAnalysis.Module.valueOf(messageObj.get("type").toString());
                if (module == KeywordAnalysis.Module.TF_CONTENTS) {
                    resultObj.put("channel_count", messageObj.getJSONObject("message"));
                }else if(module == KeywordAnalysis.Module.TF_WORD_CONTENTS){
                    resultObj.put("keywords", messageObj.getJSONObject("message"));
                } else {
                    resultObj.put("classifies", messageObj.getJSONObject("message").getJSONArray("classifies"));
                }
            }

            //파싱 및 변환에 대한 속도차이는 없는것으로 보임
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject jsonObj = gson.fromJson(resultObj.toString(), JsonObject.class);
            String result = gson.toJson(jsonObj);
            log.debug("analysis second: " + jsonValue +":  "+ TimeUtil.getSecond(System.currentTimeMillis() - analysisStartTime));
            return result;

        }catch(Exception e){
            log.error(ExceptionUtil.getStackTrace(e));
            return "{}";
        }
    }



    /**
     * 주제별분석 초기 결과 얻기
     * @param jsonValue String json object
     * @return String json object
     */
    @RequestMapping(value = "/nipars/v1/channel/subject" , method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public String subject(@RequestBody final String jsonValue) {
        long analysisStartTime = System.currentTimeMillis();

        NipaRegionalAnalysis nipaRegionalAnalysis = NipaRegionalAnalysis.getInstance();

        ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
        KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();
        JSONObject request = new JSONObject(jsonValue);

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

        JSONArray channelArray = request.getJSONArray("channels");

        String [] channels = new String[channelArray.length()];

        for (int i = 0; i < channels.length; i++) {
            channels[i] = channelArray.getString(i);
        }



        int keywordCount;
        if(request.has("keyword_count")){
            keywordCount = request.getInt("keyword_count");
        }else{
            keywordCount = 100;
        }

        Properties snaProperties = null;
        if(request.has("sna_use_count")){

            snaProperties = new Properties();

            snaProperties.put("use_count", request.getInt("sna_use_count"));
        }
        if(request.has("sna_source_count")){
            if(snaProperties == null){
                snaProperties = new Properties();
            }
            snaProperties.put("source_count", request.getInt("sna_source_count"));
        }
        if(request.has("sna_target_count")){
            if(snaProperties == null){
                snaProperties = new Properties();
            }
            snaProperties.put("target_count", request.getInt("sna_target_count"));
        }

        Map<KeywordAnalysis.Module, Properties> moduleProperties = new HashMap<>();
        Properties properties = new Properties();

        JSONArray selectors = new JSONArray();
        JSONObject positiveSelector = new JSONObject();
        positiveSelector.put("id", "positive_keywords");
        positiveSelector.put("type", "CATEGORY_ARRAY_WORD");
        positiveSelector.put("value", Config.getConfig(ServiceConfig.POSITIVE_CODE.key(),(String) ServiceConfig.POSITIVE_CODE.defaultValue()));

        JSONObject negativeSelector = new JSONObject();
        negativeSelector.put("id", "negative_keywords");
        negativeSelector.put("type", "CATEGORY_ARRAY_WORD");
        negativeSelector.put("value", Config.getConfig(ServiceConfig.NEGATIVE_CODE.key(),(String) ServiceConfig.NEGATIVE_CODE.defaultValue()));

        JSONObject nounSelector = new JSONObject();
        negativeSelector.put("id", "noun_keywords");
        negativeSelector.put("type", "WORD_CLASS");
        negativeSelector.put("value","NOUN");


        selectors.put(positiveSelector);
        selectors.put(negativeSelector);
        selectors.put(nounSelector);
        properties.put("selectors", selectors.toString());

        properties.put("count", keywordCount);
        properties.put("is_trend",false);
        moduleProperties.put(KeywordAnalysis.Module.TF_WORD_CONTENTS, properties);

        if(snaProperties != null){
            moduleProperties.put(KeywordAnalysis.Module.SNA_LITE, snaProperties);
        }


        properties = new Properties();
        String [] emotionCodes = nipaRegionalAnalysis.getEmotionCodes();
        StringBuilder emotionBuilder = new StringBuilder();
        for(String emotionCode : emotionCodes){
            emotionBuilder.append(",").append(emotionCode);
        }
        properties.put("in_codes", emotionBuilder.substring(1));
        properties.put("is_trend", true);
        moduleProperties.put(KeywordAnalysis.Module.TF_CLASSIFY, properties);

        final KeywordAnalysis.Module [] modules = new KeywordAnalysis.Module[4];
        modules[0] = KeywordAnalysis.Module.TF_CONTENTS;
        modules[1] = KeywordAnalysis.Module.TF_CLASSIFY;
        modules[2] = KeywordAnalysis.Module.TF_WORD_CONTENTS;
        modules[3] = KeywordAnalysis.Module.SNA_LITE;

        String startYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(startTime));
        String endYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(endTime-1));

        List<String> ymdList = YmdUtil.getYmdList(startYmd,endYmd);
        String [][] keysArray = GroupKeyUtil.makeChannelKeysArray(ymdList, channels);

        Map<String, Object> parameterMap = ParameterUtil.makeParameterMap(request);
        final String messageId = keywordAnalysis.analysis(startTime, endTime, standardTime, null, keysArray, modules, moduleProperties, parameterMap , endCallback);
        
        try {
            long analysisTime = System.currentTimeMillis() - analysisStartTime;
            //최대 대기 시간

            long sleepTime = analysisMaxTime - analysisTime;
            if(sleepTime>0) {
                Thread.sleep(sleepTime);
            }
        }catch (InterruptedException ignore){}

        if(!isAnalysis.get()){
            log.error("time out: " + jsonValue);
            return "{}";
        }

        DisposableMessageManager disposableMessageManager = DisposableMessageManager.getInstance();
        String responseMessage =  disposableMessageManager.getMessages(messageId);

        JSONObject responseObj = new JSONObject(responseMessage);
        JSONArray messageArray =  responseObj.getJSONArray("messages");
        JSONObject resultObj = new JSONObject();

        for (int i = 0; i <messageArray.length() ; i++) {

            JSONObject messageObj = new JSONObject(messageArray.getString(i));
            KeywordAnalysis.Module module = KeywordAnalysis.Module.valueOf(messageObj.get("type").toString());
            if (module == KeywordAnalysis.Module.TF_CONTENTS) {
                resultObj.put("channel_count", messageObj.getJSONObject("message"));
            }else if(module == KeywordAnalysis.Module.TF_CLASSIFY){
                resultObj.put("emotion_classifies_trend", messageObj.getJSONObject("message"));
            } else if(module == KeywordAnalysis.Module.TF_WORD_CONTENTS){
                JSONObject emotionObj = messageObj.getJSONObject("message");
                resultObj.put("negative_keywords", emotionObj.getJSONArray("negative_keywords"));
                resultObj.put("positive_keywords", emotionObj.getJSONArray("positive_keywords"));
                resultObj.put("noun_keywords", emotionObj.getJSONArray("noun_keywords"));
            }else{
                resultObj.put("networks", messageObj.getJSONArray("message"));
            }
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        JsonObject jsonObj = gson.fromJson(resultObj.toString(), JsonObject.class);
        String result = gson.toJson(jsonObj);
        log.debug("analysis second: " + jsonValue +":  "+ TimeUtil.getSecond(System.currentTimeMillis() - analysisStartTime));
        return result;
    }


    /**
     * 타임라인 결과 얻기
     * @param jsonValue String json object
     * @return String json object
     */
    @RequestMapping(value = "/nipars/v1/channel/timeline" , method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public String timeLine(@RequestBody final String jsonValue){
        try {

            JSONObject request = new JSONObject(jsonValue);

            SearchData searchData = search(request);
            if(searchData == null|| searchData.getDataArray().length == 0){
                return DataSearchController.nullResult;
            }

            IndexData[] searchArray = searchData.getDataArray();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject response = new JsonObject();
            response.addProperty("total", searchArray.length);
            String classifyCode = Config.getConfig(ServiceConfig.FIELD_CLASSIFY.key());


            int begin;
            if(request.isNull("begin")){
                begin = request.getInt("start");
            }else{
                begin  = request.getInt("begin");
            }

            IndexData[] subArray = IndexUtil.subData(searchArray, begin, request.getInt("end"));
            JsonArray dataArray = new JsonArray();
            for(IndexData data : subArray){
                JsonObject obj =  gson.fromJson(FileUtil.getLine(data.getIndexFileName(), data.getIndexFileLine()),JsonObject.class).getAsJsonObject(IndexData.Keys.DETAIL.key());
                obj.remove(IndexData.Keys.WORD_POSITIONS.key());
                obj.remove(IndexData.Keys.SENTENCE_POSITIONS.key());
                obj.remove(DocumentStandardKey.LANG_CODE.key());
                obj.remove(DocumentStandardKey.DOC_TYPE.key());
                obj.addProperty("id", data.getId());

                JsonArray classifies = new JsonArray();
                CodeName[] codeNames = data.getClassifies();
                for(CodeName codeName : codeNames){
                    if(!codeName.getCode().startsWith(classifyCode)){
                        continue;
                    }
                    JsonObject classifyObj = new JsonObject();
                    classifyObj.addProperty("code", codeName.getCode());
                    classifyObj.addProperty("name", codeName.getName());
                    classifies.add(classifyObj);

                }
                obj.remove("analysis_contents");

                obj.add("classifies", classifies);
                dataArray.add(obj);
            }

            response.add("data_array", dataArray);
            return gson.toJson(response);
        }catch(Exception e){
            log.error(ExceptionUtil.getStackTrace(e));
            return DataSearchController.nullResult;
        }
    }



    /**
     * 데이터 검색겱과 얻기
     * @param request JSONObject
     * @return  IndexData[]
     */
    public SearchData search(final JSONObject request){
        long startTime = request.getLong("start_time");
        long endTime = request.getLong("end_time");
        long standardTime = request.getLong("standard_time");
        long analysisMaxTime = Config.getLong(ServiceConfig.ANALYSIS_MAX_TIME.key(), (long)ServiceConfig.ANALYSIS_MAX_TIME.defaultValue());

        ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
        KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();

        String startYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(startTime));
        String endYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(endTime-1));

        JSONArray channelArray = request.getJSONArray("channels");

        String [] channels = new String[channelArray.length()];

        for (int i = 0; i < channels.length; i++) {
            channels[i] = channelArray.getString(i);
        }


        List<String> ymdList = YmdUtil.getYmdList(startYmd,endYmd);
        String [][] keysArray = GroupKeyUtil.makeChannelKeysArray(ymdList, channels);

        return  keywordAnalysis.dataSearch(startTime, endTime, standardTime, null, keysArray , analysisMaxTime);
    }

}