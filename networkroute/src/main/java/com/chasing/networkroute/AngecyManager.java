package com.chasing.networkroute;

import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Create By Olive at 2021/9/9
 */
public class AngecyManager {

    private static AngecyManager instance = new AngecyManager();

    private AngecyManager() {
    }

    /**
     * 获取代理管理实例
     *
     * @return
     */
    public static AngecyManager getInstance() {
        return instance;
    }

    /**
     * 代理连接池管理
     */
    private HashMap<String, BaseAngecy> connectionPoolMap = new HashMap<>();

    /**
     * 通过ip=port形式得到对应连接
     *
     * @param key
     * @return
     */
    public BaseAngecy getConnectionForKey(String key) {
        return connectionPoolMap.get(key);
    }

    /**
     * 清空连接池
     */
    public synchronized void clearConnectionPool() {
        for (Map.Entry<String, BaseAngecy> entry : connectionPoolMap.entrySet()) {
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            if (entry.getValue() != null) {
                entry.getValue().stop();
            }
        }
        connectionPoolMap.clear();
    }

    //http USBL 80
//    String[] httpUsblConfig = new String[]{
//            String.valueOf(RouteConfig.USBL_HTTP_80_PORT),
//            RouteConfig.USBL_CONNECTION_ADDRESS, "80"};
//        if (usblHttpMain == null) {
//        usblHttpMain = new Main(mContext, network);
//        if (usblHttpMain.checkParams(httpUsblConfig)) {
//            mThreadPool.execute(usblHttpMain);
//        }
//    }
    // CameraHeartbeat
//    String[] heartbeatConfig = new String[]{
//            String.valueOf(RouteConfig.CAMERA_HEARTBEAT_ROUTE_LISTEN_PORT),
//            String.valueOf(RouteConfig.CAMERA_HEARTBEAT_APP_LISTEN_PORT),
//            "192.168.1.88", "8000", "--udp-tun"};
//        if (heartbeatMain == null) {
//        if (RouteConfig.isDebug)
//            Log.d("重连测试", "routeDroneRequest   new  heartbeatMain");
//        heartbeatMain = new Main(mContext, network);
//    }
//        if (heartbeatMain.checkParams(heartbeatConfig)) {
//        if (RouteConfig.isDebug)
//            Log.d("重连测试", "checkParams   heartbeatMain  OK  ===");
//        mThreadPool.execute(heartbeatMain);
//    }

    /**
     * 增加一条TCP代理连接
     *
     * @param angecyParams 配置参数
     * @return 连接正常, 返回0   连接异常 返回-1
     */
    public synchronized int addNewTCPAngecy(AngecyParams angecyParams, BaseAngecy.AngecyActionListener angecyActionListener) {
        // 校验数据有效性
        if (angecyParams == null || angecyParams.activityNetwork == null
                || TextUtils.isEmpty(angecyParams.ip) || angecyParams.port == 0)
            return -1;
        TCPAngecy tcpAngecy = new TCPAngecy(angecyParams);
        tcpAngecy.setAngecyActionListener(angecyActionListener);
        tcpAngecy.start();
        connectionPoolMap.put(angecyParams.ip + ":" + angecyParams.port, tcpAngecy);
        return 0;
    }

    /**
     * 增加一条HTTP代理连接
     *
     * @param angecyParams 配置参数
     * @return 连接正常, 返回0   连接异常 返回-1
     */
    public synchronized int addNewHTTPAngecy(AngecyParams angecyParams, BaseAngecy.AngecyActionListener angecyActionListener) {
        // 校验数据有效性
        if (angecyParams == null || angecyParams.activityNetwork == null
                || TextUtils.isEmpty(angecyParams.ip) || angecyParams.port == 0)
            return -1;
        HttpAngecy httpAngecy = new HttpAngecy(angecyParams);
        httpAngecy.setAngecyActionListener(angecyActionListener);
        httpAngecy.start();
        connectionPoolMap.put(angecyParams.ip + ":" + angecyParams.port, httpAngecy);
        return 0;
    }

    /**
     * 增加一条UDP代理连接
     *
     * @param angecyParams 配置参数
     * @return 连接正常, 返回0   异常 返回-1
     */
    public synchronized int addNewUDPAngecy(AngecyParams angecyParams, BaseAngecy.AngecyActionListener angecyActionListener) {
        // 校验数据有效性
        if (angecyParams == null || angecyParams.activityNetwork == null
                || TextUtils.isEmpty(angecyParams.ip) || angecyParams.port == 0)
            return -1;
        String mapKey = angecyParams.ip + ":" + angecyParams.port;
        if (connectionPoolMap.containsKey(mapKey)) {
            BaseAngecy baseAngecy = connectionPoolMap.get(mapKey);
            if (baseAngecy != null) {
                baseAngecy.setAngecyActionListener(angecyActionListener);
                return baseAngecy.angecyPortFlag;
            }
        }
//        Log.d("代理转发测试", "angecyMangaer    addNewUDPAngecy:   ===" +mapKey);

        UDPAngecy udpAngecy = new UDPAngecy(angecyParams);
        udpAngecy.setAngecyActionListener(angecyActionListener);
        udpAngecy.start();
        connectionPoolMap.put(mapKey, udpAngecy);
        return 0;
    }
}
