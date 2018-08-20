package com.james.nettylib.netty;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * 获取当前网络状况
 * Created by james on 2018/7/20.
 */
public class NetworkHelper {
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        NetworkInfo netWorkInfo = manager.getActiveNetworkInfo();
        if (netWorkInfo == null) {
            return false;
        }
        return netWorkInfo.isAvailable();
    }

}
