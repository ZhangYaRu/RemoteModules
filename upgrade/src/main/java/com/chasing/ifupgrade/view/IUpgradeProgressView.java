package com.chasing.ifupgrade.view;

import com.chasing.ifupgrade.bean.Versions;

public interface IUpgradeProgressView {
    void defalut();

    void downloadStarted();
    void updateDownloadProgress(int progress);
    void downloadComplete();
    void downloadError();

    void firstReboot(String upgradeType);

    void updateUploadProgress(int progress);
    void uploadComplete();

    void uploadDefalut();

    void installFirmware(String upgradeType);

    void upgradeComplete();

    void upgradeVersionsData(Versions versions);
    void upgradeFinish();

    void showError(String errorMsg);
    void connectGlobalError();
//    void showError(int sResID);
    void showHint(int sResID);
    void onUnavailable();
    void upgradeNoComplete();
}
