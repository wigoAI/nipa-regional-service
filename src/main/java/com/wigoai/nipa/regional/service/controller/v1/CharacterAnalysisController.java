package com.wigoai.nipa.regional.service.controller.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.wigoai.nipa.regional.service.NipaRegionalAnalysis;
import com.wigoai.nipa.regional.service.ServiceConfig;
import com.wigoai.nipa.regional.service.channel.ChannelGroup;
import com.wigoai.nipa.regional.service.channel.ChannelManager;
import com.wigoai.nipa.regional.service.data.ChannelStatus;
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


            ChannelGroup[] groups = channelManager.getCharacterChannelGroups();

            DisposableMessageManager disposableMessageManager = DisposableMessageManager.getInstance();
            String responseMessage =  disposableMessageManager.getMessages(messageId);
            //데이터 변환

            JSONObject responseObj = new JSONObject(responseMessage);
            JSONArray messageArray =  responseObj.getJSONArray("messages");

            JsonObject resultObj = new JsonObject();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            for (int i = 0; i <messageArray.length() ; i++) {

                JSONObject messageObj = new JSONObject(messageArray.getString(i));
                KeywordAnalysis.Module module = KeywordAnalysis.Module.valueOf(messageObj.get("type").toString());


                messageObj = messageObj.getJSONObject("message");
                if (module == KeywordAnalysis.Module.TF_CONTENTS) {

                    Map<String, ChannelStatus> channelStatusMap = new HashMap<>();

                    JSONObject tfObj = messageObj.getJSONObject("tf_channel");
                    Set<String> keys = tfObj.keySet();
                    for(String key : keys){
                        ChannelStatus channelStatus = new ChannelStatus();
                        channelStatusMap.put(key, channelStatus);

                        channelStatus.setChannel_id(key);
                        channelStatus.setChannel_name(channelManager.getChannel(key).getName());
                        channelStatus.setCount(tfObj.getInt(key));


                        for(ChannelGroup channelGroup : groups){
                            if(channelGroup.hasChannel(key)){
                                channelStatus.setGroup_id(channelGroup.getId());
                                channelStatus.setGroup_name(channelGroup.getName());
                                break;
                            }
                        }

                    }
                    tfObj = messageObj.getJSONObject("title_tf");
                    for(String key : keys){
                        if(tfObj.isNull(key)){
                            continue;
                        }
                        channelStatusMap.get(key).setTitle_count(tfObj.getInt(key));
                    }


                    JSONArray codes = messageObj.getJSONArray("classify_codes");
                    int positiveIndex = 0;
                    int negativeIndex = 0;
                    int neutralIndex = 0;

                    for (int j = 0; j <codes.length() ; j++) {
                        String code = codes.getString(j);
                        if(code.equals(emotionCodes[0])){
                            positiveIndex = j;
                        }else if(code.equals(emotionCodes[1])){
                            negativeIndex = j;
                        }else if(code.equals(emotionCodes[2])){
                            neutralIndex = j;
                        }
                    }

                    tfObj = messageObj.getJSONObject("classify_tf");
                    for(String key : keys){
                        if(tfObj.isNull(key)){
                            continue;
                        }
                        JSONArray counts = tfObj.getJSONArray(key);
                        ChannelStatus channelStatus = channelStatusMap.get(key);
                        channelStatus.setPositive_count(counts.getInt(positiveIndex));
                        channelStatus.setNegative_count(counts.getInt(negativeIndex));
                        channelStatus.setNeutral_count(counts.getInt(neutralIndex));
                    }

                    ChannelStatus [] channelStatusArray = channelStatusMap.values().toArray(new ChannelStatus[0]);

                    Arrays.sort(channelStatusArray, ChannelStatus.SORT_DESC);
                    resultObj.add("channel_status_array", gson.toJsonTree(channelStatusArray));

                    channelStatusMap.clear();
                }
            }

            

            String result = gson.toJson(resultObj);
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
