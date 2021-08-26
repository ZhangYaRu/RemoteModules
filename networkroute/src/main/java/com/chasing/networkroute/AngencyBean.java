package com.chasing.networkroute;

public class AngencyBean {

    public static String UDP = "udp";
    public static String TCP = "tcp";

    /**
     * 远程ip
     */
    private String remoteAds;
    /**
     * 远程端口
     */
    private int remotePort;
    /**
     * 源头端口(APP原先监听的端口)
     */
    private int sourcePort;
    /**
     * 代理端口(随机分配的代理端口, 开启代理后APP应监听此端口)
     */
    private int angencyPort;
    /**
     * 连接类型, udp/tcp
     */
    private String connectType;

    public String getRemoteAds() {
        return remoteAds;
    }

    public void setRemoteAds(String remoteAds) {
        this.remoteAds = remoteAds;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public int getAngencyPort() {
        return angencyPort;
    }

    public void setAngencyPort(int angencyPort) {
        this.angencyPort = angencyPort;
    }

    public String getConnectType() {
        return connectType;
    }

    public void setConnectType(String connectType) {
        this.connectType = connectType;
    }

    @Override
    public String toString() {
        return "AngencyBean{" +
                "remoteAds='" + remoteAds + '\'' +
                ", remotePort=" + remotePort +
                ", sourcePort=" + sourcePort +
                ", angencyPort=" + angencyPort +
                ", connectType='" + connectType + '\'' +
                '}';
    }
}
