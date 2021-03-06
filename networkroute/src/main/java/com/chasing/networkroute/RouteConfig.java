package com.chasing.networkroute;

public interface RouteConfig {
    boolean isDebug = false;

    String LOCAL_ADDRESS = "127.0.0.1";
    // CameraHeartbeat
    int CAMERA_HEARTBEAT_ROUTE_LISTEN_PORT = 5566;
    int CAMERA_HEARTBEAT_APP_LISTEN_PORT = 7000;

    // Cameradiscover
    int CAMERA_DISCOVER_ROUTE_LISTEN_PORT = 5580;
    int CAMERA_DISCOVER_SEND_TO_ROV_PORT = 8002;
    int CAMERA_DISCOVER_APP_LISTEN_PORT = 7002;

    // Upgrade
    int UPGRADE_ROUTE_LISTEN_PORT = 5568;
    int UPGRADE_APP_LISTEN_PORT = 7004;

    // mavlink
    int MAVINLINK_ROUTE_LISTEN_PORT = 14550;
    int MAVINLINK_APP_LISTEN_PORT = 7006;

    // http ap 80
    int HTTP_AP_80_ROUTE_AGENCY_PORT = 7008;
    // http ap 8082
    int HTTP_AP_8082_ROUTE_AGENCY_PORT = 7010;

    // http rov 80
    int HTTP_ROV_80_ROUTE_AGENCY_PORT = 7012;
    // http rov 8082
    int HTTP_ROV_8082_ROUTE_AGENCY_PORT = 7014;

    // 扩展坞
    String DOCKER_CONNECTION_ADDRESS = "192.168.1.101";
    int DOCKER_CONNECTION_PORT = 15100;
//    String DOCKER_CONNECTION_ADDRESS = "192.168.1.1";
//    int DOCKER_CONNECTION_PORT = 8600;
    int DOCKER_BROADCAST_PORT = 8500;
    int DOCKER_ANGENCY_BROADCAST_PORT = 7016;
    // http docker 80
    int DOCKER_ANGENCY_HTTP_80_PORT = 7018;


    // 外置摄像头
    String DOCKER_CAMERA_CONNECTION_ADDRESS = "192.168.1.102";
    int DOCKER_CAMERA_CONNECTION_PORT = 8600;
    int DOCKER_CAMERA_BROADCAST_PORT = DOCKER_BROADCAST_PORT;
    int DOCKER_CAMERA_ANGENCY_BROADCAST_PORT = 7020;
    // http dockercamera 80
    int DOCKER_CAMERA_ANGENCY_HTTP_80_PORT = 7022;

    // 转接盒http代理端口
    String SWITCHBOX_CONNECTION_ADDRESS = "192.168.1.10";
    int SWITCHBOX_HTTP_80_PORT = 7024;

    // 转接盒tcp连接代理端口
    int SWITCHBOX_CONNECTION_PORT = 8600;
    int SWITCHBOX_TCP_PORT = 7026;

    // USBL   http代理端口
    String USBL_CONNECTION_ADDRESS = "192.168.1.103";
    int USBL_HTTP_80_PORT = 7028;

    // 转接盒tcp连接代理端口
    int USBL_CONNECTION_PORT = 8600;
    int USBL_TCP_PORT = 7030;

}
