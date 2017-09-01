package me.wuling.components.manager.handler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.wuling.components.manager.AppManager;
import me.wuling.components.manager.model.AppManagerFileModel;
import me.wuling.components.manager.model.AppManagerUpgradeModel;
import me.wuling.components.manager.receiver.ApkInstallTools;
import me.wuling.components.manager.util.AppManagerLog;
import me.wuling.components.manager.util.AppManagerRequest;
import me.wuling.components.manager.util.FileUtils;
import me.wuling.components.manager.util.Zip;

/**
 * AppManagerImpl
 * Created by zhoucheng on 2017/7/18.
 */

public class AppManagerImpl extends AppManager {
    private static final String APP_MANAGER_LOCAL_FILE_VERSION_KEY = "APP_MANAGER_LOCAL_FILE_VERSION_KEY";
    private static final String APP_MANAGER_REMOTE_CONFIGS_KEY = "APP_MANAGER_REMOTE_CONFIGS_KEY";
    private static final String APP_MANAGER_DOWNLOADED = "app.manager.models.downloaded";
    private boolean ignoreUpdate = false;

    /**
     * 检查App更新
     * @param activity
     */
    public void checkAppUpgrade(final Activity activity) {
        if (ignoreUpdate) {
            return;
        }
        try {
            Map<String, String> params = new HashMap<String, String>();
            params.put("appkey", getConfig().getAppkey());
            params.put("version", getConfig().getVersionCode(activity) + "");
            params.put("tenantCode", getConfig().getTenantCode());
            params.put("platform", "Android");
            AppManagerRequest.getInstance().get(getConfig().getServiceURL() + "/api/v1/upgrade/check.shtml", params, new AppManagerRequest.AppManagerRequestCallback() {
                @Override
                public void result(AppManagerRequest.AppManagerResponseModel model) {
                    if (model.isSuccess()) {
                        final AppManagerUpgradeModel upgradeModel = JSONObject.parseObject(model.getData(), AppManagerUpgradeModel.class);
                        if (upgradeModel.needUpdate) {
                            AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
                            alertDialog.setTitle("检测到新版本");
                            alertDialog.setMessage(upgradeModel.upgradeDescription);
                            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "立即更新", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    AppManagerLog.d("立即更新");
                                    ApkInstallTools.startDownload(getContext(), upgradeModel.upgradeURL);

                                }
                            });

                            if (!upgradeModel.mandatoryUpgrade) {
                                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "忽略", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        AppManagerLog.d("忽略更新");
                                        ignoreUpdate = true;
                                        dialog.dismiss();
                                    }
                                });
                            }
                            alertDialog.show();
                        }
                    }
                }
            });
        } catch (Exception e) {
            AppManagerLog.e(e.getMessage(), e);
        }
    }

    @Override
    public void updateLocalModelFile() {
        final AppManagerConfig config = getConfig();
        String serverURL = MessageFormat.format("{0}/api/v1/file/{1}/{2}.shtml", config.getServiceURL(), config.getAppkey(), config.getVersionName(getContext()));
        AppManagerRequest.getInstance().get(serverURL, null, new AppManagerRequest.AppManagerRequestCallback() {
            @Override
            public void result(AppManagerRequest.AppManagerResponseModel modal) {
                if (modal.isSuccess()) {
                    List<AppManagerFileModel> remoteFiles = JSONArray.parseArray(modal.getData(), AppManagerFileModel.class);
                    List<AppManagerFileModel> updateFiles = diffFileVersions(remoteFiles);
                    downloadFiles(updateFiles);
                }
            }
        });
    }

    @Override
    public JSONObject getRemoteConfigs() {
        AppManagerConfig config = getConfig();
        Map<String, String> params = new HashMap<>();
        params.put("appkey", config.getAppkey());
        params.put("version", config.getVersionName(getContext()));
        String requsetUrl = MessageFormat.format("{0}/api/v1/config.shtml", config.getServiceURL());
        AppManagerRequest.getInstance().get(requsetUrl, params, new AppManagerRequest.AppManagerRequestCallback() {
            @Override
            public void result(AppManagerRequest.AppManagerResponseModel modal) {
                if (modal.isSuccess()) {
                    SharedPreferences.Editor sp = getSharedPreferences(getContext()).edit();
                    sp.putString(APP_MANAGER_REMOTE_CONFIGS_KEY, modal.getData());
                    sp.apply();
                }
            }
        });
        String remoteConfigsString = getSharedPreferences(getContext()).getString(APP_MANAGER_REMOTE_CONFIGS_KEY, "");
        if ("".equals(remoteConfigsString)) {
            return new JSONObject();
        }
        return JSONObject.parseObject(remoteConfigsString);
    }

    private void downloadFiles(List<AppManagerFileModel> updateFiles) {
        for (final AppManagerFileModel model: updateFiles) {
            AppManagerLog.d("开始下载文件模块: " + model.getFileName() + model.getFileURL());
            AppManagerRequest.getInstance().downloadFile(model.getFileURL(), new AppManagerRequest.AppManagerDownloadCallback() {
                @Override
                public void result(File file) {
                    String unzipDirectory = getModelUnzipDirectory(model.getFileName(), false);
                    AppManagerLog.d("清空旧的文件目录: " + unzipDirectory);
                    FileUtils.deleteDirectory(new File(unzipDirectory));
                    boolean bool = Zip.unZipFolder(file.getAbsolutePath(), unzipDirectory);
                    if (bool) {
                        saveLocalFileVersion(model);
                        AppManagerLog.d("解压成功: " + unzipDirectory);
                        Intent downloadedCompletionIntent = new Intent();
                        downloadedCompletionIntent.setAction(APP_MANAGER_DOWNLOADED);
                        getContext().sendBroadcast(downloadedCompletionIntent);
                        AppManagerLog.d("发送广播: " + APP_MANAGER_DOWNLOADED);
                    } else {
                        AppManagerLog.e("解压失败: " + unzipDirectory);
                    }
                    file.deleteOnExit();
                }
            });
        }
    }

    private synchronized void saveLocalFileVersion(AppManagerFileModel model) {
        SharedPreferences sp = getSharedPreferences(getContext());
        List<AppManagerFileModel> oldModels = JSONArray.parseArray(sp.getString(APP_MANAGER_LOCAL_FILE_VERSION_KEY, "[]"), AppManagerFileModel.class);
        List<AppManagerFileModel> newModels = new ArrayList<>();
        newModels.add(model);
        for (AppManagerFileModel old : oldModels) {
            if (!old.getFileName().equals(model.getFileName())) {
                newModels.add(old);
            }
        }
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(APP_MANAGER_LOCAL_FILE_VERSION_KEY, JSONObject.toJSONString(newModels));
        editor.apply();
        editor.commit();
    }

    private List<AppManagerFileModel> diffFileVersions(List<AppManagerFileModel> remoteFiles) {
        List<AppManagerFileModel> updateFiles = new ArrayList<>();
        SharedPreferences sp = getSharedPreferences(getContext());
        List<AppManagerFileModel> localFiles = JSONObject.parseArray(sp.getString(APP_MANAGER_LOCAL_FILE_VERSION_KEY, "[]"), AppManagerFileModel.class);

        for (AppManagerFileModel remote : remoteFiles) {
            boolean needUpdate = true;
            for (AppManagerFileModel local : localFiles) {
                if (remote.getFileName().equals(local.getFileName())) {
                    if (remote.getVersionCode().equals(local.getVersionCode())) {
                        needUpdate = false;
                        break;
                    }
                }
            }
            if (needUpdate) {
                updateFiles.add(remote);
            }
        }
        AppManagerLog.d("需要更新的模块数量: " + updateFiles.size());
        for (AppManagerFileModel model : updateFiles) {
            AppManagerLog.d("模块: " + model.getFileName());
        }
        return updateFiles;
    }

    public String getModelUnzipDirectory(String modelName, boolean unzip) {
        String modelUnzipDirectory = getFileUnzipDirectory() + "/" + modelName + "Model";
        File directory = new File(modelUnzipDirectory);
        if (!directory.exists()) {
            boolean mkdirs = directory.mkdirs();
            AppManagerLog.d("创建文件夹: " + directory.getAbsolutePath() + " :" + mkdirs);
            if (unzip) {
                try {
                    boolean unZipFolder = Zip.unZipFolder(getContext().getAssets().open("weex.zip"), modelUnzipDirectory);
                    AppManagerLog.d("解压asset文件 " + modelUnzipDirectory);
                } catch (IOException e) {
                    AppManagerLog.e("读取asset文件失败 ", e);
                }

            }
        }
        return modelUnzipDirectory;
    }
    public String getFileUnzipDirectory(){
        String unzipDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getConfig().getVersionName(getContext()) + "/modelFiles";
        File directory = new File(unzipDirectory);
        if (!directory.exists()) {
            boolean mkdirs = directory.mkdirs();
            AppManagerLog.d("创建文件夹: " + directory.getAbsolutePath() + " :" + mkdirs);
        }
        return unzipDirectory;
    }
}
