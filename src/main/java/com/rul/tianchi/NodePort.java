package com.rul.tianchi;

/**
 * 节点端口
 *
 * @author RuL
 */
public class NodePort {
    public static final String FILTER_PORT1 = "8000";
    public static final String FILTER_PORT2 = "8001";
    public static final String GATHER_PORT = "8002";

    public static boolean isFilter() {
        String port = System.getProperty("server.port", "8080");
        return NodePort.FILTER_PORT1.equals(port) || NodePort.FILTER_PORT2.equals(port);
    }

    public static boolean isGather() {
        String port = System.getProperty("server.port", "8080");
        return NodePort.GATHER_PORT.equals(port);
    }
}
