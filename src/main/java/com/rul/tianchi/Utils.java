package com.rul.tianchi;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


import java.io.IOException;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

/**
 * 工具类
 *
 * @author RuL
 */
public class Utils {

    private final static OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(50L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .build();

    public static Response callHttp(Request request) throws IOException {
        Call call = OK_HTTP_CLIENT.newCall(request);
        return call.execute();
    }

    /**
     * 生成MD5
     *
     * @param key key
     * @return 由key生成的MD5
     */
    public static String MD5(String key) {
        char[] hexDigits = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        try {
            byte[] btInput = key.getBytes();
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char[] str = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析每行数据的traceId和tags
     *
     * @param line 待解析的字符串
     * @return [0]:traceId  [1]:tags
     */
    public static String[] parseTraceIdAndTags(String line) {
        String[] traceIdAndTags = new String[2];
        char[] chars = line.toCharArray();
        int index = 0;
        int len = chars.length;
        for(int i=0;i<len;i++){
            if(chars[i]=='|'){
                index = i;
                break;
            }
        }

        //traceId
        traceIdAndTags[0] = new String(chars, 0, index);

        int flagCount = 0;
        index = 0;
        for (int i = 0; i < len; i++) {
            if (chars[i] == '|') {
                flagCount++;
            }
            if (flagCount == 8) {
                index = i + 1;
                break;
            }
        }
        //tags
        traceIdAndTags[1] = new String(chars, index, len - index);

        return traceIdAndTags;
    }

    /**
     * 解析数据startTime
     */
    public static long parseStartTime(String line) {
        char[] chars = line.toCharArray();
        int len = chars.length;
        int index1 = 0;
        int index2 = 0;
        int flagCount = 0;
        for (int i = 0; i < len; i++) {
            if (chars[i] == '|') {
                flagCount++;
                if (flagCount == 1) {
                    index1 = i + 1;
                }
                if (flagCount == 2) {
                    index2 = i;
                    break;
                }
            }
        }
        return Long.parseLong(line.substring(index1,index2));
    }

}
