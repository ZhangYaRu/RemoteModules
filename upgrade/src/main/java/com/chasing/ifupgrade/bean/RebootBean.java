package com.chasing.ifupgrade.bean;

import com.google.gson.annotations.SerializedName;

public class RebootBean {

    /**
     * ip : 192.168.1.1
     */

    @SerializedName("ip")
    private String ip;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
