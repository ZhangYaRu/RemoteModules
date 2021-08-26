package com.chasing.ifupgrade.bean;

import com.google.gson.annotations.SerializedName;

public class Versions {

    /**
     * ap : 1.2.3
     * rov : 1.32.0
     * android : 1.0.0
     * ios : 1.1.0
     * model : DORY
     */

    @SerializedName("ap")
    private String ap;
    @SerializedName("rov")
    private String rov;
    @SerializedName("android")
    private String android;
    @SerializedName("ios")
    private String ios;
    @SerializedName("model")
    private String model;
    @SerializedName("docker")
    private String docker;

    public String getDocker() {
        return docker;
    }

    public void setDocker(String docker) {
        this.docker = docker;
    }

    public String getAp() {
        return ap;
    }

    public void setAp(String ap) {
        this.ap = ap;
    }

    public String getRov() {
        return rov;
    }

    public void setRov(String rov) {
        this.rov = rov;
    }

    public String getAndroid() {
        return android;
    }

    public void setAndroid(String android) {
        this.android = android;
    }

    public String getIos() {
        return ios;
    }

    public void setIos(String ios) {
        this.ios = ios;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
