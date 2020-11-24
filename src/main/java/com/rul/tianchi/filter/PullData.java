package com.rul.tianchi.filter;

import com.alibaba.fastjson.JSON;
import com.rul.tianchi.CommonController;
import com.rul.tianchi.NodePort;
import com.rul.tianchi.Utils;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 从数据流拉取数据
 *
 * @author RuL
 */
public class PullData {
    private static final Logger LOGGER = LoggerFactory.getLogger(PullData.class);
    private static final String LOCALHOST = "http://localhost:";

    public static void pullData() {
        ExecutorService threadPool = Executors.newFixedThreadPool(50);
        //连接到数据源
        HttpURLConnection connection = null;
        try {
            connection = getHttpConnection();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("connect fail");
        }
        if (connection == null) {
            LOGGER.info("connection is null");
            return;
        }

        try {
            InputStream input = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line;
            //记录当前数据的行数
            long count = 0;
            int index = 0;
            HashMap<String, ArrayList<String>> traces = FilterData.TRACE_CACHE.get(index);
            HashSet<String> badTraceIds = new HashSet<>();

            String traceId;
            String tags;

            LOGGER.info("start pulling data");
            while ((line = reader.readLine()) != null) {
                count++;
                String[] traceIdAndTags = Utils.parseTraceIdAndTags(line);
                traceId = traceIdAndTags[0];
                tags = traceIdAndTags[1];

                ArrayList<String> spans;
                if (traces.get(traceId) == null) {
                    spans = new ArrayList<>();
                } else {
                    spans = traces.get(traceId);
                }
                spans.add(line);
                //将数据添加到traces
                traces.put(traceId, spans);

                //判断是否是符合条件的traceId
                if (tags.contains("error=1") ||
                        (tags.contains("http.status_code=") && !tags.contains("http.status_code=200"))) {
                    badTraceIds.add(traceId);
                }

                //切换缓存区
                if (count % FilterData.TRACE_MAP_SIZE == 0) {
                    index++;
                    if (index >= FilterData.CACHE_SIZE) {
                        index = 0;
                    }

                    if(badTraceIds.size()>0){
                        int cachePos = (int) (count / FilterData.TRACE_MAP_SIZE - 1);
                        HashSet<String> traceIds = new HashSet<>(badTraceIds);
                        threadPool.execute(() -> setBadTraceId(traceIds, cachePos));
                    }

                    //切换到下一个缓冲区
                    traces = FilterData.TRACE_CACHE.get(index);
                    while (traces.size() > 0) {
                        Thread.sleep(5);
                    }
                    //清空badTraceIds
                    badTraceIds.clear();
                }
            }
            long finalCount = count;
            LOGGER.info("######Daren Testing finalCount = " + finalCount);
            threadPool.execute(() -> setBadTraceId(badTraceIds, (int) (finalCount / FilterData.TRACE_MAP_SIZE)));
            reader.close();
            input.close();
            filterFinish();
            LOGGER.info("finish pull data");
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("catch IOException");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取数据源http连接
     */
    private static HttpURLConnection getHttpConnection() throws Exception {
        String port = System.getProperty("server.port", "8080");
        String path;
        if ("8000".equals(port)) {
            path = LOCALHOST + CommonController.getDataSourcePort() + "/trace1.data";
        } else if ("8001".equals(port)) {
            path = LOCALHOST + CommonController.getDataSourcePort() + "/trace2.data";
        } else {
            path = "";
        }
        URL url = new URL(path);
        return (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
    }

    /**
     * 发送badTraceId集合到汇总节点
     *
     * @param badTraceIds badTraceId集合
     * @param cachePos    批次编号
     */
    public static void setBadTraceId(HashSet<String> badTraceIds, Integer cachePos) {
        try {
            String badTraceIdJson = JSON.toJSONString(badTraceIds);
            RequestBody body = new FormBody.Builder().add("badTraceIdJson", badTraceIdJson)
                    .add("cachePos", cachePos + "").build();
            Request request = new Request.Builder().url(LOCALHOST + NodePort.GATHER_PORT + "/setBadTraceId")
                    .post(body).build();
            Response response = Utils.callHttp(request);
            response.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取指定缓冲区的badTrace
     *
     * @param index       缓冲区位置
     * @param badTraceIds badTraceIds
     * @param result      结果
     */
    public static void getBadTrace(int index, HashSet<String> badTraceIds, HashMap<String, ArrayList<String>> result) {
        HashMap<String, ArrayList<String>> traceMap = FilterData.TRACE_CACHE.get(index);
        for (String traceId : badTraceIds) {
            ArrayList<String> spans = traceMap.get(traceId);
            if (spans != null) {
                ArrayList<String> resultSpans = result.get(traceId);
                if (resultSpans != null) {
                    resultSpans.addAll(spans);
                } else {
                    result.put(traceId, spans);
                }
            }
        }

//        if(previousFlag){
//            HashMap<String, ArrayList<String>> prevCache = FilterData.TRACE_CACHE.get(index);
//            prevCache.clear();
//        }
    }

    /**
     * 通知汇总节点数据拉取结束
     */
    public static void filterFinish() {
        try {
            Request request = new Request.Builder().url(LOCALHOST + NodePort.GATHER_PORT + "/finish").build();
            Response response = Utils.callHttp(request);
            response.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
