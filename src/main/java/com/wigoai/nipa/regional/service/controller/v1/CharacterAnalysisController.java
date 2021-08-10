package com.wigoai.nipa.regional.service.controller.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.wigoai.nipa.regional.service.NipaRegionalAnalysis;
import com.wigoai.nipa.regional.service.ServiceConfig;
import com.wigoai.nipa.regional.service.channel.ChannelManager;
import com.wigoai.nipa.regional.service.util.GroupKeyUtil;
import com.wigoai.nipa.regional.service.util.parameterUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.moara.ara.datamining.textmining.dictionary.word.WordDictionary;
import org.moara.ara.datamining.textmining.dictionary.word.element.Word;
import org.moara.common.callback.ObjectCallback;
import org.moara.common.config.Config;
import org.moara.common.time.TimeUtil;
import org.moara.common.util.ExceptionUtil;
import org.moara.common.util.YmdUtil;
import org.moara.keyword.KeywordAnalysis;
import org.moara.keyword.ServiceKeywordAnalysis;
import org.moara.message.disposable.DisposableMessageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 인물 분석
 * @author macle
 */
@RestController
public class CharacterAnalysisController {
    private static final Logger logger = LoggerFactory.getLogger(DataSearchController.class);

    @RequestMapping(value = "/nipars/v1/character/status" , method = RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public String status(@RequestBody final String jsonValue) {


        try{

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
                    logger.error(ExceptionUtil.getStackTrace(e));
                }
            };

            ChannelManager channelManager = NipaRegionalAnalysis.getInstance().getChannelManager();
            String [] characterChannelIds = channelManager.getCharacterChannelIds();

            String startYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(startTime));
            String endYmd =  new SimpleDateFormat("yyyyMMdd").format(new Date(endTime-1));

            List<String> ymdList = YmdUtil.getYmdList(startYmd,endYmd);
            String [][] keysArray = GroupKeyUtil.makeChannelKeysArray(ymdList,  characterChannelIds);

            final KeywordAnalysis.Module [] modules = new KeywordAnalysis.Module[1];
            modules[0] =  KeywordAnalysis.Module.TF_CONTENTS;


            WordDictionary wordDictionary = WordDictionary.getInstance();
            Word characterWord = wordDictionary.getSyllable(request.getString("name")).getDictionaryWord().getWord();


            NipaRegionalAnalysis nipaRegionalAnalysis = NipaRegionalAnalysis.getInstance();

            String [] emotionCodes = nipaRegionalAnalysis.getEmotionCodes();


            Map<KeywordAnalysis.Module, Properties> moduleProperties = new HashMap<>();


            Properties properties = new Properties();
            properties.put("title_word", characterWord);
            properties.put("classify_codes", emotionCodes);

            moduleProperties.put(KeywordAnalysis.Module.TF_CONTENTS, properties);

            ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
            KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();


            Map<String, Object> parameterMap = parameterUtil.makeParameterMap(request);

            String messageId = keywordAnalysis.analysis(startTime, endTime, standardTime, keywordAnalysis.makeSearchKeywords(getKeywordJson(request)), keysArray, modules, moduleProperties, parameterMap, endCallback);

            try {
                long analysisTime = System.currentTimeMillis() - analysisStartTime;
                //최대 대기 시간
                long sleepTime = analysisMaxTime - analysisTime;
                if(sleepTime>0) {
                    Thread.sleep(sleepTime);
                }
            }catch (InterruptedException ignore){}

            if(!isAnalysis.get()){
                logger.error("time out: " + jsonValue);
                return "{}";
            }


            DisposableMessageManager disposableMessageManager = DisposableMessageManager.getInstance();
            String responseMessage =  disposableMessageManager.getMessages(messageId);
            //데이터 변환
            
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String result = gson.toJson(gson.fromJson(responseMessage, JsonObject.class));
            logger.debug("analysis second: " + jsonValue +":  "+ TimeUtil.getSecond(System.currentTimeMillis() - analysisStartTime));
            return result;


        }catch(Exception e){
            logger.error(ExceptionUtil.getStackTrace(e));
            return "{}";
        }

    }


    private JSONArray getKeywordJson(JSONObject request){

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
