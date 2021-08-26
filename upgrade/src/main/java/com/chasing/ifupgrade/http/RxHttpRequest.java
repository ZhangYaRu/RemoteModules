package com.chasing.ifupgrade.http;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RxHttpRequest {
    private static String BASE_URL = "";
    private Map<String, Retrofit> mRetrofitMap = new HashMap<>();

    private RxHttpRequest() {
    }

    /**
     * 单例模式
     *
     * @return
     */
    public static RxHttpRequest getInstance(String baseurl) {
        BASE_URL = baseurl;
        return RxHttpHolder.sInstance;
    }

    private static class RxHttpHolder {
        private final static RxHttpRequest sInstance = new RxHttpRequest();
    }

    private Retrofit getRetrofit(String serverUrl) {
        Retrofit retrofit;
        if (mRetrofitMap.containsKey(serverUrl)) {
            retrofit = mRetrofitMap.get(serverUrl);
        } else {
            retrofit = createRetrofit(serverUrl);
            mRetrofitMap.put(serverUrl, retrofit);
        }
        return retrofit;
    }

    public UpdateService getUpgradeServer() {
        return getRetrofit(BASE_URL).create(UpdateService.class);
    }
//
//    public F1CameraService getF1CameraServer() {
//        return getRetrofit(BASE_URL).create(F1CameraService.class);
//    }
//
//    public UserService getUserServer() {
//        return getRetrofit(BASE_URL).create(UserService.class);
//    }
//
//    public OnlineReleaseAPI getVersionServer() {
//        return getRetrofit(BASE_URL).create(OnlineReleaseAPI.class);
//    }
//
//    public OnlineDebugAPI getDVersionServer() {
//        return getRetrofit(BASE_URL).create(OnlineDebugAPI.class);
//    }

    /**
     * @param baseUrl baseUrl要以/作为结尾 eg：https://github.com/
     * @return
     */
    private Retrofit createRetrofit(String baseUrl) {
        HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor();
        logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(500, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);
//        if (Constants.isDebug) {
//            builder.addInterceptor(logInterceptor);
//        }
        OkHttpClient client = builder.build();

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(client)
                .build();
    }
}
