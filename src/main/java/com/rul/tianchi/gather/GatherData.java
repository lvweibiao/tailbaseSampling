package com.rul.tianchi.gather;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 汇总节点数据封装
 *
 * @author RuL
 */
public class GatherData {

    public static final int CACHE_SIZE = 60;
    public static ArrayList<TraceIdSet> TRACE_ID_CACHE = new ArrayList<>(CACHE_SIZE);

    //初始化
    public static void initCache() {
        for (int i = 0; i < CACHE_SIZE; i++) {
            TRACE_ID_CACHE.add(new TraceIdSet());
        }
    }

    //过滤节点拉取数据全部完成的数量
    public static int FINISHED_FILTER = 0;

    //结果
    public static HashMap<String, String> CHECKSUM = new HashMap<>();
}
