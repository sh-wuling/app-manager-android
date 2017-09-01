package me.wuling.components.manager.handler;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import me.wuling.components.manager.util.AppManagerLog;

/**
 * AppManagerConfig
 * Created by zhoucheng on 2017/8/4.
 */

public class AppManagerConfig {
    private String appkey;
    private String appSecret;
    private String tenantCode;
    private boolean debugger;

    public void setAppkey(String appkey) {
        this.appkey = appkey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getAppkey() {
        return appkey;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getTenantCode() {
        if (tenantCode == null || "".equals(tenantCode.trim())) {
            tenantCode = "00000";
        }
        return tenantCode;
    }

    public void setDebugger(boolean debugger) {
        this.debugger = debugger;
    }

    public boolean isDebugger() {
        return debugger;
    }


    public String getServiceURL() {
        if (isDebugger()) {
            return "http://app.stage.wulingd.com";
        } else {
            return "http://app.wulingd.com";
        }
    }

    public String getVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            AppManagerLog.e(e.getMessage(), e);
        }
        return "";
    }

    public int getVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            AppManagerLog.e(e.getMessage(), e);
        }
        return 0;
    }
}
