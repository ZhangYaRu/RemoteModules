package com.chasing.ifupgrade.bean;

import com.google.gson.annotations.SerializedName;

public class UpgradeStatus {

    /**
     * status : 3
     * percent : 100
     * errcode : 1001
     */

    @SerializedName("status")
    private int status;
    @SerializedName("percent")
    private int percent;
    @SerializedName("errcode")
    private int errcode;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public int getErrcode() {
        return errcode;
    }

    public void setErrcode(int errcode) {
        this.errcode = errcode;
    }
}
