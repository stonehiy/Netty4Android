package com.stonehiy.server.netty.constant;

/**
 * Created by james on 2018/6/20.
 */

public class NetworkConfig {


    /**
     * RSA公钥
     */
    public static String RSA_PUBLIC_KEY = "OjET9o7KBOCbhPmCQQjCEo5DKEuBJCxNpNFj6uuBDIZwGPvqCjEA8rQlLNFxafFgTL/PtR/d3MRQujaqJu+Q1Z0aWrhubv+LLZIlh37NmAd0OMR4q2wIDAQAB";




    /**
     * 文件上传服务器
     */
    public static String UPLOAD_HOST = "fileupload.xx.com";
    public static int UPLOAD_PORT = 87;
    public static String UPLOAD_HOST_TEST = "fileupload.xx.com";
    public static int UPLOAD_PORT_TEST = 1507;

    /**
     * 连接超时时间
     */
    public static final int CONNECTION_TIMEOUT = 10000;
    /**
     * 心跳时间间隔
     */
    public static final int HEART_BEAT_TIME = 15;

    /**
     * 响应超时时间
     */
    public static final int READ_IDLE_TIME_SECOND = 30;
    /**
     * 重连时间间隔
     */
    public static final long RECONNECT_INTERVAL_TIME = 2000;

}
