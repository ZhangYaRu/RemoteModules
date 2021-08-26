package com.chasing.ifupgrade.constants;

public class UpgradeConstants {
    public static final String HTTP_VERSION_1 = "v1";
    public static final String GO1_ROOT_TYPE = "GO1";
    public static final String GO2_ROOT_TYPE = "GO2";

    // 根目录地址
//    public static final String ROOT_FOLDER_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/IF.Dory";
//    public static final String ROOT_FOLDER_PATH_DIVE = Environment.getExternalStorageDirectory().getAbsolutePath()+"/IF.Dive";


//    // 下载文件路径
//    public static final String DOWNLOAD_PATH = ROOT_FOLDER_PATH+"/Download/";
//    // 缓存路径
//    public static final String CACHE_DOWNLOAD_PATH = ROOT_FOLDER_PATH+"/Cache/";
//    // 美化图片缓存路径
//    public static final String BEAUTIFY_PIC_DOWNLOAD_PATH = ROOT_FOLDER_PATH+"/Cache/PicBeautifyCache/";
//    // 分享图片缓存路径
//    public static final String SHARE_PIC_DOWNLOAD_PATH = ROOT_FOLDER_PATH+"/Cache/ShareCache/";
//    // 升级下载文件路径
//    public static final String  UPGRADE_DOWNLOAD_PATH = ROOT_FOLDER_PATH+"/UpdateDownload/";
//
//    public static final String DOWNLOAD_PATH_DIVE = ROOT_FOLDER_PATH_DIVE+"/Download/";
//    // 缓存路径
//    public static final String CACHE_DOWNLOAD_PATH_DIVE = ROOT_FOLDER_PATH_DIVE+"/Cache/";
//    // 美化图片缓存路径
//    public static final String BEAUTIFY_PIC_DOWNLOAD_PATH_DIVE = ROOT_FOLDER_PATH_DIVE+"/Cache/PicBeautifyCache/";
//    // 分享图片缓存路径
//    public static final String SHARE_PIC_DOWNLOAD_PATH_DIVE = ROOT_FOLDER_PATH_DIVE+"/Cache/ShareCache/";
//    // 升级下载文件路径
//    public static final String  UPGRADE_DOWNLOAD_PATH_DIVE = ROOT_FOLDER_PATH_DIVE+"/UpdateDownload/";

    public static String AP_BASEURL = "http://192.168.1.1/";

    /**固件下载文件名称前缀*/
    String ROV_FIRMWARE_PREFIX = "firmware_rov_";
    String AP_FIRMWARE_PREFIX = "firmware_ap_";
    String HANDLE_FIRMWARE_PREFIX = "firmware_handle_";
    String ZIP = ".zip";
    String PX4 = ".px4";

    // rov  ap  升级参数
    public static final String DEV_ROV = "DEV_ROV";
    public static final String DEV_AP = "DEV_AP";
    public static final String DEV_F1_CAMERA = "DEV_F1_CAMERA";

    // 连接机器类型
    public static final int DEVICES_DORY = 100;
    public static final int DEVICES_F1 = 101;
}
