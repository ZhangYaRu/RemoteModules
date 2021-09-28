package com.chasing.networkroute;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NetworkRouteManager {
    private static NetworkRouteManager instance;
    private NetworkStateListenTask networkStateListenTask;
    private static Context mContext;
    private Network mWifiNetwork;
    private Network mCellularNetwork;
    private Network mEthernetNetwork;
    private ArrayList<AngencyBean> activityTunnels = new ArrayList<>();
    private int portFlag = 10001;
    // TODO   这里没有ping通的时候需要清空变量,未找到合适位置
    private Network mActivityNetwork;
    private Network mPublicNetwork;

    private NetworkType mRovNetworkType = NetworkType.NONE;
    private NetworkType mPublicNetworkType = NetworkType.NONE;
    private Handler mSubHandler;

    public enum NetworkType {
        NONE,           // 无可用网络
        ETHERNET,       // 有线网络
        WIFI,           // wifi网络
        CELLULAR        // 数据流量
    }

    private ThreadPoolExecutor mThreadPool;
    private Main publicHttpsMain;


    private final int FIND_WIFI_NETWORK = 1;
    private final int FIND_CELLULAR_NETWORK = 2;
    //    private final int PREPARE = 3;
    // 用于主线程回调
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case FIND_WIFI_NETWORK:
                    findWifiNetwork();
                    break;
                case FIND_CELLULAR_NETWORK:
                    findCellularNetwork();
                    break;
//                case PREPARE:
//                    break;
            }
        }
    };

    private int checkNetworkFlag = 0;

    private NetworkRouteManager() {
        HandlerThread handlerThread = new HandlerThread("check-network");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                checkNetworkFlag++;
                sendEmptyMessageDelayed(1, 1000);
//                Log.d("网络库测试", "NetworkRouteManager     handleMessage     000000000000");

                if (networkStateListenTask == null && mContext != null) {
//                    if (RouteConfig.isDebug)
//                    Log.d("网络库测试", "NetworkRouteManager     handleMessage     111111111111");
                    networkStateListenTask = new NetworkStateListenTask(mContext);
                    networkStateListenTask.setNetworkChangeListener(networkChangeListener);
                }
                if (networkStateListenTask != null) {
//                        if (RouteConfig.isDebug)
//                            Log.d("网络库测试", "NetworkRouteManager     handleMessage     22222222222222");
                    post(networkStateListenTask);
                }
            }
        };
        handler.sendEmptyMessage(1);
        HandlerThread routeHandlerThread = new HandlerThread("network-route");
        routeHandlerThread.start();
        mSubHandler = new Handler(routeHandlerThread.getLooper());

        if (mThreadPool == null) {
            mThreadPool = new ThreadPoolExecutor(20, 20,
                    0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(20));
        }
    }

    NetworkStateListenTask.NetworkChangeListener networkChangeListener = new NetworkStateListenTask.NetworkChangeListener() {
        @Override
        public void onMobilChanged(Network network) {
            if (RouteConfig.isDebug)
                Log.d("网络库测试", "NetworkRouteManager      数据 网络变化");
            runTestPublicRunnable(network, NetworkType.CELLULAR);
        }

        @Override
        public void onWifiChanged(final Network network) {
            if (RouteConfig.isDebug)
                Log.d("网络库测试", "NetworkRouteManager      wifi 网络变化");
            destroy();
            mWifiNetwork = network;
            // 检查是否可以连接机器网络
            if (!droneRouteIsStart) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        runTestDroneRunnable(network, NetworkType.WIFI);
                        runTestPublicRunnable(network, NetworkType.WIFI);
                    }
                }, 1500);
            }
        }

        @Override
        public void onEthnetChanged(Network network) {
            if (RouteConfig.isDebug)
                Log.d("网络库测试", "NetworkRouteManager      有线 网络变化");
            destroy();
            mEthernetNetwork = network;
            // 检查是否可以连接机器网络
            runTestDroneRunnable(network, NetworkType.ETHERNET);
            runTestPublicRunnable(network, NetworkType.ETHERNET);
        }
    };

    public static NetworkRouteManager getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkRouteManager();
        }
        mContext = context;
        return instance;
    }

    public synchronized boolean prepare() {
        if (RouteConfig.isDebug)
            Log.d("重连测试", "NetworkRouteManager    prepare");
        findEthernetNetwork();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                findWifiNetwork();
            }
        }, 1500);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                findCellularNetwork();
            }
        }, 1500);
        return true;
    }

    Main[] mains = new Main[30];
    int mainsIndex = 0;

    public void destroyDockerDevicesAngency() {
        if (mains != null && mains.length > 0) {
            for (int i = 0; i < mains.length; i++) {
                if (mains[i] != null) {
                    mains[i].stop();
                    mThreadPool.remove(mains[i]);
                    mains[i] = null;
                }
            }
        }
    }

    public void destroy() {
        droneRouteIsStart = false;
        if (RouteConfig.isDebug)
            Log.d("重连测试", "NetworkRouteManager   destroy");
//      ============================https=====================
        if (publicHttpsMain != null) {
            publicHttpsMain.stop();
            mThreadPool.remove(publicHttpsMain);
            publicHttpsMain = null;
        }

        mWifiNetwork = null;
        mCellularNetwork = null;
        mEthernetNetwork = null;
    }

    public Network getWifiNetwork() {
        return mWifiNetwork;
    }

    public Network getCellularNetwork() {
        return mCellularNetwork;
    }

    public Network getEthernetNetwork() {
        return mEthernetNetwork;
    }

    private void findWifiNetwork() {
        // 指定某个网络请求采用指定的网络访问
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder req = new NetworkRequest.Builder();
        req.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (RouteConfig.isDebug)
                    Log.d("网络转发测试", "getWifiNetwork    onAvailable     network:" + network.toString());
                mWifiNetwork = network;
                if (mActivityNetwork == null)
                    mActivityNetwork = mWifiNetwork;
                // 检查是否可以连接机器网络
                if (!droneRouteIsStart)
                    runTestDroneRunnable(network, NetworkType.WIFI);
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                if (RouteConfig.isDebug)
                    Log.d("网络转发测试", "getWifiNetwork    onUnavailable");

            }
        };
        cm.requestNetwork(req.build(), callback);
    }

    private void findCellularNetwork() {
        // 指定某个网络请求采用指定的网络访问
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder req = new NetworkRequest.Builder();
        req.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (RouteConfig.isDebug)
                    Log.d("网络测试", "getCellularNetwork    onAvailable     network:" + network.toString());
                mCellularNetwork = network;
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                if (RouteConfig.isDebug)
                    Log.d("网络测试", "getCellularNetwork    onUnavailable");

            }
        };
        cm.requestNetwork(req.build(), callback);
    }

    private void findEthernetNetwork() {
        // 指定某个网络请求采用指定的网络访问
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder req = new NetworkRequest.Builder();
        req.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET);
        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (RouteConfig.isDebug)
                    Log.d("网络转发测试", "getEthernetNetwork    onAvailable     network:" + network.toString());
                mEthernetNetwork = network;
                // 检查是否可以连接机器网络
                runTestDroneRunnable(network, NetworkType.ETHERNET);
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                if (RouteConfig.isDebug)
                    Log.d("网络转发测试", "getEthernetNetwork    onUnavailable");

            }
        };
        cm.requestNetwork(req.build(), callback);
    }

    private boolean droneRouteIsStart = false;

    private synchronized void routeDroneRequest(final Network network) {
        if (RouteConfig.isDebug)
            Log.d("重连测试", "routeDroneRequest===" + network);
        droneRouteIsStart = true;
    }

    private synchronized void routePublicRequest(final Network network) {
        //http  公网
        String[] httpUsblConfig = new String[]{
                String.valueOf(RouteConfig.PUBLIC_HTTP_443_PORT),
                RouteConfig.PUBLIC_CONNECTION_ADDRESS, "443"};
        if (publicHttpsMain == null) {
            publicHttpsMain = new Main(mContext, network);
            if (publicHttpsMain.checkParams(httpUsblConfig)) {
                mThreadPool.execute(publicHttpsMain);
            }
        }
    }

    private NetworkEnableCallback mNetworkEnableCallback;

    public void setNetworkEnableCallback(NetworkEnableCallback networkEnableCallback) {
        mNetworkEnableCallback = networkEnableCallback;
    }

    public interface NetworkEnableCallback {
        void onDroneNetworkIsEnable(boolean isEnable, NetworkRouteManager.NetworkType networkType);

        void onPublicNetworkIsEnable(boolean isEnable, Network network, NetworkRouteManager.NetworkType networkType);
    }

    private OnReceiveBroadcastCallback mOnReceiveBroadcastCallback;

    public void setOnReceiveBroadcastCallback(OnReceiveBroadcastCallback onReceiveBroadcastCallback) {
        mOnReceiveBroadcastCallback = onReceiveBroadcastCallback;
    }

    public interface OnReceiveBroadcastCallback {
        void onReciveBroadcast(String ip, int port);
    }

    /**
     * 判断是否可以连接机器网络
     */
    public void isDroneNetworkEnable() {
        if (mSubHandler == null) return;
        if (mEthernetNetwork == null) {
            if (mWifiNetwork != null) {
                runTestDroneRunnable(mWifiNetwork, NetworkRouteManager.NetworkType.WIFI);
            }
        } else {
            runTestDroneRunnable(mEthernetNetwork, NetworkRouteManager.NetworkType.ETHERNET);
        }
    }

    private void runTestDroneRunnable(Network network, NetworkRouteManager.NetworkType networkType) {
        TestDroneRunnable testDroneRunnable = new TestDroneRunnable();
        testDroneRunnable.setmNetworkInfo(network, networkType);
        if (RouteConfig.isDebug)
            Log.d("重连测试", "runTestDroneRunnable    network:" + network);
        mSubHandler.post(testDroneRunnable);
    }

    private void runTestPublicRunnable(Network network, NetworkRouteManager.NetworkType networkType) {
        TestPublicRunnable testPublicRunnable = new TestPublicRunnable();
        testPublicRunnable.setmNetworkInfo(network, networkType);
        mSubHandler.post(testPublicRunnable);
    }

    /**
     * 测试机器网络是否通畅
     */
    private class TestDroneRunnable implements Runnable {
        private Network mNetwork;
        private NetworkRouteManager.NetworkType mNetworkType;

        public void setmNetworkInfo(Network mNetwork, NetworkRouteManager.NetworkType networkType) {
            this.mNetwork = mNetwork;
            this.mNetworkType = networkType;
        }

        @Override
        public void run() {
            if (RouteConfig.isDebug)
                Log.d("重连测试", "TestDroneRunnable   run");
            if (mNetwork == null) return;
            if (RouteConfig.isDebug)
                Log.d("重连测试", "TestDroneRunnable   run     not null   network:" + mNetwork);
            // 这里改成rov的url检查网络
            if (!checkRovConn(mNetwork)) {
                if (!checkApConn(mNetwork)) {
                    if (!checkBoxConn(mNetwork)) {
                        if (mNetworkEnableCallback != null)
                            mNetworkEnableCallback.onDroneNetworkIsEnable(false, NetworkType.NONE);
                        return;
                    }
                }
            }
            routeDroneRequest(mNetwork);
            if (mNetworkEnableCallback != null)
                mNetworkEnableCallback.onDroneNetworkIsEnable(true, mNetworkType);
            mSubHandler.removeCallbacks(this);
            if (mNetworkType == NetworkType.ETHERNET)
                mActivityNetwork = mEthernetNetwork;
            else if (mNetworkType == NetworkType.WIFI && mActivityNetwork == null)
                mActivityNetwork = mWifiNetwork;
        }
    }

    /**
     * 测试是否可以连接互联网
     */
    private class TestPublicRunnable implements Runnable {
        private Network mNetwork;
        private NetworkRouteManager.NetworkType mNetworkType;

        public void setmNetworkInfo(Network mNetwork, NetworkRouteManager.NetworkType networkType) {
            this.mNetwork = mNetwork;
            this.mNetworkType = networkType;
        }

        @Override
        public void run() {
            if (mNetwork == null) return;
            // 这里改成rov的url检查网络
            if (!checkPublicConn(mNetwork)) {
                if (mNetworkEnableCallback != null)
                    mNetworkEnableCallback.onPublicNetworkIsEnable(false, null, NetworkType.NONE);
                return;
            }
            mPublicNetwork = mNetwork;
            routePublicRequest(mNetwork);
            if (mNetworkEnableCallback != null)
                mNetworkEnableCallback.onPublicNetworkIsEnable(true, mNetwork, mNetworkType);
            mSubHandler.removeCallbacks(this);
        }
    }

    private boolean checkPublicConn(Network network) {
        try {
            URL urlRov = new URL("http://61.135.169.125/");
            HttpURLConnection urlConnection;
            urlConnection = (HttpURLConnection) network.openConnection(urlRov);
            urlConnection.setRequestMethod("GET"); //提交模式
            urlConnection.setConnectTimeout(2000);
            urlConnection.connect();
            return urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean checkRovConn(Network network) {
        try {
            URL urlRov = new URL("http://192.168.1.88:8082/v1/versions");
            HttpURLConnection urlConnection;
            urlConnection = (HttpURLConnection) network.openConnection(urlRov);
            urlConnection.setRequestMethod("GET"); //提交模式
            urlConnection.setConnectTimeout(2000);
            urlConnection.connect();
            if (RouteConfig.isDebug)
                Log.d("重连测试", "手柄  connect responseCode  :" + urlConnection.getResponseCode());
            return urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (
                Exception e) {
            e.printStackTrace();
            if (RouteConfig.isDebug)
                Log.d("重连测试", "connect  exception:" + e.getMessage());
        }
        return false;
    }

    private boolean checkApConn(Network network) {
        try {
            URL urlAP = new URL("http://192.168.1.1:8082/v1/versions");
            HttpURLConnection urlConnection;
            urlConnection = (HttpURLConnection) network.openConnection(urlAP);
            urlConnection.setRequestMethod("GET"); //提交模式
            urlConnection.setConnectTimeout(2000);
            urlConnection.connect();
            if (RouteConfig.isDebug)
                Log.d("重连测试", "手柄  connect responseCode  :" + urlConnection.getResponseCode());
            return urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (
                Exception e) {
            e.printStackTrace();
            if (RouteConfig.isDebug)
                Log.d("重连测试", "connect  exception:" + e.getMessage());
        }
        return false;
    }

    private boolean checkBoxConn(Network network) {
        try {
            URL urlBox = new URL("http://127.0.0.1:8888/v1/status");
            HttpURLConnection urlConnection;
            urlConnection = (HttpURLConnection) network.openConnection(urlBox);
            urlConnection.setRequestMethod("GET"); //提交模式
            urlConnection.connect();
            if (RouteConfig.isDebug)
                Log.d("重连测试", "Box   connect responseCode  :" + urlConnection.getResponseCode());
            return urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (
                Exception e) {
            e.printStackTrace();
            if (RouteConfig.isDebug)
                Log.d("重连测试", "connect  exception:" + e.getMessage());
        }
        return false;
    }
}
