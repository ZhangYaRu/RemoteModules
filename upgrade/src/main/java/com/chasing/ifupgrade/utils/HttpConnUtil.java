package com.chasing.ifupgrade.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.orhanobut.logger.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpConnUtil {

    private static ExecutorService executorService;

    static {
        executorService = Executors.newCachedThreadPool();
    }

    public static void doGet(final Context context, final String requestUrl, final boolean isUseCellularNetwork, final OnHttpConnListener listener) {
        Runnable runnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                // 指定某个网络请求采用指定的网络访问
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                        Context.CONNECTIVITY_SERVICE);
                NetworkRequest.Builder req = new NetworkRequest.Builder();
                req.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                req.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
                ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        try {
                            URL url = new URL(requestUrl);
                            HttpURLConnection urlConnection;
                            if (isUseCellularNetwork) {
                                urlConnection = (HttpURLConnection) network.openConnection(url);
                            } else {
                                urlConnection = (HttpURLConnection) url.openConnection();
                            }
                            urlConnection.setRequestMethod("GET"); //提交模式
                            urlConnection.connect();
                            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                InputStream is = urlConnection.getInputStream();
                                byte[] buffer = new byte[1024];
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                for (int len = 0; (len = is.read(buffer)) > 0; ) {
                                    baos.write(buffer, 0, len);
                                }
                                String returnValue = new String(baos.toByteArray(), "utf-8");
                                baos.flush();
                                baos.close();
                                is.close();
                                if (listener != null) {
                                    listener.onHttpConnSuccessed(returnValue);
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (listener != null) {
                            listener.onHttpConnFailed();
                        }
                    }
                };
            }
        };
        executorService.execute(runnable);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void doPost(final Context context, final String requestUrl, final String
            params, final boolean isUseCellularNetwork, final OnHttpConnListener listener) {
        // 指定某个网络请求采用指定的网络访问
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder req = new NetworkRequest.Builder();
        req.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        req.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                try {
                    //建立连接
                    URL url = new URL(requestUrl);
                    HttpURLConnection connection;
                    if (isUseCellularNetwork) {
                        connection = (HttpURLConnection) network.openConnection(url);
                    } else {
                        connection = (HttpURLConnection) url.openConnection();
                    }
                    //设置连接属性
                    connection.setDoOutput(true); //使用URL连接进行输出
                    connection.setDoInput(true); //使用URL连接进行输入
                    connection.setUseCaches(false); //忽略缓存
                    connection.setRequestMethod("POST"); //设置URL请求方法
                    //String requestString = requestbody;

                    //设置请求属性
                    if (params != null) {
                        byte[] requestStringBytes = params.getBytes(); //获取数据字节数据
                        connection.setRequestProperty("Content-length", "" + requestStringBytes.length);
                        connection.setRequestProperty("Content-Type", "application/octet-stream");
                        connection.setRequestProperty("Connection", "Keep-Alive");// 维持长连接
                        connection.setRequestProperty("Charset", "UTF-8");

                        connection.setConnectTimeout(8000);
                        connection.setReadTimeout(8000);

                        //建立输出流,并写入数据
                        OutputStream outputStream = connection.getOutputStream();
                        outputStream.write(requestStringBytes);
                        outputStream.close();
                    }

                    //获取响应状态
                    int responseCode = connection.getResponseCode();
                    if (HttpURLConnection.HTTP_OK == responseCode) { //连接成功
                        //当正确响应时处理数据
                        StringBuffer buffer = new StringBuffer();
                        String readLine;
                        BufferedReader responseReader;
                        //处理响应流
                        responseReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        while ((readLine = responseReader.readLine()) != null) {
                            buffer.append(readLine).append("\n");
                        }
                        responseReader.close();
                        Log.d("HttpPOST", buffer.toString());
                        if (listener != null) {
                            listener.onHttpConnSuccessed(buffer.toString());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (listener != null) {
                    listener.onHttpConnFailed();
                }
            }
        };
    }

    public static void doDownload(final Context context, final String httpUrl, final File file, final boolean isUseCellularNetwork, final OnHttpDownloadListener listener) {
        Runnable runnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                final boolean[] first = {true};
                // 指定某个网络请求采用指定的网络访问
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                        Context.CONNECTIVITY_SERVICE);
                NetworkRequest.Builder req = new NetworkRequest.Builder();
                req.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                req.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
                ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        if (first[0]) {
                            Logger.d("文件的大小是 开始下载（流量）");
                            first[0] = false;
                            BufferedInputStream bis = null;
                            BufferedOutputStream bos = null;
                            try {
                                URL url = new URL(httpUrl);
                                HttpURLConnection connection;
                                if (isUseCellularNetwork) {
                                    connection = (HttpURLConnection) network.openConnection(url);
                                } else {
                                    connection = (HttpURLConnection) url.openConnection();
                                }
                                connection.setRequestMethod("GET");
                                connection.setRequestProperty("Content-Type", "application/octet-stream");
//                                connection.setDoOutput(true);
                                connection.setDoInput(true);
                                connection.setConnectTimeout(8000);
                                connection.setReadTimeout(8000);
//                                connection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
//                                connection.setRequestProperty("Accept","*/*");
                                connection.setRequestProperty("Connection", "Keep-Alive");
                                connection.connect();
                                int responseCode = connection.getResponseCode();
                                Logger.d("文件的大小是 responseCode（流量） "+responseCode);
                                if (responseCode == 301) {
                                    String location = connection.getHeaderField("Location");
                                    url  = new URL(location);

                                    if (isUseCellularNetwork) {
                                        connection = (HttpURLConnection) network.openConnection(url);
                                    } else {
                                        connection = (HttpURLConnection) url.openConnection();
                                    }
                                    connection.setRequestMethod("GET");
                                    connection.setRequestProperty("Content-Type", "application/octet-stream");
//                                    connection.setDoOutput(true);
                                    connection.setDoInput(true);
                                    connection.setConnectTimeout(8000);
                                    connection.setReadTimeout(8000);
//                                    connection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
//                                    connection.setRequestProperty("Accept","*/*");
                                    connection.setRequestProperty("Connection", "Keep-Alive");
                                    connection.connect();
                                }
                                int contentLength = connection.getContentLength();
                                Logger.e("文件的大小是:" + contentLength);
                                if (listener != null) {
                                    listener.onStartDownload(contentLength);
                                }
                                InputStream is = connection.getInputStream();
                                bis = new BufferedInputStream(is);
                                if (!file.exists()) {
                                    file.getParentFile().mkdirs();
                                    file.createNewFile();
                                } else {
                                    FileInputStream fis = new FileInputStream(file);
                                    FileChannel fc = fis.getChannel();
                                    long size = fc.size();
                                    if (size == contentLength) {
                                        if (listener != null) {
                                            listener.onProgress(100);
                                            listener.onDownloadSuccessed(file);
                                        }
                                        return;
                                    }
                                    Logger.e("文件的大小是:  文件下载 size" + size);
                                }
                                FileOutputStream fos = new FileOutputStream(file);
                                bos = new BufferedOutputStream(fos);
                                int b = 0;
                                int done = 0;
                                byte[] byArr = new byte[1024];
                                while ((b = bis.read(byArr)) != -1) {
                                    bos.write(byArr, 0, b);
                                    done += b;
                                    if (listener != null) {
                                        listener.onProgress((done * 100 / contentLength));
                                    }
                                }
                                Logger.e("下载的文件的大小是----------------------------------------------:" + contentLength);
                                if (listener != null) {
                                    listener.onDownloadSuccessed(file);
                                    return;
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                Logger.e(" 文件的大小是 exception: "+e.getMessage());
                            } finally {
                                try {
                                    if (bis != null) {
                                        bis.close();
                                    }
                                    if (bos != null) {
                                        bos.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Logger.e(" 文件的大小是2 exception: "+e.getMessage());
                                }
                            }
                            if (listener != null) {
                                listener.onDownloadFailed();
                            }
                        }
                    }
                };
                cm.requestNetwork(req.build(), callback);

            }
        };
        executorService.execute(runnable);
    }

    public static void doWIFIDownload(final Context context, final String httpUrl, final File file, final boolean isUseCellularNetwork, final OnHttpDownloadListener listener) {
        Runnable runnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                final boolean[] first = {true};

                // 指定某个网络请求采用指定的网络访问
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                        Context.CONNECTIVITY_SERVICE);
                NetworkRequest.Builder req = new NetworkRequest.Builder();
                req.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                req.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
                ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        if (first[0]) {
                            Logger.d("文件的大小是 开始流量下载  未报错");
                            first[0] = false;
                            BufferedInputStream bis = null;
                            BufferedOutputStream bos = null;
                            try {
                                URL url = new URL(httpUrl);
                                HttpURLConnection connection;
                                if (isUseCellularNetwork) {
                                    connection = (HttpURLConnection) network.openConnection(url);
                                } else {
                                    connection = (HttpURLConnection) url.openConnection();
                                }
                                connection.setRequestMethod("GET");
                                connection.setRequestProperty("Content-Type", "application/octet-stream");
//                                connection.setDoOutput(true);
                                connection.setDoInput(true);
                                connection.setConnectTimeout(8000);
                                connection.setReadTimeout(8000);
                                connection.setRequestProperty("Connection", "Keep-Alive");
                                connection.connect();
                                int responseCode = connection.getResponseCode();
                                Logger.d("文件的大小是 responseCode "+ responseCode);
                                if ( responseCode== 301) {
                                    Logger.d("文件的大小是 开始流量下载 301了");
                                    String location = connection.getHeaderField("Location");
                                    url  = new URL(location);
                                    if (isUseCellularNetwork) {
                                        connection = (HttpURLConnection) network.openConnection(url);
                                    } else {
                                        connection = (HttpURLConnection) url.openConnection();
                                    }
                                    connection.setRequestMethod("GET");
                                    connection.setRequestProperty("Content-Type", "application/octet-stream");
//                                    connection.setDoOutput(true);
                                    connection.setDoInput(true);
                                    connection.setConnectTimeout(8000);
                                    connection.setReadTimeout(8000);
                                    connection.setRequestProperty("Connection", "Keep-Alive");
                                    connection.connect();
                                }
                                int contentLength = connection.getContentLength();
                                Logger.e("文件的大小是:" + contentLength+" file "+file);
                                if (listener != null) {
                                    listener.onStartDownload(contentLength);
                                }
                                InputStream is = connection.getInputStream();
                                bis = new BufferedInputStream(is);
                                if (!file.exists()) {
                                    file.getParentFile().mkdirs();
                                    file.createNewFile();
                                } else {
                                    FileInputStream fis = new FileInputStream(file);
                                    FileChannel fc = fis.getChannel();
                                    long size = fc.size();
                                    if (size == contentLength) {
                                        if (listener != null) {
                                            listener.onProgress(100);
                                            listener.onDownloadSuccessed(file);
                                        }
                                        return;
                                    }
                                }
                                FileOutputStream fos = new FileOutputStream(file);
                                bos = new BufferedOutputStream(fos);
                                int b = 0;
                                int done = 0;
                                byte[] byArr = new byte[1024];
                                while ((b = bis.read(byArr)) != -1) {
                                    bos.write(byArr, 0, b);
                                    done += b;
                                    if (listener != null) {
                                        listener.onProgress((done * 100 / contentLength));
                                    }
                                }
                                Logger.e("下载的文件的大小是----------------------------------------------:" + contentLength);
                                if (listener != null) {
                                    listener.onDownloadSuccessed(file);
                                    return;
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                Logger.e(e.getMessage());
                                Logger.d("文件的大小是 WiFi下载 exception: "+e.getMessage());
                            } finally {
                                try {
                                    if (bis != null) {
                                        bis.close();
                                    }
                                    if (bos != null) {
                                        bos.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Logger.d("文件的大小是 WiFi下载1 exception: "+e.getMessage());
                                }
                            }
                            if (listener != null) {
                                listener.onDownloadFailed();
                            }
                        }
                    }
                };
                cm.requestNetwork(req.build(), callback);

            }
        };
        executorService.execute(runnable);
    }

    public interface OnHttpConnListener {
        void onHttpConnSuccessed(String response);

        void onHttpConnFailed();
    }

    public interface OnHttpDownloadListener {
        void onDownloadSuccessed(File response);

        void onStartDownload(int all);

        void onProgress(int progress);

        void onDownloadFailed();
    }
}
