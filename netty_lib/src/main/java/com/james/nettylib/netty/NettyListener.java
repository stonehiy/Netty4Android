package com.james.nettylib.netty;


import java.util.Map;

/**
 * Created by james on 2018/6/20.
 */

public interface NettyListener {

    byte STATUS_CONNECT_SUCCESS = 1;

    byte STATUS_CONNECT_CLOSED = 2;

    byte STATUS_CONNECT_RECONNECT = 3;

    byte STATUS_CONNECT_ERROR = 0;


    /**
     * 对消息的处理
     */
    void onMessageResponse(Map messageHolder);

    /**
     * 当服务状态发生变化时触发
     */
    void onServiceStatusConnectChanged(int statusCode);
}
