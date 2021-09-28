package com.chasing.networkroute;

public interface RouteConfig {
    boolean isDebug = false;

    String LOCAL_ADDRESS = "127.0.0.1";
    // USBL   http代理端口
    String PUBLIC_CONNECTION_ADDRESS = "89.208.247.90";// https://api.chasing.com
    int PUBLIC_HTTP_443_PORT = 7028;
}
