package me.wuling.components.manager.model;

/**
 * 资源文件版本
 * Created by zhoucheng on 2017/8/4.
 */

public class AppManagerFileModel {
    private String fileName;
    private String fileURL;
    private Integer versionCode;

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileURL(String fileURL) {
        this.fileURL = fileURL;
    }

    public void setVersionCode(Integer versionCode) {
        this.versionCode = versionCode;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileURL() {
        return fileURL;
    }

    public Integer getVersionCode() {
        return versionCode;
    }
}
