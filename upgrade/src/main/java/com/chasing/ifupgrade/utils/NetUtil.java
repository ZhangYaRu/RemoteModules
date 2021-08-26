package com.chasing.ifupgrade.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * 网络工具类
 *
 * @author olivia
 * @data 2019/4/23 17:01
 */
public class NetUtil {

    public static final String KEY = "UUi6d1UdA32jKdas0o/=P86a";

    /**
     * 判断是否有网络连接
     *
     * @param context
     * @return
     */
    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            @SuppressLint("MissingPermission") NetworkInfo mNetworkInfo = mConnectivityManager
                    .getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    /**
     * 判断WIFI网络是否可用
     *
     * @param context
     * @return
     */
    public static boolean isWifiConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            @SuppressLint("MissingPermission") NetworkInfo mWiFiNetworkInfo = mConnectivityManager
                    .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (mWiFiNetworkInfo != null) {
                return mWiFiNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    public static boolean isWiFiActive(Context inContext) {
        Context context = inContext.getApplicationContext();
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            @SuppressLint("MissingPermission") NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getTypeName().equals("WIFI") && info[i].isConnected()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 获取当前网络连接的类型信息
     *
     * @param context
     * @return
     */
    public static int getConnectedType(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            @SuppressLint("MissingPermission") NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null && mNetworkInfo.isAvailable()) {
                return mNetworkInfo.getType();
            }
        }
        return -1;
    }

    private static long lastTotalRxBytes = 0;
    private static long lastTimeStamp = 0;

    public static long getNetSpeed(Context context) {
        int uid = context.getApplicationContext().getApplicationInfo().uid;
        long nowTotalRxBytes = getTotalRxBytes(uid);//转为KB
        long nowTimeStamp = System.currentTimeMillis();
        long speed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 / (nowTimeStamp - lastTimeStamp));
        lastTimeStamp = nowTimeStamp;
        lastTotalRxBytes = nowTotalRxBytes;
//        return String.valueOf(speed) + " kb/s";

//        Log.e("getNetSpeed","下载速度  "+speed+" KB/s");

        return speed;
    }

    //getApplicationInfo().uid
    public static long getTotalRxBytes(int uid) {
        return TrafficStats.getUidRxBytes(uid) == TrafficStats.UNSUPPORTED ? 0 : (TrafficStats.getTotalRxBytes() / 1024);//转为KB
    }


    /**
     * 检测手机是否连接 机器WIFI网络
     * 通过WifiConfiguration列表比较判断
     * 不需要定位权限。
     * <p>
     * 可以在主线程执行
     */
    @SuppressLint("MissingPermission")
    public static synchronized boolean isConnectToGladiusWifiByConfigs(Context mContext) {
//        Log.e("isConnectToGlad","isConnectToGladiusWifiByConfigs+++++++");

        WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        @SuppressLint("MissingPermission") WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        ConnectivityManager connec = (ConnectivityManager) mContext.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if ((connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED)
                && wifiInfo != null) {

//            List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
            List<WifiConfiguration> configs;
            try {
                configs = wifiManager.getConfiguredNetworks();//某些机型偶尔报错：java.lang.RuntimeException:android.os.DeadSystemException
            } catch (RuntimeException e) {
                return false;
            } catch (Exception e) {
                return false;
            }


            if (configs != null) {
                for (WifiConfiguration config : configs) {
                    if (config.networkId == wifiInfo.getNetworkId()) {
//                        Log.e("WifiSSID","config.SSID = "+config.SSID+"  config.networkId = "+config.networkId);

                        if (config.SSID.contains("Gladius")) {
//                            Log.e("WifiSSID","Gladius");
                            return true;
                        }
                    }
//                    Log.e("WifiSSID","config.SSID = "+config.SSID+"  config.networkId = "+config.networkId);
                }
            }
        }
        return false;
    }

    /* @author suncat
     * @category 判断是否连接的wif是否是可用的
     * @return
     */
    public static boolean isAvailableWifi() {
        try {
            String ip = "www.baidu.com";// ping 的地址，可以换成任何一种可靠的外网
            Process p = Runtime.getRuntime().exec("ping -c 1 -w 100 " + ip);// ping网址3次
            // ping的状态
            int status = p.waitFor();
            if (status == 0) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "isAvailableWifi: " + e.getMessage());
        }
        return false;
    }

//    public static void isConnInternet(ConnectedInternetListener listener) {
//        Process p = null;
//        try {
//            p = Runtime.getRuntime().exec("ping -c 1 -w 1 " + GlobleConfig.BAIDU_IP);
//            int status = p.waitFor();
//            switch (status) {
//                case 0:// 成功
//                    if (listener != null) {
//                        listener.canConnect();
//                        return;
//                    }
//                    break;
//                default:// 失败
//                    break;
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        if (listener != null) {
//            listener.cannotConnect();
//        }
//    }

    public interface ConnectedInternetListener {
        void canConnect();

        void cannotConnect();
    }

//    /**
//     * 判断移动数据是否打开
//     *
//     * @return {@code true}: 是<br>{@code false}: 否
//     */
//    public static boolean getDataEnabledBakeup() {
//        try {
//            TelephonyManager tm = (TelephonyManager) App.getContext().getSystemService(Context.TELEPHONY_SERVICE);
//            Method getMobileDataEnabledMethod = tm.getClass().getDeclaredMethod("getDataEnabled");
//            if (null != getMobileDataEnabledMethod) {
//                return (boolean) getMobileDataEnabledMethod.invoke(tm);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;
//    }
//
    /**
     * 判断移动数据是否可用
     *
     * @return {@code true}: 是<br>{@code false}: 否
     */
    public static boolean getDataEnabled(Context mContext) {
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return tm.isDataEnabled();

        } else {
            return tm.getSimState() == TelephonyManager.SIM_STATE_READY && tm.getDataState() != TelephonyManager.DATA_DISCONNECTED;
        }
    }
    /**
     * 判断MOBILE网络是否可用
     * @param context
     * @return
     * @throws Exception
     */
    @SuppressLint("MissingPermission")
    public static boolean isMobileDataEnable(Context context){
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isMobileDataEnable = false;
        isMobileDataEnable = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting();
        return isMobileDataEnable;
    }
    /**
     * 判断是否包含SIM卡
     *
     * @return 状态
     */
    public static boolean hasSimCard(Context context) {
        TelephonyManager telMgr = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        boolean result = true;
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                result = false; // 没有SIM卡
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN:
                result = false;
                break;
        }
        Log.d("try", result ? "有SIM卡" : "无SIM卡");
        return result;
    }
}
