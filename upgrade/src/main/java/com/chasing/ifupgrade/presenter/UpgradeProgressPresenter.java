package com.chasing.ifupgrade.presenter;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;

import com.chasing.ifupgrade.bean.CameraStatusBean;
import com.chasing.ifupgrade.bean.RebootBean;
import com.chasing.ifupgrade.bean.UpgradeStatus;
import com.chasing.ifupgrade.bean.Versions;
import com.chasing.ifupgrade.constants.UpgradeConstants;
import com.chasing.ifupgrade.http.BaseUpgradeObserver;
import com.chasing.ifupgrade.http.RxHttpRequest;
import com.chasing.ifupgrade.utils.HttpConnUtil;
import com.chasing.ifupgrade.utils.NetUtil;
import com.chasing.ifupgrade.utils.Utils;
import com.chasing.ifupgrade.view.IUpgradeProgressView;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class UpgradeProgressPresenter {

//    @Inject
//    EventBus mEventbus;
//
//    @Inject
//    CameraManager mCameraManager;

    private final int DEFAULT = 0;
    private final int START_DOWNLOAD = 1;
    private final int DOWNLOADING = 2;
    private final int DOWNLOAD_DONE = 3;
    private final int FIRST_REBOOT = 4;
    private final int FIRST_REBOOTING = 5;
    private final int START_UPLOAD = 6;
    private final int UPLOADING = 7;
    private final int UPLOAD_DONE = 8;
    private final int SECOND_REBOOT = 9;
    private final int SECOND_REBOOT_SUCCESS = 10;
    private final int UPGRADE_SUCCESS = 11;
    private final int ROV_FIRST_REBOOT = 12;


    private final int CELLULAR_FAIL = 13;
    private int mCurStep = 0;

    private IUpgradeProgressView mView;
    private String mDownloadUrl;
    private String mUpgradeType;

    private HandlerThread upgradethread;
    private Handler subHandler;
    private File mUpgradeFile;
    private int machineType = -1;

    private boolean cellularIsRunning = false;
    private boolean cellularFirst = true;
    private boolean pingIsRunning = false;
    private boolean isUseCellularNetwork = false;
    private boolean isNeedFirstReboot = false;
    private String baseUrl = "";
    private CameraStatusBean mUpgradeApStatus;
    private String appType = "GO2";
    private String downLoadPath = "GO2";

    private boolean isDebug = true;

    private Context mContext;

    public void setmContext(Context mContext) {
        this.mContext = mContext;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isUseCellularNetwork() {
        return isUseCellularNetwork;
    }

    public void setUseCellularNetwork(boolean useCellularNetwork) {
        isUseCellularNetwork = useCellularNetwork;
    }

    public void setNeedFirstReboot(boolean needFirstReboot) {
        isNeedFirstReboot = needFirstReboot;
    }

    public boolean isNeedFirstReboot() {
        return isNeedFirstReboot;
    }

    public UpgradeProgressPresenter(Context mContext, IUpgradeProgressView view, String baseUrl, String downloadUrl, String upgradeType) {
        this.baseUrl = baseUrl;
        this.mContext = mContext;
        mView = view;
        mDownloadUrl = downloadUrl;
        mUpgradeType = upgradeType;
        init();
    }

    public UpgradeProgressPresenter setMachineType(int machineType) {
        this.machineType = machineType;
        return this;
    }
    public UpgradeProgressPresenter setAppType(String appType) {
        this.appType = appType;
        return this;
    }
    public UpgradeProgressPresenter setDownLoadPath(String downLoadPath) {
        this.downLoadPath = downLoadPath;
        return this;
    }

    private void init() {
        Logger.addLogAdapter(new AndroidLogAdapter() {
            @Override
            public boolean isLoggable(int priority, @Nullable String tag) {
                return isDebug;
            }
        });
        upgradethread = new HandlerThread("upgradethread");
        upgradethread.start();
        subHandler = new Handler(upgradethread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case FIRST_REBOOTING:
                        if (mCurStep == FIRST_REBOOTING) {
                            getAPStatusSync(baseUrl);
                            subHandler.sendEmptyMessageDelayed(FIRST_REBOOTING, 1000);
                        }
                        break;
                    case CELLULAR_FAIL:
                        mView.connectGlobalError();
//                        mView.showError(R.string.please_connnect_global_net);
                        break;
                    case UPLOADING:
                        if (mCurStep == UPLOADING) {
                            getUpgradeStatus(baseUrl);
//                            subHandler.sendEmptyMessageDelayed(UPLOADING, 1000);
                        }
                        break;
                    case UPGRADE_SUCCESS:
                        if (mCurStep == UPGRADE_SUCCESS) {
                            getNewRovVersion(baseUrl);
//                            subHandler.sendEmptyMessageDelayed(UPGRADE_SUCCESS, 1000);
                        }
                        break;
                }
            }
        };
    }

//    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
//    public void onConnectStatusUpdate(BaseEvent event) {
//        switch (event.getEventId()) {
//            case CameraEvent.DORY_CONNECTED:
//                if (mCurStep == SECOND_REBOOT_SUCCESS) {
//                    mCurStep = UPGRADE_SUCCESS;
//                    getNewVersion();
//                }
//                // TODO   版本更新~
//                break;
//        }
//    }


    public void nextStep() {
        Logger.e("FirmwareUPgrade    presenter   machineType " + machineType + " mCurStep " + mCurStep);
        switch (mCurStep) {
            case DEFAULT:

                Logger.e("FirmwareUPgrade    presenter    DEFAULT");
                mCurStep = START_DOWNLOAD;
                // 取消文件是否存在校验, 直接去下载
                mView.downloadStarted();
//                checkFileExists();
                // 检查网络连接
                checkwifiInternet();
                break;
            case DOWNLOAD_DONE:
//                getStatus();
                Logger.e("FirmwareUPgrade    presenter    DOWNLOAD_DONE ");
//                mView.showHint(R.string.upgrade_cant_intercept_hint);
                if (machineType == UpgradeConstants.DEVICES_DORY) {
                    if (mUpgradeType.equals(UpgradeConstants.DEV_AP)) {
                        mCurStep = FIRST_REBOOT;
                    } else {
//                        mCurStep = ROV_FIRST_REBOOT;
                        Logger.e("FirmwareUPgrade    presenter    mCurStep = ROV_FIRST_REBOOT ");
                        mCurStep = START_UPLOAD;
                        uploadUpgradeFile(baseUrl);
                    }
                } else {
                    getAPStatusSync(baseUrl);
                }
                mView.firstReboot(mUpgradeType);
                break;
            case ROV_FIRST_REBOOT:
                getAPStatusSync(baseUrl);
                break;
            case FIRST_REBOOT:
                Logger.e("FirmwareUPgrade    presenter    FIRST_REBOOT");
//                firstReboot();
//                getStatus();
                getAPStatusSync(baseUrl);
                break;
            case FIRST_REBOOTING:
                Logger.e("FirmwareUPgrade    presenter    FIRST_REBOOTING");
                break;
            case UPLOAD_DONE:
                Logger.e("FirmwareUPgrade    presenter    UPLOAD_DONE");
                if (machineType == UpgradeConstants.DEVICES_DORY) {

                    mCurStep = SECOND_REBOOT;
                    mView.installFirmware(mUpgradeType);
                } else {
                    secondReboot(baseUrl);
                }
                break;
            case SECOND_REBOOT:
                Logger.e("FirmwareUPgrade    presenter    SECOND_REBOOT");
                secondReboot(baseUrl);
                break;
//            case SECOND_REBOOT_SUCCESS:
//                Logger.e("FirmwareUPgrade    presenter    SECOND_REBOOT_SUCCESS");
//                mView.upgradeFinish();
//                break;
            case UPGRADE_SUCCESS:
                Logger.e("FirmwareUPgrade    presenter    UPGRADE_SUCCESS");
                mView.upgradeFinish();
                break;
        }
    }

    public int getmCurStep() {
        return mCurStep;
    }

    public void backStep(int curStep) {
        if (curStep == -1) {
            mCurStep = DEFAULT;
        }
        if (curStep == 3) {
            mCurStep = UPLOADING;
        }
        switch (mCurStep) {
//            case DEFAULT:
//                Logger.e("FirmwareUPgrade    presenter  backStep  DEFAULT");
//                mCurStep = START_DOWNLOAD;
//                mView.downloadStarted();
//                // 取消文件是否存在校验, 直接去下载
////                checkFileExists();
//                // 检查网络连接
//                checkwifiInternet();
//                break;
            case DOWNLOAD_DONE:
                Logger.e("FirmwareUPgrade    presenter  backStep    UPLOAD_DONE");
                mCurStep = DEFAULT;
//                nextStep();
                break;
            case FIRST_REBOOT:
                Logger.e("FirmwareUPgrade    presenter    FIRST_REBOOT");
//                firstReboot();
//                getStatus();
                break;
            case UPLOADING:
                mCurStep = DOWNLOAD_DONE;
                break;
            case UPLOAD_DONE:
                Logger.e("FirmwareUPgrade    presenter  backStep  UPLOAD_DONE");
//                mCurStep = DOWNLOAD_DONE;
//                nextStep();
                break;
            case SECOND_REBOOT:
                Logger.e("FirmwareUPgrade    presenter  backStep  SECOND_REBOOT");
                mCurStep = UPLOAD_DONE;
//                nextStep();
                break;
        }
    }

//
//    private void checkFileExists() {
//        if (!TextUtils.isEmpty(mDownloadUrl)) {
//            // 判断文件是否已经存在,存在则不需要下载等操作
//            String fileName = mDownloadUrl.substring(mDownloadUrl.lastIndexOf('/') + 1);
//            File file;
//            if (!TextUtils.isEmpty(fileName)) {
//                file = new File(appType.equals(UpgradeConstants.GO1_ROOT_TYPE)?UpgradeConstants.UPGRADE_DOWNLOAD_PATH_DIVE:UpgradeConstants.UPGRADE_DOWNLOAD_PATH   + fileName);
//                if (file.exists()) {
//                    // 判断文件大小是否一致
//                    subHandler.post(fileIsCompleteTask);
//                    return;
//                }
//            }
//            // 检查网络连接
//            checkwifiInternet();
//        }
////        else {
////            mView.showError(R.string.url_parse_error);
////        }
//    }


//    private Runnable fileIsCompleteTask = new Runnable() {
//        @Override
//        public void run() {
//            String fileName = mDownloadUrl.substring(mDownloadUrl.lastIndexOf('/') + 1);
//            File file = new File(appType.equals(UpgradeConstants.GO1_ROOT_TYPE)?UpgradeConstants.UPGRADE_DOWNLOAD_PATH_DIVE:UpgradeConstants.UPGRADE_DOWNLOAD_PATH  + fileName);
//            boolean same = Utils.isSameSize(mDownloadUrl, file);
//            if (same) {
//                downloasSuccess(file);
//            } else {
//                checkwifiInternet();
//            }
//        }
//    };

    // 检查wifi是否可以连接互联网
    private void checkwifiInternet() {
        // 判断是否可以连接互联网
        Logger.d("文件的大小是 检测网络 " + pingIsRunning);
        if (!pingIsRunning) {
            pingIsRunning = true;
            subHandler.removeCallbacks(pingTask);
            subHandler.post(pingTask);
        }
    }

    private Runnable pingTask = new Runnable() {
        @Override
        public void run() {
            Process p = null;
            try {
                p = Runtime.getRuntime().exec("ping -c 2 -w 3 " + "www.baidu.com");
                int status = p.waitFor();
                Logger.d("文件的大小是 ping的结果： " + status);
                switch (status) {
                    case 0:// 成功
                        Logger.d("文件的大小是 开始wifi下载");
                        wifiDownloadUpgradeFile(mDownloadUrl, isUseCellularNetwork);
                        break;
                    default:// 失败
                        Logger.d("文件的大小是 开始流量下载");
                        if (!NetUtil.hasSimCard(mContext)) {
                            pingIsRunning = false;
                            mView.connectGlobalError();
                            return;
                        }
                        if (NetUtil.getDataEnabled(mContext)) {// 使用移动流量下载
                            Logger.d("文件的大小是 流量可用");
                            if (!cellularIsRunning) {
                                cellularIsRunning = true;
                                subHandler.removeCallbacks(cellularTask);
                                cellularFirst = true;
                                subHandler.post(cellularTask);
//                                mView.showHint(R.string.upgrade_cant_intercept_hint);
                            }
                        } else {// 无可用网络
//                            mView.showError(R.string.please_connnect_global_net);
                            mView.connectGlobalError();
                            mCurStep = DEFAULT;
                            mView.defalut();
                        }
                        break;
                }

            } catch (IOException e) {
                e.printStackTrace();
                Logger.d("文件的大小是 " + e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
                Logger.d("文件的大小是 " + e.getMessage());
            }
            pingIsRunning = false;
        }
    };

    private Thread cellularTask = new Thread() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void run() {
            // 指定某个网络请求采用指定的网络访问
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
            NetworkRequest.Builder req = new NetworkRequest.Builder();
            req.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            req.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
            ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    if (cellularFirst) {
                        cellularFirst = false;
                        cellularIsRunning = false;
                        isUseCellularNetwork = true;
                        downloadUpgradeFile(mDownloadUrl, isUseCellularNetwork);
                    }
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    cellularIsRunning = false;
                    mView.onUnavailable();
//                    mView.showError(R.string.please_connnect_global_net);
                }
            };
            cm.requestNetwork(req.build(), callback);
        }
    };

    private HttpConnUtil.OnHttpDownloadListener downloadListener = new HttpConnUtil.OnHttpDownloadListener() {
        @Override
        public void onDownloadSuccessed(File response) {
            downloasSuccess(response);
        }

        @Override
        public void onStartDownload(int all) {
            mView.updateDownloadProgress(0);
//            mView.showHint(R.string.upgrade_cant_intercept_hint);
        }

        @Override
        public void onProgress(int progress) {
            mView.updateDownloadProgress(progress);
        }

        @Override
        public void onDownloadFailed() {
            mCurStep = DEFAULT;
            mView.downloadError();
//            mView.showError(R.string.download_error);
        }
    };

    private void downloadUpgradeFile(String downloadUrl, boolean isUseCellularNetwork) {
        if (TextUtils.isEmpty(downloadUrl)) {
//            T.getInstance().showToast(R.string.url_parse_error);
            return;
        }

        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
        Logger.d("文件的大小是 URL " + fileName);
        if (TextUtils.isEmpty(fileName)) return;
        File file = new File(downLoadPath + fileName);
//        File file = new File((appType.equals(UpgradeConstants.GO1_ROOT_TYPE)?UpgradeConstants.UPGRADE_DOWNLOAD_PATH_DIVE:UpgradeConstants.UPGRADE_DOWNLOAD_PATH ) + fileName);
        Logger.d("文件的大小是 URL不为空！");
        // 判断用户是否同意同时使用蜂窝数据
        if (isUseCellularNetwork) {
            HttpConnUtil.doDownload(mContext, downloadUrl, file, isUseCellularNetwork, downloadListener);
        }
    }

    private void wifiDownloadUpgradeFile(String downloadUrl, boolean isUseCellularNetwork) {
        if (TextUtils.isEmpty(downloadUrl)) {
//            T.getInstance().showToast(R.string.url_parse_error);
            return;
        }
        Logger.d("文件的大小是 开始wifi下载");
        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
        Logger.d("文件的大小是 " + fileName);
        if (TextUtils.isEmpty(fileName)) return;
        File file = new File(downLoadPath + fileName);
//        File file = new File((appType.equals(UpgradeConstants.GO1_ROOT_TYPE)?UpgradeConstants.UPGRADE_DOWNLOAD_PATH_DIVE:UpgradeConstants.UPGRADE_DOWNLOAD_PATH)  + fileName);
        HttpConnUtil.doWIFIDownload(mContext, downloadUrl, file, isUseCellularNetwork, downloadListener);
    }

    private void downloasSuccess(File file) {
        mCurStep = DOWNLOAD_DONE;
        mView.downloadComplete();
        mUpgradeFile = file;
    }

    // firstReboot
    private void firstReboot(String apUrl) {
        Logger.e("FirmwareUPgrade    presenter    firstReboot " + apUrl);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject json = new JSONObject();

        try {
            json.put("sys", 3);

            if (mUpgradeType.equals(UpgradeConstants.DEV_F1_CAMERA)) {
                json.put("upgradedev", UpgradeConstants.DEV_ROV);
            } else {
                json.put("upgradedev", mUpgradeType);
            }
            json.put("rovid", 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //json为String类型的json数据
        RequestBody requestBody = RequestBody.create(JSON, String.valueOf(json));

        RxHttpRequest.getInstance(apUrl)
                .getUpgradeServer()
                .reboot(requestBody)
                .observeOn(Schedulers.io())// 这里不要在主线程
                .subscribeOn(Schedulers.io())
                .subscribe(new BaseUpgradeObserver<RebootBean>(mContext) {

                    @Override
                    public void onSuccess(RebootBean result) {// 成功返回rovip
                        mCurStep = FIRST_REBOOTING;
//                        mCurStep = UPLOAD_DONE;
                        // 开始检查rov状态
                        subHandler.sendEmptyMessage(FIRST_REBOOTING);
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        //mCurStep = FIRST_REBOOTING;
                        Logger.d("FirmwareUPgrade " + e.getMessage() + " " + errorMsg);
                        // 开始检查rov状态
                        subHandler.sendEmptyMessage(FIRST_REBOOTING);
//                        mView.uploadDefalut();
                        mView.showError(errorMsg);
//                        mView.showError(R.string.upload_fail);
                        mCurStep = DEFAULT;
//                        mCurStep = DOWNLOAD_DONE;

                    }
                });
    }

    private boolean isFirstUpload = true;
    private boolean isFirstRebootUpload = true;

    private void getAPStatusSync(final String baseUrl) {
        Logger.e("FirmwareUPgrade : AP状态请求开始 baseUrl " + baseUrl);
        RxHttpRequest.getInstance(baseUrl)
                .getUpgradeServer()
                .getStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new BaseUpgradeObserver<CameraStatusBean>(mContext) {
                    @Override
                    public void onSuccess(CameraStatusBean result) {
                        Logger.e("FirmwareUPgrade : AP状态请求成功 " + isFirstUpload + "  " + mCurStep + "  " + isFirstRebootUpload);
                        mUpgradeApStatus = result;
                        if (null != mUpgradeApStatus && mUpgradeApStatus.getSys() == 3
                                && mCurStep != FIRST_REBOOT) {
                            Logger.e("FirmwareUPgrade : sys状态 " + mUpgradeApStatus.getSys());
                            if (isFirstUpload) {
                                isFirstUpload = false;
                                mCurStep = START_UPLOAD;
                                uploadUpgradeFile(baseUrl);
                            }
                        } else {
                            if (mCurStep == FIRST_REBOOT) {
                                mCurStep = FIRST_REBOOTING;
                                firstReboot(baseUrl);
                            } else {
                                if (isFirstRebootUpload) {
                                    isFirstRebootUpload = false;
                                    mCurStep = START_UPLOAD;
                                    uploadUpgradeFile(baseUrl);
                                }
                            }
                            if (null != mUpgradeApStatus && mUpgradeApStatus.getSys() == 2) {
                                //上次升级未完成
                                if (mView != null) {
                                    mView.upgradeNoComplete();
                                }
                            }
                        }

                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        mUpgradeApStatus = null;
                        mCurStep = DOWNLOAD_DONE;
                        mView.downloadComplete();
                        Logger.d("FirmwareUPgrade  " + e.getMessage() + " errorMsg " + errorMsg);
                    }
                });
    }

    private void uploadUpgradeFile(String baseUrl) {
        if (mUpgradeFile == null || !mUpgradeFile.exists()) {
            mView.showError("");
//            mView.showError(R.string.no_files_upload);
            return;
        }
        if (mUpgradeType.equals(UpgradeConstants.DEV_F1_CAMERA)) {
            mCurStep = UPLOADING;
            subHandler.sendEmptyMessage(UPLOADING);
            mView.updateUploadProgress(0);
        }

        Logger.d("FirmwareUPgrade 文件上传 " + baseUrl + " mUpgradeFile.name " + mUpgradeFile.getName());
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("multipart/form-data"), mUpgradeFile);
        MultipartBody.Part jokeFile = MultipartBody.Part.createFormData(
                mUpgradeFile.getName(), mUpgradeFile.getName(), requestBody);
        RxHttpRequest.getInstance(baseUrl)
                .getUpgradeServer()
                .upgrade(jokeFile, mUpgradeFile.getName())
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(new BaseUpgradeObserver<Void>(mContext) {
                    @Override
                    public void onSuccess(Void result) {
                        Logger.d("FirmwareUPgrade 文件上传成功");
                        if (!mUpgradeType.equals(UpgradeConstants.DEV_F1_CAMERA)) {
                            mCurStep = UPLOADING;
                            subHandler.sendEmptyMessage(UPLOADING);
                            mView.updateUploadProgress(0);
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        // TODO  上传失败
                        Logger.d("FirmwareUPgrade 文件上传失败" + e.getMessage() + " errorMsg " + errorMsg);
                        mView.showError(errorMsg);
//                        mView.showError(R.string.upload_fail);
                        if (machineType == UpgradeConstants.DEVICES_DORY && mUpgradeType.equals(UpgradeConstants.DEV_AP))
                            mCurStep = FIRST_REBOOT;
                        else
                            mCurStep = DOWNLOAD_DONE;

                        mView.uploadDefalut();
                        isFirstUpload = true;
                        isFirstRebootUpload = true;
                    }
                });
    }

    /**
     * 升级状态 0-ready    1-busy     2-success     3-failed
     */
    private void getUpgradeStatus(String baseUrl) {
        Logger.e("FirmwareUPgrade    presenter    getUpgradeStatus" + baseUrl);
        RxHttpRequest.getInstance(baseUrl)
                .getUpgradeServer()
                .upgradeStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new BaseUpgradeObserver<UpgradeStatus>(mContext) {
                    @Override
                    public void onSuccess(UpgradeStatus result) {
                        Logger.e("FirmwareUPgrade    presenter    getUpgradeStatus   onSuccess" + "  mCurStep status " + result.getStatus());
                        if (result != null) {
                            mView.updateUploadProgress(result.getPercent());
                            // 空闲状态   且   percent == 100   升级成功,需要重启
                            if (result.getStatus() == -1) {// 出错终止
//                                mView.showUploadError(R.string.error_occurred);
                                mCurStep = DOWNLOAD_DONE;
                                Logger.e("FirmwareUPgrade    presenter    getUpgradeStatus 终止");
                                mView.updateUploadProgress(0);
                                return;
                            } else if (result.getStatus() == 0 && result.getPercent() == 100) {
                                // 提示重启
                                mCurStep = UPLOAD_DONE;
                                mView.uploadComplete();
                                Logger.e("FirmwareUPgrade    presenter    getUpgradeStatus    百分百");
                                return;
                            }
                            if (result.getPercent() != 100) {
                                Logger.e("FirmwareUPgrade    presenter    getUpgradeStatus    百分百===" + result.getPercent());
                                subHandler.sendEmptyMessageDelayed(UPLOADING, 1000);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        Logger.e("FirmwareUPgrade    presenter" + errorMsg);

                        //subHandler.sendEmptyMessageDelayed(UPLOADING,1000);
                    }
                });
    }

    private void getNewRovVersion(String baseUrl) {
        RxHttpRequest.getInstance(baseUrl)
                .getUpgradeServer()
                .getVersion()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BaseUpgradeObserver<Versions>(mContext) {
                    @Override
                    public void onSuccess(Versions result) {
                        if (result != null) {
                            mView.upgradeVersionsData(result);
//                            Constants.rovCurVersion = result.getRov();
//                            Logger.d(" getNewVersion rovCurVersion  "+Constants.rovCurVersion);
                        } else {
                            subHandler.sendEmptyMessageDelayed(UPGRADE_SUCCESS, 1000);
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        mView.showError(errorMsg);
                        subHandler.sendEmptyMessageDelayed(UPGRADE_SUCCESS, 1000);
                    }
                });
    }

    //  secondReboot
    public void secondReboot(String baseUrl) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject json = new JSONObject();

        try {
            json.put("sys", 2);

            if (mUpgradeType.equals(UpgradeConstants.DEV_F1_CAMERA)) {
                json.put("upgradedev", UpgradeConstants.DEV_ROV);
            } else {
                json.put("upgradedev", mUpgradeType);
            }
            json.put("rovid", 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //json为String类型的json数据
        RequestBody requestBody = RequestBody.create(JSON, String.valueOf(json));

        RxHttpRequest.getInstance(baseUrl)
                .getUpgradeServer()
                .reboot(requestBody)
                .observeOn(Schedulers.io())// 这里不要在主线程
                .subscribeOn(Schedulers.io())
                .subscribe(new BaseUpgradeObserver<RebootBean>(mContext) {
                    @Override
                    public void onSuccess(RebootBean result) {
                        mCurStep = UPGRADE_SUCCESS;
//                        mCurStep = SECOND_REBOOT_SUCCESS;

                        mView.upgradeComplete();
                        subHandler.sendEmptyMessageDelayed(UPGRADE_SUCCESS, 4000);
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        mView.showError(errorMsg);
                    }
                });

    }


    private boolean getNewVRxHttpersion(final String rovOnlineVersion, final String rovCurVersion) {
        final boolean[] isContinue = {false};
        RxHttpRequest.getInstance(UpgradeConstants.AP_BASEURL)
                .getUpgradeServer()
                .getVersion()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BaseUpgradeObserver<Versions>(mContext) {
                    @Override
                    public void onSuccess(Versions result) {
                        if (result != null) {
                            if ("dory".equals(result.getModel())) {
                                // 检测升级是否完成,去除标记
                                if (!TextUtils.isEmpty(rovOnlineVersion)) {
                                    int rovOnline = Utils.versionName2Code(rovOnlineVersion);
                                    int rovCur = Utils.versionName2Code(rovCurVersion);
                                    if (rovOnline > rovCur) {// 需要更新
                                        isContinue[0] = true;
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String errorMsg) {
                        isContinue[0] = false;
                    }
                });
        return isContinue[0];
    }


}
