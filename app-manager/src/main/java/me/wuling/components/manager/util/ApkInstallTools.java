package me.wuling.components.manager.util;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.blankj.utilcode.util.AppUtils;

import java.io.File;

import me.wuling.components.manager.AppManager;
import me.wuling.components.manager.util.AppManagerLog;

public class ApkInstallTools {
    private static final String DOWNLOAD_PATH = "/download/";

    private static void installApk(Context context, File apkFile) {
        if (apkFile.exists()) {
            ;
            AppUtils.installApp(apkFile, context.getPackageName() + ".update.provider");
        } else {
            setDownloadId(context, 0);
        }
    }

    private static String getRealFilePath(Context context, Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    public static void startDownload(Context context, final String uri) {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + DOWNLOAD_PATH + context.getPackageName() + "-update.apk");
        AppManagerLog.i("download save path:" + file.getAbsolutePath());
        if (getDownloadApkFile(context, uri).exists()) {
            installApk(context, getDownloadApkFile(context, uri));
            return;
        }
        file.delete();
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(uri));

        req.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        //req.setAllowedOverRoaming(false);

        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        //设置文件的保存的位置[三种方式]
        //第一种
        //file:///storage/emulated/0/Android/data/your-package/files/Download/update.apk
//        req.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, uri.substring(uri.lastIndexOf("/")));
        //第二种
        //file:///storage/emulated/0/Download/update.apk
        req.setDestinationInExternalPublicDir(DOWNLOAD_PATH, file.getName());
        //第三种 自定义文件路径
        //req.setDestinationUri()


        // 设置一些基本显示信息
//        req.setTitle(title);
//        req.setDescription(description);
        req.setMimeType("application/vnd.android.package-archive");

        //加入下载队列
        final long taskId = dm.enqueue(req);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setDownloadId(context, taskId);
                AppManagerLog.d("onReceice" + intent.getAction());
                if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                    installApk(context, getDownloadApkFile(context, uri));
                }
            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private static File getDownloadApkFile(Context context, String url) {
        SharedPreferences sp = AppManager.getSharedPreferences(context);
        sp.edit().putString("download_task_url", url).apply();
        long downloadTaskId = sp.getLong("download_task_id_" + url, 0);
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        File file = new File("" + System.currentTimeMillis());
        Uri downloadFileUri = dm.getUriForDownloadedFile(downloadTaskId);
        if (downloadFileUri != null) {
            file = new File(getRealFilePath(context, downloadFileUri));
        } else {
            setDownloadId(context, 0);
        }
        return file;
    }

    private static void setDownloadId(Context context, long downloadId) {
        SharedPreferences sp = AppManager.getSharedPreferences(context);
        String url = sp.getString("download_task_url", "");
        sp.edit().putLong("download_task_id_" + url, downloadId).apply();

    }
}