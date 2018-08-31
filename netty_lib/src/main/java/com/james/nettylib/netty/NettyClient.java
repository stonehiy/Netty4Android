package com.james.nettylib.netty;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.james.nettylib.constant.NetworkConfig;
import com.james.nettylib.netty.util.LogUtils;

import java.util.concurrent.atomic.AtomicInteger;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Created by james on 2018/6/20.
 */

public class NettyClient {

    public static final String TAG = NettyClient.class.getSimpleName();
    private static NettyClient mNettyClient = new NettyClient();
    private long mReconnectIntervalTime = NetworkConfig.RECONNECT_INTERVAL_TIME;
    private AtomicInteger mAtomicInteger = new AtomicInteger(1);
    private int mReconnectNum = 10;
    private EventLoopGroup group;
    private NettyListener listener;
    private Channel channel;
    private Bootstrap mBootstrap;
    private Context mContext;
    /**
     * 已经连接
     */
    private boolean isConnect = false;
    /**
     * 是否正在重连
     */
    private boolean isReconnecting = false;

    /**
     * 是否尝试重连
     */
    private boolean isDoReconnect = true;
    /**
     * 是否正在连接
     */
    private boolean isConnecting = false;
    /**
     * 网关index
     */
    private int mGateIndex = 0;


    /**
     * 是否已经发送密钥
     */
    private boolean sendAesKeyFinish;

    private NettyClient() {

    }

    public static NettyClient getInstance() {
        return mNettyClient;
    }

    public void setContext(Context ctx) {
        mContext = ctx;
    }

    public Context getContext() {
        return mContext;
    }

    public synchronized NettyClient connect() {
        isDoReconnect = true;
        if (isConnecting) {
            return this;
        }
        if (!isConnect) {
            sendAesKeyFinish = false;
            isConnecting = true;
            String gate = getGate();
            String host = gate.split(":")[0];
            int port = Integer.parseInt(gate.split(":")[1]);
            group = new NioEventLoopGroup();
            mBootstrap = new Bootstrap().group(group)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, NetworkConfig.CONNECTION_TIMEOUT)
                    .channel(NioSocketChannel.class)
                    .handler(new NettyClientInitializer(listener));
            try {
                ChannelFuture future = mBootstrap.connect(host, port).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        isConnecting = false;
                        if (future != null && future.isSuccess()) {
                            channel = future.channel();
                            isConnect = true;
                            isReconnecting = false;

                        } else {
                            isConnect = false;
                            isReconnecting = false;
                            reconnect();
                        }
                    }
                });


            } catch (Exception e) {
                e.printStackTrace();
                listener.onServiceStatusConnectChanged(NettyListener.STATUS_CONNECT_ERROR);
                isConnecting = false;
                isReconnecting = false;
                reconnect();
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
        if (mGateIndex < getGateList().length) {
            return getGateList()[mGateIndex];
        } else {
            return getGateList()[0];
        }
    }

    /**
     * 切换网关地址
     *
     * @return
     */
    private void swapGatePath() {
        mGateIndex = mGateIndex + 1;
        if (mGateIndex >= getGateList().length) {
            mGateIndex = 1;
        }

    }

    /**
     * 断开连接|不自动重连了
     */
    public void shutDown() {
        if (isConnect) {
            setDoReconnect(false);
            disconnect();
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        LogUtils.logError(TAG, "调用disconnect主动关闭连接");
        if (group != null) {
            group.shutdownGracefully();
            sendAesKeyFinish = false;
            isReconnecting = false;
            isConnecting = false;
            isConnect = false;
        }
    }

    /**
     * 重连
     *
     * @return
     */
    public synchronized void reconnect() {
        if (!isDoReconnect) {
            return;
        }
        // 有网络且没有重连则进行重连
        if (NetworkHelper.isNetworkAvailable(mContext) && !isReconnecting) {
            if (!isConnect && mReconnectNum > 0) {
                try {
                    Thread.sleep(mReconnectIntervalTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                swapGatePath();
                disconnect();
                connect();
                isReconnecting = true;
                listener.onServiceStatusConnectChanged(NettyListener.STATUS_CONNECT_RECONNECT);
                mReconnectNum--;
            } else {
                mReconnectNum = 10;
                disconnect();
            }
        }
    }

    private Channel getChannel() {
        return channel;
    }


    public int getSN() {
        return mAtomicInteger.incrementAndGet();
    }


    public void sendMessage(Request request) {
        sendMessage(request, null);
    }

    /**
     * 发送消息
     *
     * @param request
     * @param futureListener
     */
    public void sendMessage(final Request request, FutureListener futureListener) {
        // 判断网络状况，如果有网络并且没有进行重连，则尝试连接
        boolean flag = channel != null && isConnect;
        if (!flag) {
            request.getCallback().onFail(NettyListener.STATUS_CONNECT_CLOSED);
            //Toast.makeText(getContext(),"请检查您的网络连接",Toast.LENGTH_SHORT).show();
            if (NetworkHelper.isNetworkAvailable(mContext) && !isReconnecting) {
                connect();
            }
            return;
        }
        // 请求是否重复发送
        if (RequestManager.getInstance().isRepeatRequest(request)) {
            return;
        }
        // 管理请求和响应
        RequestManager.getInstance().add(request);
        ResponseManager.getInstance().addListener(request);
        // 发送请求
        if (futureListener == null) {
            channel.writeAndFlush(request).addListener(new FutureListener() {
                @Override
                public void success() {
                    LogUtils.logError(TAG, "发送成功--->Method====" + request.getServiceName());
                }

                @Override
                public void error() {
                    LogUtils.logError(TAG, "发送失败--->");
                    // 这里响应后后不移除回调，重连后会重新请求
                    request.getCallback().onFail(NettyListener.STATUS_CONNECT_ERROR);
                    reconnect();
                }
            });
        } else {
            channel.writeAndFlush(request).addListener(futureListener);
        }
    }

    /**
     * 设置重连次数
     *
     * @param reconnectNum 重连次数
     */
    public void setReconnectNum(int reconnectNum) {
        this.mReconnectNum = reconnectNum;
    }

    /**
     * 设置重连时间间隔
     *
     * @param reconnectIntervalTime 时间间隔
     */
    public void setReconnectIntervalTime(long reconnectIntervalTime) {
        this.mReconnectIntervalTime = reconnectIntervalTime;
    }

    public boolean getConnectStatus() {
        return isConnect;
    }

    /**
     * 设置连接状态
     *
     * @param status
     */
    public void setConnectStatus(boolean status) {
        this.isConnect = status;
    }

    public void setListener(NettyListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener == null ");
        }
        this.listener = listener;
    }

    /**
     * 发送秘钥
     *
     * @param listener
     */
    public void sendAESKey(final ResponseListener listener) {
        NettyClient.getInstance().sendMessage(new Request("syncAesKey", null, new ResponseListener() {
            @Override
            public void onSuccess(String data) {
                sendAesKeyFinish = true;
                listener.onSuccess(data);
                LogUtils.logError(TAG, "syncAesKey请求响应成功");
                // 请求重发
                RequestManager.getInstance().resend();
            }

            @Override
            public void onFail(int errcode) {
                sendAesKeyFinish = false;
                listener.onFail(errcode);
            }
        }));
    }

    /**
     * 是否发送密钥完成
     *
     * @return
     */
    public boolean isSendAesKeyFinish() {
        return sendAesKeyFinish;
    }

    /**
     * 是否重连
     *
     * @param doReconnect
     */
    public void setDoReconnect(boolean doReconnect) {
        isDoReconnect = doReconnect;
    }

}
