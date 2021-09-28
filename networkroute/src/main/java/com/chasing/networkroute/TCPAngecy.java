package com.chasing.networkroute;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author olivia
 * @date 2021/9/9 14:49
 * <p>
 * TCP  转发
 */
public class TCPAngecy extends BaseAngecy implements Runnable {
    private Thread mSelfThread;
    private Thread forwardNo1;
    private Thread forwardNo2;
    private HandlerThread watchdog;
    private Handler watchdogHandler;

    /**
     * 代理是否可以执行
     */
    private boolean canWork = false;
    private AngecyParams mAngecyParams;

    private Socket appClientSocket = null;
    private Socket angecyClientSocket = null;
    private ServerSocket serverSocket;

    public TCPAngecy(AngecyParams angecyParams) {
        mAngecyParams = angecyParams;
    }

    /**
     * 开始代理工作
     */
    public void start() {
        mSelfThread = new Thread(this);
        mSelfThread.start();
    }

    @Override
    public void run() {
        while (!canWork) {
            try {
                serverSocket = new ServerSocket(angecyPortFlag);
                canWork = true;
                if (mAngecyActionListener != null)
                    mAngecyActionListener.onAngecyStarted(angecyPortFlag);
            } catch (IOException e) {
                e.printStackTrace();
                if (mAngecyActionListener != null)
                    mAngecyActionListener.onWarning("No available port: " + e.getMessage());
            }
            synchronized (this) {
                angecyPortFlag += 2;
            }
        }
        if (!serverSocket.isClosed()) {
            try {
                // 等待APP端客户端连接接入, 会阻塞当前线程
                appClientSocket = serverSocket.accept();
                // 开始连接检查
                startWatchdog();
                // 开始创建代理客户端进行数据转发
                try {
                    Log.d("代理转发测试", "get  angecyClientSocket  before");
                    angecyClientSocket = mAngecyParams.activityNetwork.getSocketFactory()
                            .createSocket(mAngecyParams.ip, mAngecyParams.port);
                    angecyClientSocket.setKeepAlive(true);
                    Log.d("代理转发测试", "get  angecyClientSocket  after");
                    // 开启转发
                    startForward(appClientSocket, angecyClientSocket);

                } catch (IOException e) {
                    e.printStackTrace();
                    if (mAngecyActionListener != null)
                        mAngecyActionListener.onError("create tcp clientsocket error: " + e.getMessage());
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (mAngecyActionListener != null)
                    mAngecyActionListener.onError("Wait client socket error: " + e.getMessage());
            }
        }
    }

    private void startWatchdog() {
        if (watchdog == null) {
            watchdog = new HandlerThread("tcp-angecy-watchdog" + mAngecyParams.port);
            watchdog.start();
            watchdogHandler = new Handler(watchdog.getLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    try {
                        if (angecyClientSocket != null) {
                            OutputStream out = angecyClientSocket.getOutputStream();
                            OutputStreamWriter outWriter = new OutputStreamWriter(out);
                            outWriter.write(65); // 向服务器发送字符"A"
                            outWriter.flush();
                            Log.d("代理转发测试", "   angecyClientSocket.sendUrgentData");
                        }
                        watchdogHandler.sendEmptyMessageDelayed(0, 1000);
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (mAngecyActionListener!=null)
                            mAngecyActionListener.onAngecyClosed();
                        // 表示和外部设备连接已经断开, 此时主动断开和APP的连接
                        stop();
                    }
                }
            };
            watchdogHandler.sendEmptyMessageDelayed(0, 1000);
        }
    }

    private void startForward(Socket appClientSocket, Socket angecyClientSocket) {
        InputStream clientIn;
        OutputStream clientOut;
        InputStream serverIn;
        OutputStream serverOut;
        try {
            clientIn = angecyClientSocket.getInputStream();
            clientOut = angecyClientSocket.getOutputStream();
            serverIn = appClientSocket.getInputStream();
            serverOut = appClientSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            if (mAngecyActionListener != null)
                mAngecyActionListener.onError("Forward start error: " + e.getMessage());
            return;
        }
        Log.d("代理转发测试", "startForward:   ");

        if (forwardNo1 == null) {
            forwardNo1 = new Thread("tcp-forward-no.1") {
                @Override
                public void run() {
                    Log.d("代理转发测试", "forwardNo1:   run");

                    forward(clientIn, serverOut);
                }
            };
        }
        forwardNo1.start();
        if (forwardNo2 == null) {
            forwardNo2 = new Thread("tcp-forward-no.2") {
                @Override
                public void run() {
                    Log.d("代理转发测试", "forwardNo2:   run");

                    forward(serverIn, clientOut);
                }
            };
        }
        forwardNo2.start();
    }

    private void forward(InputStream is, OutputStream os) {
        byte[] buffer = new byte[8192];
        try {
            while (true) {
                Log.d("代理转发测试", "forward    while   ");
                int bytesRead = is.read(buffer);
                Log.d("代理转发测试", "forward    while   after   read");
                if (bytesRead != -1) {
                    os.write(buffer, 0, bytesRead);
                    os.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (mAngecyActionListener != null)
                mAngecyActionListener.onError("read/write failed: " + e.getMessage());
        }
    }

    /**
     * Stops the tunneling application (no more waiting for new connections to open tunnels).
     */
    public void stop() {
        canWork = false;
        try {
            if (appClientSocket != null) {
                appClientSocket.close();
                appClientSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (mAngecyActionListener != null)
                mAngecyActionListener.onError("Error closing app client socket: " + e.getMessage());
        }

        try {
            if (angecyClientSocket != null) {
                angecyClientSocket.close();
                angecyClientSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (mAngecyActionListener != null)
                mAngecyActionListener.onError("Error closing angecy client socket: " + e.getMessage());
        }

        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            if (mAngecyActionListener != null)
                mAngecyActionListener.onError("Error closing server socket: " + e.getMessage());
        }
    }
}

