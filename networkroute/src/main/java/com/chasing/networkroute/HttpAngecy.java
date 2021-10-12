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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author olivia
 * @date 2021/9/9 14:49
 * <p>
 * TCP  转发
 */
public class HttpAngecy extends BaseAngecy implements Runnable {
    private Thread mSelfThread;
    private ThreadPoolExecutor mThreadPool;
    private ForwardRunnable forwardNo1;
    private ForwardRunnable forwardNo2;

    class ForwardRunnable implements Runnable{
        InputStream is;
        OutputStream os;

        public InputStream getIs() {
            return is;
        }

        public void setIs(InputStream is) {
            this.is = is;
        }

        public OutputStream getOs() {
            return os;
        }

        public void setOs(OutputStream os) {
            this.os = os;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[8192];
            try {
                while (true) {
//                    Log.d("代理转发测试3", "forward    while   ");
                    int bytesRead = is.read(buffer);
                    Log.d("代理转发测试3", "forward    while   after   read");
                    if (bytesRead != -1) {
                        os.write(buffer, 0, bytesRead);
                        os.flush();
                        Log.d("代理转发测试3", "forward    bytesRead:" + new String(buffer, 0, bytesRead));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("代理转发测试3", "forward    IOException:" + e.getMessage());
                if (mAngecyActionListener != null)
                    mAngecyActionListener.onError("read/write failed: " + e.getMessage());
            }
        }
    };

    /**
     * 代理是否可以执行
     */
    private boolean canWork = false;
    private AngecyParams mAngecyParams;

    private Socket appClientSocket = null;
    private Socket angecyClientSocket = null;
    private ServerSocket serverSocket;

    public HttpAngecy(AngecyParams angecyParams) {
        mAngecyParams = angecyParams;
        mThreadPool = new ThreadPoolExecutor(20, 20,
                0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(20));
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
        while (canWork) {
            try {
                // 等待APP端客户端连接接入, 会阻塞当前线程
                appClientSocket = serverSocket.accept();
                // 开始创建代理客户端进行数据转发
                try {
                    Log.d("代理转发测试", "get  angecyClientSocket  before");
                    angecyClientSocket = mAngecyParams.activityNetwork.getSocketFactory()
                            .createSocket(mAngecyParams.ip, mAngecyParams.port);
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

        forwardNo1 = new ForwardRunnable();
        forwardNo1.setIs(clientIn);
        forwardNo1.setOs(serverOut);
        mThreadPool.execute(forwardNo1);
        forwardNo2 = new ForwardRunnable();
        forwardNo2.setIs(serverIn);
        forwardNo2.setOs(clientOut);
        mThreadPool.execute(forwardNo2);
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

