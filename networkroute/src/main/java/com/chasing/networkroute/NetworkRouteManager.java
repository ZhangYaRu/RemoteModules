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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
    private Main heartbeatMain;
    private Main mavlinkMain;
    private Main discoverMain;
    private Main upgradeMain;
    private Main httpAp80Main;
    private Main httpAp8082Main;
    private Main httpRov80Main;
    private Main httpRov8082Main;
    private Main dockerMain;
    private Main dockerTCPMain;
    private Main dockerHttpMain;
    private Main dockerCameraMain;
    private Main dockerCameraTCPMain;
    private Main dockerCameraHttpMain;
    private Main switchboxHttpMain;
    private Main switchboxTCPMain;
    private Main usblHttpMain;
    private Main usblTCPMain;


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

    public Map<Integer, Integer> startDockerDevicesAngency(int[] ports) {
        Map<Integer, Integer> map = new HashMap<>();
        if (RouteConfig.isDebug)
            Log.d("扩展坞外设转发测试", "startDockerDevicesAngency  ports:" + Arrays.toString(ports));
        if (ports != null && ports.length > 0) {
            if (mains == null) {
                mains = new Main[ports.length];
                for (int i = 0; i < ports.length; i++) {
                    if (ports[i] == 0) continue;
                    Main subMain = new Main(mContext, mActivityNetwork);
                    portFlag += 2;
                    map.put(ports[i], portFlag);
                    if (RouteConfig.isDebug)
                        Log.d("扩展坞外设转发测试", "startDockerDevicesAngency     put   port=" + ports[i] + ",portFlag:" + portFlag);
                    String[] config = new String[]{
                            String.valueOf(portFlag),
                            RouteConfig.DOCKER_CONNECTION_ADDRESS,
                            String.valueOf(ports[i])};
                    if (subMain.checkParams(config)) {
                        mThreadPool.execute(subMain);
                        mains[i] = subMain;
                    }
                }
            }
        }
        if (RouteConfig.isDebug)
            Log.d("扩展坞外设转发测试", "startDockerDevicesAngency     after   put" + map.size());
        return map;
    }

    public int startSinglePeripheralsAngency(int port) {
        Main subMain = new Main(mContext, mActivityNetwork);
        portFlag += 2;
        String[] config = new String[]{
                String.valueOf(portFlag),
                RouteConfig.DOCKER_CONNECTION_ADDRESS,
                String.valueOf(port)};
        if (subMain.checkParams(config)) {
            mThreadPool.execute(subMain);
            mains[mainsIndex++] = subMain;
            if (mainsIndex >= mains.length) mainsIndex = 0;
        }
        return portFlag;
    }

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
//      ============================http=====================
        if (httpRov80Main != null) {
            httpRov80Main.stop();
            mThreadPool.remove(httpRov80Main);
            httpRov80Main = null;
        }
        if (httpRov8082Main != null) {
            httpRov8082Main.stop();
            mThreadPool.remove(httpRov8082Main);
            httpRov8082Main = null;
        }
        if (httpAp80Main != null) {
            httpAp80Main.stop();
            mThreadPool.remove(httpAp80Main);
            httpAp80Main = null;
        }
        if (httpAp8082Main != null) {
            httpAp8082Main.stop();
            mThreadPool.remove(httpAp8082Main);
            httpAp8082Main = null;
        }

        if (dockerHttpMain != null) {
            dockerHttpMain.stop();
            mThreadPool.remove(dockerHttpMain);
            dockerHttpMain = null;
        }
        if (dockerCameraHttpMain != null) {
            dockerCameraHttpMain.stop();
            mThreadPool.remove(dockerCameraHttpMain);
            dockerCameraHttpMain = null;
        }
        if (switchboxHttpMain != null) {
            switchboxHttpMain.stop();
            mThreadPool.remove(switchboxHttpMain);
            switchboxHttpMain = null;
        }
        if (usblHttpMain != null) {
            usblHttpMain.stop();
            mThreadPool.remove(usblHttpMain);
            usblHttpMain = null;
        }
//      ============================tcp=====================
        if (dockerTCPMain != null) {
            dockerTCPMain.stop();
            mThreadPool.remove(dockerTCPMain);
            dockerTCPMain = null;
        }
        destroyDockerDevicesAngency();

        if (usblTCPMain != null) {
            usblTCPMain.stop();
            mThreadPool.remove(usblTCPMain);
            usblTCPMain = null;
        }

        if (switchboxTCPMain != null) {
            switchboxTCPMain.stop();
            mThreadPool.remove(switchboxTCPMain);
            switchboxTCPMain = null;
        }
//        ============================udp=====================
        if (heartbeatMain != null) {
            heartbeatMain.stop();
            mThreadPool.remove(heartbeatMain);
            heartbeatMain = null;
        }
        if (mavlinkMain != null) {
            mavlinkMain.stop();
            mThreadPool.remove(mavlinkMain);
            mavlinkMain = null;
        }
        if (discoverMain != null) {
            discoverMain.stop();
            mThreadPool.remove(discoverMain);
            discoverMain = null;
        }
        if (upgradeMain != null) {
            upgradeMain.stop();
            mThreadPool.remove(upgradeMain);
            upgradeMain = null;
        }
        if (dockerMain != null) {
            dockerMain.stop();
            mThreadPool.remove(dockerMain);
            dockerMain = null;
        }
        if (dockerCameraMain != null) {
            dockerCameraMain.stop();
            mThreadPool.remove(dockerCameraMain);
            dockerCameraMain = null;
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
        if (RouteConfig.isDebug)
            Log.d("网络转发测试", "NetworkRouteManager    findWifiNetwork");

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

        // Http ap 80
        String[] httpAp80Config = new String[]{
                String.valueOf(RouteConfig.HTTP_AP_80_ROUTE_AGENCY_PORT),
                "192.168.1.1", "80"};
        if (httpAp80Main == null) {
            httpAp80Main = new Main(mContext, network);
            if (httpAp80Main.checkParams(httpAp80Config)) {
                mThreadPool.execute(httpAp80Main);
            }
        }

        // Http ap 8082
        String[] httpAp8082 = new String[]{
                String.valueOf(RouteConfig.HTTP_AP_8082_ROUTE_AGENCY_PORT),
                "192.168.1.1", "8082"};
        if (httpAp8082Main == null) {
            httpAp8082Main = new Main(mContext, network);
            if (httpAp8082Main.checkParams(httpAp8082)) {
                mThreadPool.execute(httpAp8082Main);
            }
        }

//         Http rov  80
        String[] httpRov80Config = new String[]{
                String.valueOf(RouteConfig.HTTP_ROV_80_ROUTE_AGENCY_PORT),
                "192.168.1.88", "80"};
        if (httpRov80Main == null) {
            httpRov80Main = new Main(mContext, network);
            if (httpRov80Main.checkParams(httpRov80Config)) {
                if (RouteConfig.isDebug)
                    Log.d("网络转发测试", "checkParams===ok " + network);
                mThreadPool.execute(httpRov80Main);
            } else {
                if (RouteConfig.isDebug)
                    Log.d("网络转发测试", "checkParams===no " + network);
            }
        }

        // Http rov 8082
        String[] httpRov8082Config = new String[]{
                String.valueOf(RouteConfig.HTTP_ROV_8082_ROUTE_AGENCY_PORT),
                "192.168.1.88", "8082"};
        if (httpRov8082Main == null) {
            httpRov8082Main = new Main(mContext, network);
            if (httpRov8082Main.checkParams(httpRov8082Config)) {
                mThreadPool.execute(httpRov8082Main);
            }
        }
        // CameraHeartbeat
        String[] heartbeatConfig = new String[]{
                String.valueOf(RouteConfig.CAMERA_HEARTBEAT_ROUTE_LISTEN_PORT),
                String.valueOf(RouteConfig.CAMERA_HEARTBEAT_APP_LISTEN_PORT),
                "192.168.1.88", "8000", "--udp-tun"};
        if (heartbeatMain == null) {
            if (RouteConfig.isDebug)
                Log.d("重连测试", "routeDroneRequest   new  heartbeatMain");
            heartbeatMain = new Main(mContext, network);
        }
        if (heartbeatMain.checkParams(heartbeatConfig)) {
            if (RouteConfig.isDebug)
                Log.d("重连测试", "checkParams   heartbeatMain  OK  ===");
            mThreadPool.execute(heartbeatMain);
        }

        // Discover
        String[] discoverConfig = new String[]{
                String.valueOf(RouteConfig.CAMERA_DISCOVER_ROUTE_LISTEN_PORT),
                String.valueOf(RouteConfig.CAMERA_DISCOVER_APP_LISTEN_PORT),
                "192.168.1.88", String.valueOf(RouteConfig.CAMERA_DISCOVER_SEND_TO_ROV_PORT),
                "--udp-tun"};
        if (discoverMain == null) {
            discoverMain = new Main(mContext, network);
        }
        if (discoverMain.checkParams(discoverConfig)) {
            mThreadPool.execute(discoverMain);
        }

        // Mavlink
        String[] mavlinkConfig = new String[]{
                String.valueOf(RouteConfig.MAVINLINK_ROUTE_LISTEN_PORT),
                String.valueOf(RouteConfig.MAVINLINK_APP_LISTEN_PORT),
                "192.168.1.88", "14550", "--udp-tun"};
        if (mavlinkMain == null) {
            mavlinkMain = new Main(mContext, network);
        }
        if (mavlinkMain.checkParams(mavlinkConfig)) {
            mThreadPool.execute(mavlinkMain);
        }

        // upgrade
        String[] upgradeConfig = new String[]{
                String.valueOf(RouteConfig.UPGRADE_ROUTE_LISTEN_PORT),
                String.valueOf(RouteConfig.UPGRADE_APP_LISTEN_PORT),
                "192.168.1.99", "5568", "--udp-tun"};// 这里ip随便写, 并不会用到
        if (upgradeMain == null) {
            upgradeMain = new Main(mContext, network);
        }
        if (upgradeMain.checkParams(upgradeConfig)) {
            mThreadPool.execute(upgradeMain);
        }

        // 扩展坞广播
        String[] dockerConfig = new String[]{
                String.valueOf(RouteConfig.DOCKER_BROADCAST_PORT),
                String.valueOf(RouteConfig.DOCKER_ANGENCY_BROADCAST_PORT),
                RouteConfig.DOCKER_CONNECTION_ADDRESS,
                "8001",
                "--udp-tun"};
        if (dockerMain == null) {
            dockerMain = new Main(mContext, network);
            dockerMain.setDiscoveringListener(new Main.DiscoveringListener() {
                @Override
                public void onDiscovering(AngencyBean angencyBean) {
                    if (angencyBean != null) {
                        angencyBean.setAngencyPort(portFlag);
                        portFlag += 2;
                        activityTunnels.add(angencyBean);
                        if (RouteConfig.isDebug)
                            Log.d("扩展坞连接测试", "NetworkRouteManager   onDiscovering    " + angencyBean);
                        if (RouteConfig.DOCKER_CONNECTION_ADDRESS.equals(angencyBean.getRemoteAds())) {
                            startDockerAngency(angencyBean, network);
                        }// 这里判断是外置摄像头
                    }
                }
            });
        }
        if (dockerMain.checkParams(dockerConfig)) {
            mThreadPool.execute(dockerMain);
        }

        startDockerCameraAngency(network);

        // 外置摄像头广播
//        String[] dockerCameraConfig = new String[]{
//                String.valueOf(RouteConfig.),
//                RouteConfig.DOCKER_CONNECTION_ADDRESS,
//                String.valueOf(RouteConfig.DOCKER_CONNECTION_PORT)};
//        if (dockerCameraMain == null) {
//                dockerCameraMain = new Main(mContext, network);
//            if (dockerCameraMain.checkParams(dockerCameraConfig)) {
//                mThreadPool.execute(dockerCameraMain);
//            }
//        }
        //http 转接盒 80
        String[] httpAdapterBoxConfig = new String[]{
                String.valueOf(RouteConfig.SWITCHBOX_HTTP_80_PORT),
                RouteConfig.SWITCHBOX_CONNECTION_ADDRESS, "80"};
        if (switchboxHttpMain == null) {
            switchboxHttpMain = new Main(mContext, network);
            if (switchboxHttpMain.checkParams(httpAdapterBoxConfig)) {
                mThreadPool.execute(switchboxHttpMain);
            }
        }

        // 转接盒连接
        if (switchboxTCPMain == null) {
            String[] switchboxTCPConfig = new String[]{
                    String.valueOf(RouteConfig.SWITCHBOX_TCP_PORT),
                    RouteConfig.SWITCHBOX_CONNECTION_ADDRESS,
                    String.valueOf(RouteConfig.SWITCHBOX_CONNECTION_PORT)};
            switchboxTCPMain = new Main(mContext, network);
            if (switchboxTCPMain.checkParams(switchboxTCPConfig)) {
                mThreadPool.execute(switchboxTCPMain);
            }
        }
        //http USBL 80
        String[] httpUsblConfig = new String[]{
                String.valueOf(RouteConfig.USBL_HTTP_80_PORT),
                RouteConfig.USBL_CONNECTION_ADDRESS, "80"};
        if (usblHttpMain == null) {
            usblHttpMain = new Main(mContext, network);
            if (usblHttpMain.checkParams(httpUsblConfig)) {
                mThreadPool.execute(usblHttpMain);
            }
        }

        // USBL连接
        if (usblTCPMain == null) {
            String[] usblTCPConfig = new String[]{
                    String.valueOf(RouteConfig.USBL_TCP_PORT),
                    RouteConfig.USBL_CONNECTION_ADDRESS,
                    String.valueOf(RouteConfig.USBL_CONNECTION_PORT)};
            usblTCPMain = new Main(mContext, network);
            if (usblTCPMain.checkParams(usblTCPConfig)) {
                mThreadPool.execute(usblTCPMain);
            }
        }
    }

    private void startDockerAngency(AngencyBean angencyBean, Network network) {
        // 扩展坞连接
        if (dockerTCPMain == null) {
            if (RouteConfig.isDebug)
                Log.d("扩展坞连接测试", "NetworkRouteManager   onDiscovering   dockerTCPMain ");
            if (mOnReceiveBroadcastCallback != null)
                mOnReceiveBroadcastCallback.onReciveBroadcast(
                        "127.0.0.1", angencyBean.getAngencyPort());
            String[] dockerTCPConfig = new String[]{
                    String.valueOf(angencyBean.getAngencyPort()),
                    angencyBean.getRemoteAds(),
                    String.valueOf(RouteConfig.DOCKER_CONNECTION_PORT)};
            dockerTCPMain = new Main(mContext, network);
            if (RouteConfig.isDebug)
                Log.d("扩展坞连接测试", "NetworkRouteManager   onDiscovering    new");
            if (dockerTCPMain.checkParams(dockerTCPConfig)) {
                if (RouteConfig.isDebug)
                    Log.d("扩展坞连接测试", "NetworkRouteManager   onDiscovering   checkparams   ok ");
                mThreadPool.execute(dockerTCPMain);
            }
        }
        //扩展坞http  80端口
        if (dockerHttpMain == null) {
            String[] dockerTCPConfig = new String[]{
                    String.valueOf(RouteConfig.DOCKER_ANGENCY_HTTP_80_PORT),
                    angencyBean.getRemoteAds(), "80"};
            dockerHttpMain = new Main(mContext, network);
            if (RouteConfig.isDebug)
                Log.d("扩展坞连接测试", "NetworkRouteManager   onDiscovering    new");
            if (dockerHttpMain.checkParams(dockerTCPConfig)) {
                if (RouteConfig.isDebug)
                    Log.d("扩展坞连接测试", "NetworkRouteManager   onDiscovering   checkparams   ok ");
                mThreadPool.execute(dockerHttpMain);
            }
        }
    }

    private void startDockerCameraAngency(Network network) {
        //外置摄像头  80端口
        if (dockerCameraHttpMain == null) {
            String[] dockerTCPConfig = new String[]{
                    String.valueOf(RouteConfig.DOCKER_CAMERA_ANGENCY_HTTP_80_PORT),
                    RouteConfig.DOCKER_CAMERA_CONNECTION_ADDRESS, "80"};
            dockerCameraHttpMain = new Main(mContext, network);
            if (RouteConfig.isDebug)
                Log.d("扩展坞连接测试", "NetworkRouteManager  startDockerCameraAngency");
            if (dockerCameraHttpMain.checkParams(dockerTCPConfig)) {
                if (RouteConfig.isDebug)
                    Log.d("扩展坞连接测试", "NetworkRouteManager   startDockerCameraAngency   checkparams   ok ");
                mThreadPool.execute(dockerCameraHttpMain);
            }
        }
    }

    private NetworkEnableCallback mNetworkEnableCallback;

    public void setNetworkEnableCallback(NetworkEnableCallback networkEnableCallback) {
        mNetworkEnableCallback = networkEnableCallback;
    }

    public interface NetworkEnableCallback {
        void onDroneNetworkIsEnable(boolean isEnable, NetworkRouteManager.NetworkType networkType);

        void onPublicNetworkIsEnable(boolean isEnable, NetworkRouteManager.NetworkType networkType);
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
