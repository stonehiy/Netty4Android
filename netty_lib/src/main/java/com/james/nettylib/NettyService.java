package com.james.nettylib;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.james.nettylib.netty.ConnectionManager;
import com.james.nettylib.netty.GameMessageListener;
import com.james.nettylib.netty.MessageManager;
import com.james.nettylib.netty.NettyClient;
import com.james.nettylib.netty.NettyListener;
import com.james.nettylib.netty.NotifyMessageListener;
import com.james.nettylib.netty.RequestManager;
import com.james.nettylib.netty.ResponseListener;
import com.james.nettylib.netty.ResponseManager;
import com.james.nettylib.netty.util.LogUtils;
import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

/**
 * Create by james on 2018/6/23
 */
public class NettyService extends Service implements NettyListener {

    private NetworkReceiver receiver;
    public static final String TAG = "NettyClient";
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private static final int PUSHTYPE_REQUEST = 1;
    private static final int PUSHTYPE_GAME = 2;
    private static final int PUSHTYPE_NOTIFY = 3;

    public NettyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        receiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, filter);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NettyClient.getInstance().setListener(this);
        connect();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        NettyClient.getInstance().setReconnectNum(0);
        NettyClient.getInstance().disconnect();
    }

    private void connect() {
        if (!NettyClient.getInstance().getConnectStatus()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    NettyClient.getInstance().connect();//连接服务器
                }
            }).start();
        }
    }

    @Override
    public void onMessageResponse(Map messageHolder) {
        dispatchData(messageHolder);

    }

    private void dispatchData(final Map messageHolder) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                int pushType = Integer.parseInt((String) messageHolder.get("pushType"));
                switch (pushType) {
                    case PUSHTYPE_REQUEST:
                        int sn = Integer.parseInt((String) messageHolder.get("sn"));
                        RequestManager.getInstance().removeBySN(sn);
                        ResponseListener listener = ResponseManager.getInstance().getListener(sn);
                        if (listener != null) {
                            listener.onSuccess((String) messageHolder.get("respInfo"));
                            ResponseManager.getInstance().removeListener(sn);
                        }
                        break;
                    case PUSHTYPE_GAME:
                        LogUtils.log(TAG, "MsgType:" + pushType + ", MsgInfo:" + (String) messageHolder.get("respInfo"));
                        List<GameMessageListener> listeners = MessageManager.getInstance().getListeners();
                        for (GameMessageListener l : listeners) {
                            l.callback(new Gson().toJson(messageHolder,Map.class));
                        }
                        break;
                    case PUSHTYPE_NOTIFY:
                        List<NotifyMessageListener> notifyListeners = MessageManager.getInstance().getNotifyListeners();
                        for (NotifyMessageListener l : notifyListeners) {
                            l.callback(new Gson().toJson(messageHolder,Map.class));
                        }
                        break;
                }
            }
        });

    }

    @Override
    public void onServiceStatusConnectChanged(final int statusCode) {
        if (statusCode == NettyListener.STATUS_CONNECT_SUCCESS) {
            LogUtils.logError(TAG, "connect sucessful statusCode = " + statusCode + " 连接成功");
        } else {
            if (statusCode == NettyListener.STATUS_CONNECT_CLOSED) {
                LogUtils.logError(TAG, "connect fail statusCode = " + statusCode + " 服务器断开连接");
            } else if (statusCode == NettyListener.STATUS_CONNECT_RECONNECT) {
                LogUtils.logError(TAG, "connect fail statusCode = " + statusCode + " 尝试重新连接");
            }
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                    LogUtils.log(TAG, "this is main thread");
                }
                ConnectionManager.getInstance().dispatch(statusCode);
            }
        });

    }

    private static final int NETWORK_CONNECT_STATUS_CONNECTED = 1;
    private static final int NETWORK_CONNECT_STATUS_DISCONNECT = 2;
    private int networkConnectStatus = 0;

    public class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                NetworkInfo info = intent
                        .getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                if (info != null) {
                    //如果当前的网络连接成功并且网络连接可用
                    if (NetworkInfo.State.CONNECTED == info.getState() && info.isAvailable()) {
                        if (info.getType() == ConnectivityManager.TYPE_WIFI
                                || info.getType() == ConnectivityManager.TYPE_MOBILE) {
                            LogUtils.log(TAG, getConnectionType(info.getType()) + "连上");
                            if (networkConnectStatus == NETWORK_CONNECT_STATUS_DISCONNECT) {
                                LogUtils.log(TAG, "网络断开后重新连接上");
                                connect();
                            }
                            networkConnectStatus = NETWORK_CONNECT_STATUS_CONNECTED;
                        }
                    } else {
                        networkConnectStatus = NETWORK_CONNECT_STATUS_DISCONNECT;
                        LogUtils.log(TAG, getConnectionType(info.getType()) + "断开");
                    }
                }
            }
        }
    }

    private String getConnectionType(int type) {
        String connType = "";
        if (type == ConnectivityManager.TYPE_MOBILE) {
            connType = "移动数据";
        } else if (type == ConnectivityManager.TYPE_WIFI) {
            connType = "WIFI网络";
        }
        return connType;
    }
}
