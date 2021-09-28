package com.chasing.networkroute;

import android.net.Network;

/**
 * Create By Olive at 2021/9/9
 */
public class AngecyParams {
    public AngecyParams(Network activityNetwork, String ip, int port) {
        this.activityNetwork = activityNetwork;
        this.ip = ip;
        this.port = port;
    }
    public AngecyParams(Network activityNetwork, String ip, int port, int localPort) {
        this.activityNetwork = activityNetwork;
        this.ip = ip;
        this.port = port;
        this.localPort = localPort;
    }

    public Network activityNetwork;
    public String ip;
    public int port;
    public int localPort;
}
