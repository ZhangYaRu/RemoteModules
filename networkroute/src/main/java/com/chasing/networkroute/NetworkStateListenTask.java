package com.chasing.networkroute;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

public class NetworkStateListenTask extends Thread {

    private boolean isMobilConnect = false;
    private boolean isWifiConnect = false;
    private String wifiNetworkId = "";
    private boolean isEthnetConnect = false;

    private Context mContext;

    public NetworkStateListenTask(Context context) {
        mContext = context;
    }

    private NetworkChangeListener mNetworkChangeListener;

    public interface NetworkChangeListener {
        void onMobilChanged(Network network);

        void onWifiChanged(Network network);

        void onEthnetChanged(Network network);
    }

    public void setNetworkChangeListener(NetworkChangeListener networkChangeListener) {
        mNetworkChangeListener = networkChangeListener;
    }

    @Override
    public void run() {
        //获得ConnectivityManager对象
        if (RouteConfig.isDebug)
            Log.d("网络库测试", "NetworkStateListenTask      1111111111111");
        ConnectivityManager connMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr == null) return;
        if (RouteConfig.isDebug)
            Log.d("网络库测试", "NetworkStateListenTask      22222222222222");

        //获取所有网络连接的信息
        Network[] networks = connMgr.getAllNetworks();
        if (RouteConfig.isDebug)
            Log.d("网络库测试", "NetworkStateListenTask      3333333333==" + networks.length);

        //通过循环将网络信息逐个取出来
        boolean haveMobil = false;
        boolean haveWifi = false;
        boolean haveEthnet = false;

        Network mobilNetwork = null;
        Network wifiNetwork = null;
        Network ethNetwork = null;

        for (int i = 0; i < networks.length; i++) {
            //获取ConnectivityManager对象对应的NetworkInfo对象
            NetworkInfo networkInfo = connMgr.getNetworkInfo(networks[i]);
            if (networkInfo == null) {
                continue;
            }
            if (RouteConfig.isDebug)
                Log.d("网络库测试", "NetworkStateListenTask      44444444==" + networkInfo.getTypeName());

            if ("MOBILE".equals(networkInfo.getTypeName())) {
                haveMobil = networkInfo.isConnected();
                mobilNetwork = networks[i];
            } else if ("WIFI".equals(networkInfo.getTypeName())) {
                haveWifi = networkInfo.isConnected();
                wifiNetwork = networks[i];
            } else if ("Ethernet".equals(networkInfo.getTypeName())) {
                haveEthnet = networkInfo.isConnected();
                ethNetwork = networks[i];
            }
        }

        if (haveMobil != isMobilConnect) {
            isMobilConnect = haveMobil;
            if (RouteConfig.isDebug)
                Log.d("网络库测试", "NetworkStateListenTask      数据 网络变化");

            if (mNetworkChangeListener != null)
                mNetworkChangeListener.onMobilChanged(mobilNetwork);

        }

        if (haveWifi != isWifiConnect || (wifiNetwork != null && !TextUtils.isEmpty(wifiNetwork.toString()) && !wifiNetworkId.equals(wifiNetwork.toString()))) {
            if (RouteConfig.isDebug)
                Log.d("网络库测试", "NetworkStateListenTask      wifi 网络变化 ===sourceid:" + wifiNetworkId + "===thisid:" + wifiNetwork);
            isWifiConnect = haveWifi;
            if (wifiNetwork != null)
                wifiNetworkId = wifiNetwork.toString();

            if (mNetworkChangeListener != null)
                mNetworkChangeListener.onWifiChanged(wifiNetwork);
        }

        if (haveEthnet != isEthnetConnect) {
            isEthnetConnect = haveEthnet;
            if (RouteConfig.isDebug)
                Log.d("网络库测试", "NetworkStateListenTask      有线 网络变化");

            if (mNetworkChangeListener != null)
                mNetworkChangeListener.onEthnetChanged(ethNetwork);
        }
    }
}
