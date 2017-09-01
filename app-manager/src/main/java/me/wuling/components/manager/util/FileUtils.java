package me.wuling.components.manager.util;

import java.io.File;

/**
 * Created by zhoucheng on 2017/8/4.
 */

public class FileUtils {
    public static boolean deleteDirectory(File directory) {
        if (!directory.exists()) {
            return true;
        }
        if (directory.isFile()) {
            return directory.delete();
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            deleteDirectory(file);
        }
        return directory.delete();
    }
}
