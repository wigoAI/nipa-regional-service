package com.wigoai.nipa.regional.service.controller.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.wigoai.nipa.regional.service.NipaRegionalAnalysis;
import com.wigoai.nipa.regional.service.ServiceConfig;
import com.wigoai.nipa.regional.service.channel.ChannelManager;
import com.wigoai.nipa.regional.service.util.GroupKeyUtil;
import com.wigoai.nipa.regional.service.util.parameterUtil;
import org.json.JSONObject;
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
import org.springframework.web.bind.annotation.RequestBody;
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

    public String analysis(@RequestBody final String jsonValue) {


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

            Map<KeywordAnalysis.Module, Properties> moduleProperties = new HashMap<>();


            ServiceKeywordAnalysis serviceKeywordAnalysis = ServiceKeywordAnalysis.getInstance();
            KeywordAnalysis keywordAnalysis = serviceKeywordAnalysis.getKeywordAnalysis();


            Map<String, Object> parameterMap = parameterUtil.makeParameterMap(request);
            String keywordJson = request.getJSONArray("keywords").toString();

            String messageId = keywordAnalysis.keywordAnalysis(startTime, endTime, standardTime, keywordJson, keysArray, modules, moduleProperties, parameterMap, endCallback);


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

}
