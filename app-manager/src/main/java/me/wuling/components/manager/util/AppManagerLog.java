package me.wuling.components.manager.util;

import android.util.Log;

/**
 * Created by zhoucheng on 2017/8/4.
 */

public class AppManagerLog {
    private static final String TAG = "app-manager";
    public static void d(String msg){
        Log.d(TAG, msg);
    }
    public static void d(String msg, Throwable e){
        Log.d(TAG, msg, e);
    }
    public static void i(String msg){
        Log.i(TAG, msg);
    }
    public static void i(String msg, Throwable e){
        Log.i(TAG, msg, e);
    }
    public static void w(String msg){
        Log.w(TAG, msg);
    }
    public static void w(String msg, Throwable e){
        Log.w(TAG, msg, e);
    }
    public static void e(String msg){
        Log.e(TAG, msg);
    }
    public static void e(String msg, Throwable e){
        Log.e(TAG, msg, e);
    }
}
