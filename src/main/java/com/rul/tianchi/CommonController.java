package com.rul.tianchi;

import com.rul.tianchi.filter.PullData;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评测接口
 *
 * @author RuL
 */
@RestController
public class CommonController {

    public static Integer DATA_SOURCE_PORT = 0;

    public static Integer getDataSourcePort() {
        return DATA_SOURCE_PORT;
    }

    /**
     * 状态接口
     *
     * @return success
     */
    @RequestMapping("/ready")
    public String ready() {
        return "success";
    }

    /**
     * 接收数据源端口的接口
     *
     * @param port 数据源端口
     * @return success
     */
    @RequestMapping("/setParameter")
    public String setParamter(@RequestParam Integer port) {
        //设置数据源端口
        CommonController.DATA_SOURCE_PORT = port;

        //当前启动为过滤节点
        if (NodePort.isFilter()) {
            new Thread(PullData::pullData, "filter thread").start();
        }
        return "success";
    }
}
