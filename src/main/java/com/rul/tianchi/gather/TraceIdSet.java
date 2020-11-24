package com.rul.tianchi.gather;

import java.util.HashSet;

/**
 * badTraceId集合
 *
 * @author RuL
 */
public class TraceIdSet {

    private int cachePos = 0;
    private int finishNum = 0;
    private HashSet<String> traceIds;

    public int getCachePos() {
        return cachePos;
    }

    public void setCachePos(int dataPos) {
        this.cachePos = dataPos;
    }

    public int getFinishNum() {
        return finishNum;
    }

    public HashSet<String> getTraceIds() {
        return traceIds;
    }

    public void setTraceIds(HashSet<String> traceIds) {
        this.traceIds = traceIds;
    }

    public void increaseFinishNum() {
        this.finishNum++;
    }
}
