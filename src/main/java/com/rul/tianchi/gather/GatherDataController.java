package com.rul.tianchi.gather;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 与过滤节点通信接口
 *
 * @author RuL
 */
@RestController
public class GatherDataController {
    ExecutorService threadPool = Executors.newFixedThreadPool(2);

    @RequestMapping("/setBadTraceId")
    public String setFinishedTraceId(@RequestParam String badTraceIdJson, @RequestParam Integer cachePos) {
        HashSet<String> badTraceIds = JSON.parseObject(badTraceIdJson, new TypeReference<HashSet<String>>() {
        });
        int index = cachePos % GatherData.CACHE_SIZE;
        TraceIdSet traceIdSet = GatherData.TRACE_ID_CACHE.get(index);

        if (traceIdSet.getTraceIds() == null) {
            //还没有初始化
            traceIdSet.setCachePos(cachePos);
            traceIdSet.setTraceIds(badTraceIds);
        } else {
            //已经初始化
            traceIdSet.getTraceIds().addAll(badTraceIds);
        }
        traceIdSet.increaseFinishNum();

        //两个过滤节点均完成这批数据的处理
        if (traceIdSet.getFinishNum() == 2) {
            threadPool.execute(() -> {
                int prevIndex = (traceIdSet.getCachePos() - 1 + GatherData.CACHE_SIZE) % GatherData.CACHE_SIZE;
                TraceIdSet prevTraceIds = GatherData.TRACE_ID_CACHE.get(prevIndex);
                //请求前一批数据
                MergeData.getBadTrace(prevTraceIds.getTraceIds(), prevTraceIds.getCachePos());
                //生成新对象放在原位置
                GatherData.TRACE_ID_CACHE.set(prevIndex, new TraceIdSet());
            });
        }
        return "success";
    }

    @RequestMapping("/finish")
    public String finished() {
        GatherData.FINISHED_FILTER++;
        //两个过滤节点均拉取数据完成
        if (GatherData.FINISHED_FILTER == 2) {
            threadPool.execute(MergeData::finish);
        }
        return "success";
    }
}
