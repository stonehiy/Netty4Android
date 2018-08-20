package com.james.nettylib.netty;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理接收到的服务器的Push消息
 * Create by james on 2018/7/25
 */
public class MessageManager {
    public static MessageManager mInstance = new MessageManager();
    /**
     * 处理游戏消息
     */
    private List<GameMessageListener> mListeners = new ArrayList<>();

    /**
     * 处理通知消息
     */
    private List<NotifyMessageListener> mNotifyListeners = new ArrayList<>();

    private MessageManager() {
    }

    public static MessageManager getInstance() {
        return mInstance;
    }

    public void registerListener(GameMessageListener l) {
        mListeners.add(l);
    }

    public void registerListener(NotifyMessageListener l) {
        mNotifyListeners.add(l);
    }

    public void unregisterListener(GameMessageListener l) {
        mListeners.remove(l);
    }

    public void unregisterListener(NotifyMessageListener l) {
        mNotifyListeners.remove(l);
    }

    public List<GameMessageListener> getListeners() {
        return mListeners;
    }

    public List<NotifyMessageListener> getNotifyListeners() {
        return mNotifyListeners;
    }

}
