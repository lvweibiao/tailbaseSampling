package com.rul.tianchi.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 与汇总节点通信接口
 *
 * @author RuL
 */
@RestController
public class FilterDataController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterDataController.class);

    @RequestMapping("/getBadTrace")
    public String getBadTrace(@RequestParam String badTraceIdJson, @RequestParam Integer cachePos) throws InterruptedException {
        HashSet<String> badTraceIds = JSON.parseObject(badTraceIdJson, new TypeReference<HashSet<String>>() {
        });
        if (badTraceIds == null) {
            return null;
        }
        int index = cachePos % FilterData.CACHE_SIZE;
        int prevIndex = (index - 1 + FilterData.CACHE_SIZE) % FilterData.CACHE_SIZE;
        int nextIndex = (index + 1) % FilterData.CACHE_SIZE;

        HashMap<String, ArrayList<String>> badTraceMap = new HashMap<>();

        LOGGER.info("######Daren Testing: prePrevIndex = " + prevIndex + "#index = " + index +  "#nextIndex = " + nextIndex);

//        final ExecutorService exec = Executors.newFixedThreadPool(2); //Add by Daren
//        Runnable runPrev = new Runnable() {
//            @Override
//            public void run() {
//                PullData.getBadTrace(prevIndex, badTraceIds, badTraceMap,true);
//            }
//        };
//        exec.submit(runPrev);
//
//        Runnable run = new Runnable() {
//            @Override
//            public void run() {
//                PullData.getBadTrace(prevIndex, badTraceIds, badTraceMap,false);
//            }
//        };
//        exec.submit(run);

//        PullData.getBadTrace(prevIndex, badTraceIds, badTraceMap);
//        PullData.getBadTrace(index, badTraceIds, badTraceMap);
//        PullData.getBadTrace(nextIndex, badTraceIds, badTraceMap);

        int[] arr = {prevIndex, index, nextIndex};
        final CountDownLatch endGate = new CountDownLatch(arr.length);
        for (int i = 0; i < arr.length; i++) {
            GetBadTraceRunnable getBadTraceRunnable = new GetBadTraceRunnable(arr[i], badTraceIds, badTraceMap);
            Thread t = new Thread() {
                @Override
                public void run () {
                    try {
                        getBadTraceRunnable.run();
                    } finally {
                        endGate.countDown();
                    }
                }
            };
            t.start();
        }
        endGate.await();

        //前一个缓冲区中的数据已经超时
        HashMap<String, ArrayList<String>> prevCache = FilterData.TRACE_CACHE.get(prevIndex);
        prevCache.clear();
        return JSON.toJSONString(badTraceMap);
    }

    class GetBadTraceRunnable implements Runnable {

        private int index;
        private HashSet<String> badTraceIds;
        private HashMap<String, ArrayList<String>> result;

        GetBadTraceRunnable(int index, HashSet<String> badTraceIds, HashMap<String, ArrayList<String>> result) {
            this.index = index;
            this.badTraceIds = badTraceIds;
            this.result = result;
        }

        @Override
        public void run() {
            PullData.getBadTrace(index, badTraceIds, result);
        }
    }
}
