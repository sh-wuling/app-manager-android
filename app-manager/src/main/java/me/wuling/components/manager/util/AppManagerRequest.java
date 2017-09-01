package me.wuling.components.manager.util;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.alibaba.fastjson.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import me.wuling.components.manager.handler.AppManagerImpl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * AppManagerRequest
 * Created by zhoucheng on 2017/7/18.
 */

public class AppManagerRequest {
    private static AppManagerRequest instance = new AppManagerRequest();
    public static AppManagerRequest getInstance(){
        return instance;
    }
    private Handler handler = new Handler(Looper.getMainLooper());
    private OkHttpClient client = new OkHttpClient();
    public void downloadFile(final String url, final AppManagerDownloadCallback callback){
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AppManagerLog.e(e.getMessage(), e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String remoteFileName = url.substring(url.lastIndexOf("/"));
                String prefix = remoteFileName.substring(0, remoteFileName.indexOf("."));
                String suffix = remoteFileName.substring(remoteFileName.indexOf("."));
                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + System.currentTimeMillis() + suffix);
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                BufferedInputStream bin = new BufferedInputStream(response.body().byteStream());
                byte[] buffer = new byte[2048];
                int read = -1;
                while ((read = bin.read(buffer)) != -1) {
                    bos.write(buffer, 0, read);
                }
                bos.flush();
                bos.close();
                bin.close();
                if (callback != null) {
                    callback.result(file);
                }
            }
        });

    }
    public void get(String url, Map<String, String> params, final AppManagerRequestCallback callback) {

        StringBuilder stringBuilder = new StringBuilder(url);
        if (stringBuilder.indexOf("?") == -1) {
            stringBuilder.append("?");
        }
        if (params != null) {
            for (String key: params.keySet()) {
                String value = params.get(key);
                stringBuilder.append("&");
                stringBuilder.append(key);
                stringBuilder.append("=");
                try {
                    stringBuilder.append(URLEncoder.encode(value,"utf-8"));
                } catch (UnsupportedEncodingException e) {
                    AppManagerLog.e(e.getMessage(), e);
                }
            }
        }

        AppManagerLog.d("request:" + stringBuilder);
        Request request = new Request.Builder().url(stringBuilder.toString()).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                final AppManagerResponseModel modal = new AppManagerResponseModel(e.getMessage());
                AppManagerLog.d(JSONObject.toJSONString(modal));
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.result(modal);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseString = response.body().string();
                String json = aesDecode(responseString, AppManagerImpl.getInstance().getConfig().getAppSecret());
                if (json != null) {
                    final AppManagerResponseModel modal = JSONObject.parseObject(json, AppManagerResponseModel.class);
                    AppManagerLog.d(JSONObject.toJSONString(modal));
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.result(modal);
                        }
                    });
                } else {
                    final AppManagerResponseModel modal = JSONObject.parseObject(responseString, AppManagerResponseModel.class);
                    AppManagerLog.d(JSONObject.toJSONString(modal));
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.result(modal);
                        }
                    });
                }
            }
        });
    }

    /**
     * aes解密
     */
    public static String aesDecode(String source, String key) {
        try {
            byte[] bs = base64Decode(source);
            key = getMD5(key).substring(0, 16);
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(cipher.doFinal(bs));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static byte[] base64Decode(String source) {
        return Base64.decode(source, Base64.DEFAULT);
    }
    /**
     * MD5加密
     */
    public static String getMD5(String val) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(val.getBytes());
        BigInteger bigInt = new BigInteger(1, md5.digest());
        return bigInt.toString(16);
    }

    public static interface AppManagerRequestCallback {
        void result(AppManagerResponseModel modal);
    }
    public static interface AppManagerDownloadCallback {
        void result(File file);
    }

    public static class AppManagerResponseModel {
        private String data;
        private boolean state;
        private String message;

        public AppManagerResponseModel(){

        }
        public AppManagerResponseModel(String message){
            this.message = message;
            this.state = false;
        }

        public void setData(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }

        public boolean isSuccess() {
            return state;
        }

        public void setState(boolean state) {
            this.state = state;
        }
        public void setMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
