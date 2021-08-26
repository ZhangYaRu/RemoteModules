package com.chasing.ifupgrade.bean;

/**
 * Created by a on 2017/8/4.
 */

public class UpgradePayloadBean {

    /**
     * step : prepare
     * error :
     */

    private String step;
    private String error;

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
