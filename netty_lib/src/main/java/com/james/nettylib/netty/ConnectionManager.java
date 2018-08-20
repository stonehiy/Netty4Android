package com.james.nettylib.netty;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by james on 2018/6/21
 */
public class ConnectionManager {
    public static ConnectionManager mInstance = new ConnectionManager();
    private  List<ConnectionListener> mConnectionListeners = new ArrayList<ConnectionListener>();

    private ConnectionManager(){

    }

    public interface ConnectionListener{
        public void onConnectionStatusChange(int status);
    }

    public static ConnectionManager getInstance(){
        return mInstance;
    }

    public void registerListener(ConnectionListener listener){
        mConnectionListeners.add(listener);
    }

    public void unregisterListener(ConnectionListener listener){
        mConnectionListeners.remove(listener);
    }

    public void dispatch(int status){
        for(ConnectionListener listener:mConnectionListeners){
            listener.onConnectionStatusChange(status);
        }
    }
}
