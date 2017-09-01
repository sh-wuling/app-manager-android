package me.wuling.components.manager;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.alibaba.fastjson.JSONObject;
import com.blankj.utilcode.util.Utils;

import me.wuling.components.manager.handler.AppManagerConfig;
import me.wuling.components.manager.handler.AppManagerImpl;

/**
 * AppManager
 * Created by zhoucheng on 2017/8/4.
 */

public abstract class AppManager {
    private Application context;
    private AppManagerConfig config;
    private static final AppManagerImpl instance = new AppManagerImpl();
    public static AppManager getInstance() {
        return instance;
    }
    public void init(Application context, AppManagerConfig config) {
        this.context = context;
        this.config = config;
        Utils.init(context);
    }
    public AppManagerConfig getConfig() {
        return config;
    }
    public Context getContext() {
        return context;
    }
    public abstract void checkAppUpgrade(Activity activity);
    public abstract void updateLocalModelFile();
    public abstract JSONObject getRemoteConfigs();
    public abstract String getModelUnzipDirectory(String modelName, boolean unzip);
    public abstract String getFileUnzipDirectory();
    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("app-manager", Context.MODE_PRIVATE);
    }
}
