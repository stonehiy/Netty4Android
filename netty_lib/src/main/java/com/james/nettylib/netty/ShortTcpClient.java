package com.james.nettylib.netty;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.james.nettylib.constant.NetworkConfig;
import com.james.nettylib.netty.codec.MessageEncryptDecoder;
import com.james.nettylib.netty.codec.MessageEncryptEncoder;
import com.james.nettylib.netty.util.LogUtils;

import java.util.HashMap;
import java.util.Map;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Created by james on 2018/6/20.
 * TCP短连接
 */

public class ShortTcpClient {

    public static final int REQUEST_ERROR_CONNECT_FAILED = 2;
    public static final int REQUEST_ERROR_SEND_FAILED = 3;
    public static final int REQUEST_ERROR_RECEIVE_TIMEOUT = 4;

    private static final int PUSHTYPE_REQUEST = 1;
    private static final int PUSHTYPE_GAME = 2;
    private static final int PUSHTYPE_NOTIFY = 3;

    private static final int CONNECTION_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 5000;
    private static final int WRITE_TIMEOUT = 5000;

    private EventLoopGroup group;
    private Channel channel;
    private boolean isConnect = false;

    public final static String TAG = ShortTcpClient.class.getName();
    private Bootstrap bootstrap;

    private int gateIndex = 0;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private static final String SEND_AESKEY_SERVICE = "syncAesKey";
    private Map<Integer,Request> mRequestMap = new HashMap<>();
    private Map<Integer,ResponseListener> mResponseListeners = new HashMap<>();


    private ShortTcpClient connect(final Request request) {
        if (!isConnect) {
            String gate = getGate();
            String host = gate.split(":")[0];
            int port = Integer.parseInt(gate.split(":")[1]);
            group = new NioEventLoopGroup();
            bootstrap = new Bootstrap().group(group)
                    .option(ChannelOption.SO_KEEPALIVE, false)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_TIMEOUT, READ_TIMEOUT)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECTION_TIMEOUT)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new LoggingHandler(LogLevel.INFO));
                            pipeline.addLast("IdleStateHandler", new IdleStateHandler(READ_TIMEOUT, WRITE_TIMEOUT, 0));
                            pipeline.addLast("StringDecoder", new MessageEncryptDecoder());//解码器 这里要与服务器保持一致
                            pipeline.addLast("StringEncoder", new MessageEncryptEncoder());
                            pipeline.addLast(new SimpleChannelInboundHandler<Map>() {

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    sendAesKey(request);
                                }

                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                }

                                @Override
                                protected void channelRead0(ChannelHandlerContext channelHandlerContext, Map map) throws Exception {
                                    onResponse(map);
                                }

                                @Override
                                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                    if (evt instanceof IdleStateEvent) {
                                        IdleStateEvent event = (IdleStateEvent) evt;
                                        if (event.state() == IdleState.WRITER_IDLE){
                                        }else if (event.state() == IdleState.READER_IDLE){
                                            request.getCallback().onFail(REQUEST_ERROR_RECEIVE_TIMEOUT);
                                        }
                                        disconnect();
                                    }
                                }
                            });
                        }
                    });
            try {
                ChannelFuture future = bootstrap.connect(host, port).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future != null && future.isSuccess()) {
                            channel = future.channel();
                            isConnect = true;
                        } else {
                            isConnect = false;
                            request.getCallback().onFail(REQUEST_ERROR_CONNECT_FAILED);
                        }
                    }
                });

            } catch (Exception e) {
                disconnect();
                request.getCallback().onFail(REQUEST_ERROR_CONNECT_FAILED);
            }
        }
        return this;
    }

    /**
     * 获取网关列表
     *
     * @return
     */
    private String[] getGateList() {
        if (NetworkConfig.DEV) {
            return NetworkConfig.TEST_GATE_LIST;
        } else {
            return NetworkConfig.GATE_LIST;
        }
    }

    /**
     * 获取网关地址
     *
     * @return
     */
    private String getGate() {
        if (gateIndex < getGateList().length) {
            return getGateList()[gateIndex];
        } else {
            return getGateList()[0];
        }
    }

    /**
     * 关闭连接
     */
    private void disconnect() {
        group.shutdownGracefully();
    }


    public void request(Request request) {
        connect(request);
    }

    private void onResponse(final Map messageHolder) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                int pushType = Integer.parseInt((String) messageHolder.get("pushType"));
                switch (pushType) {
                    case PUSHTYPE_REQUEST:
                        int sn = Integer.parseInt((String) messageHolder.get("sn"));
                        ResponseListener listener = mResponseListeners.get(sn);
                        if (listener != null) {
                            listener.onSuccess((String) messageHolder.get("respInfo"));
                        }
                        if(!mRequestMap.get(sn).getServiceName().equals(SEND_AESKEY_SERVICE)){
                            disconnect();
                        }
                        break;
                    case PUSHTYPE_GAME:
                        break;
                    case PUSHTYPE_NOTIFY:
                        break;
                }
            }
        });
    }

    /**
     * 发送消息
     *
     * @param request
     * @param futureListener
     */
    private void sendMessage(final Request request, FutureListener futureListener) {
        boolean flag = channel != null && isConnect;
        if (!flag) {
            LogUtils.logError(TAG, "------尚未连接");
            return;
        }
        mRequestMap.put(request.getSN(),request);
        mResponseListeners.put(request.getSN(),request.getCallback());
        if (futureListener == null) {
            channel.writeAndFlush(request).addListener(new FutureListener() {

                @Override
                public void success() {
                    LogUtils.logError(TAG, "发送成功--->");
                }

                @Override
                public void error() {
                    LogUtils.logError(TAG, "发送失败--->");
                    request.getCallback().onFail(REQUEST_ERROR_SEND_FAILED);
                }
            });
        } else {
            channel.writeAndFlush(request).addListener(futureListener);
        }
    }


    /**
     * 发送密钥
     * @param request
     */
    private void sendAesKey(final Request request){
        sendMessage(new Request(SEND_AESKEY_SERVICE, null, new ResponseListener() {
            @Override
            public void onSuccess(String data) {
                //发送密钥后再发业务请求
                sendMessage(request,null);
            }

            @Override
            public void onFail(int errCode) {
            }
        }),null);
    }
}
