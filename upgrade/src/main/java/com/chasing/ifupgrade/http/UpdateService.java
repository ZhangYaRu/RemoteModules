package com.chasing.ifupgrade.http;

import com.chasing.ifupgrade.bean.CameraStatusBean;
import com.chasing.ifupgrade.bean.RebootBean;
import com.chasing.ifupgrade.bean.ResponseBean;
import com.chasing.ifupgrade.bean.UpgradeStatus;
import com.chasing.ifupgrade.bean.Versions;
import com.chasing.ifupgrade.constants.UpgradeConstants;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface UpdateService {
    @POST(UpgradeConstants.HTTP_VERSION_1 + "/reboot")
    Observable<ResponseBean<RebootBean>> reboot(@Body RequestBody body);// always   1
    @GET(UpgradeConstants.HTTP_VERSION_1 + "/upgrade/status")
    Observable<ResponseBean<UpgradeStatus>> upgradeStatus();
    @Multipart
    @POST(UpgradeConstants.HTTP_VERSION_1 + "/upgrade")
    Observable<ResponseBean<Void>> upgrade(@Part MultipartBody.Part parts, @Part("upgrade") String filename);
    @GET(UpgradeConstants.HTTP_VERSION_1 + "/status")
    Observable<ResponseBean<CameraStatusBean>> getStatus();
    @GET(UpgradeConstants.HTTP_VERSION_1 + "/versions")
    Observable<ResponseBean<Versions>> getVersion();
}
