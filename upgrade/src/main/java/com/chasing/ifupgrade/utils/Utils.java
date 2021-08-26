package com.chasing.ifupgrade.utils;

import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Utils {
    /**
     * 判断网址连接对应的文件与本地文件是否一致(需要在子线程执行)
     *
     * @param url
     * @param file
     * @return
     */
    public static boolean isSameSize(String url, File file) {
        try {
            URL u = new URL(url);
            HttpURLConnection urlcon = (HttpURLConnection) u.openConnection();
            int fileLength = urlcon.getContentLength();
            FileInputStream fis = new FileInputStream(file);
            int totalSpace = fis.available();
//            Logger.e("url file space:" + fileLength + ",local file space:" + totalSpace);
            if (totalSpace < fileLength) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public static int versionName2Code(String versionName) {
        if (TextUtils.isEmpty(versionName)) return 0;
        String replace = versionName.replace(".", "");
        try {
            int versionCode = Integer.parseInt(replace);
            return versionCode;
        } catch (Exception e) {
            return 0;
        }
    }
}
