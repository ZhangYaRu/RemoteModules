package com.chasing.networkroute;

import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

import static com.chasing.networkroute.RouteConfig.LOCAL_ADDRESS;

/**
 * @author olivia
 * @date 2021/9/9 14:49
 * <p>
 * UDP  转发
 */
public class UDPAngecy extends BaseAngecy implements Runnable {
    private Thread mSelfThread;
    private Thread forwardNo1;

    /**
     * 代理是否可以执行
     */
    private boolean canWork = false;
    private AngecyParams mAngecyParams;

    private DatagramSocket localListenerSocket = null;
    private Thread thread;

    public UDPAngecy(AngecyParams angecyParams) {
        mAngecyParams = angecyParams;
    }

    /**
     * 开始代理工作
     */
    public void start() {
        mSelfThread = new Thread(this);
        mSelfThread.start();
        Log.d("代理转发测试", "udpangecy:   start   ===");

        thread = new Thread(sendRemoteRunnable);
        thread.start();
        new Thread(sendLocalRunnable).start();
    }

    @Override
    public void run() {
        while (!canWork) {
            Log.d("代理转发测试", "udpangecy:   run   ===");
            synchronized (UDPAngecy.this) {
                try {
                    Log.d("代理转发测试", "udpangecy:   run =" + angecyPortFlag);
                    localListenerSocket = new DatagramSocket(angecyPortFlag);
                    if (mAngecyActionListener != null)
                        mAngecyActionListener.onAngecyStarted(angecyPortFlag);
                    canWork = true;
                    // TODO  测试测试   特定网路转发
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
//                        mAngecyParams.activityNetwork.bindSocket(localListenerSocket);
//                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (mAngecyActionListener != null)
                        mAngecyActionListener.onError("UDPAngecy-remote Socket create error: " + e.getMessage());
                }
                synchronized (BaseAngecy.class) {
                    angecyPortFlag += 2;
                }
            }
        }

        // 开启转发
        startForward();
    }

    private void startForward() {
        if (forwardNo1 == null) {
            forwardNo1 = new Thread("udp-forward1-" + mAngecyParams.ip + ":" + mAngecyParams.port) {
                @Override
                public void run() {
                    while (true) {
                        byte[] buffer1 = new byte[1024 * 5];
                        DatagramPacket receiveFromAngecyPortPacket = new DatagramPacket(buffer1, buffer1.length);
                        try {
                            localListenerSocket.receive(receiveFromAngecyPortPacket);
                            Log.d("代理转发测试", "forwardNo1:   run =" + receiveFromAngecyPortPacket.getAddress().getHostAddress()
                                    + "===" + receiveFromAngecyPortPacket.getPort());
                        } catch (IOException e) {
                            if (mAngecyActionListener != null)
                                mAngecyActionListener.onError("UDPAngecy-receive error: " + e.getMessage());
                            e.printStackTrace();
                            continue;
                        }
                        // 路由转发
                        try {
                            // 包根据地址做分发
                            byte[] address = receiveFromAngecyPortPacket.getAddress().getAddress();
                            String hostName = "";
                            if (address.length > 3) {
                                hostName = (address[0] & 0xFF) + "."
                                        + (address[1] & 0xFF) + "."
                                        + (address[2] & 0xFF) + "."
                                        + (address[3] & 0xFF);
                            }
                            if (LOCAL_ADDRESS.equals(hostName) || "localhost".equals(hostName)) {
                                receiveFromAngecyPortPacket.setAddress(InetAddress.getByName(mAngecyParams.ip));
                                receiveFromAngecyPortPacket.setPort(mAngecyParams.port);
                                mLocalReceivedPackets.put(receiveFromAngecyPortPacket);
//                                Log.d("代理转发测试", "接收本地包:" + mAngecyParams.ip + "===" + mAngecyParams.port
//                                        + "=======================================");
                            } else {// 接收到远程包
                                receiveFromAngecyPortPacket.setAddress(InetAddress.getByName(LOCAL_ADDRESS));
                                receiveFromAngecyPortPacket.setPort(mAngecyParams.localPort);
                                mRemoteReceivedPackets.put(receiveFromAngecyPortPacket);
//                                Log.d("代理转发测试", "接收远程包:127.0.0.1===7788"
//                                        + "+++++++++++++++++++++++++++++++++++++++++"+mRemoteReceivedPackets.size() +"==="+thread.isInterrupted());
                            }
                        } catch (UnknownHostException | InterruptedException e) {
                            e.printStackTrace();
                            if (mAngecyActionListener != null)
                                mAngecyActionListener.onError("UDPAngecy-receive form app error: " + e.getMessage());
                        }
                    }
                }
            };
        }
        forwardNo1.start();
    }

    private final LinkedBlockingQueue<DatagramPacket> mLocalReceivedPackets = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<DatagramPacket> mRemoteReceivedPackets = new LinkedBlockingQueue<>();
    private final Thread sendLocalRunnable = new Thread() {

        @Override
        public void run() {
            try {
                while (true) {
                    DatagramPacket localPacket = mLocalReceivedPackets.take();
                    if (localPacket != null) {
                        if (localListenerSocket != null)
                            localListenerSocket.send(localPacket);
//                        Log.d("代理转发测试", "发往 远程 的 包:  =======================================");
                    }
                }
            } catch (InterruptedException e) {
                Log.e("exception", "Dispatching received data thread was interrupted.");
            } catch (IOException e) {
                Log.e("exception", "udpTunnel   send error");
            }
        }
    };

    private final Runnable sendRemoteRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                while (true) {
//                    Log.d("代理转发测试", "sendRemoteRunnable   take+++++++++++++++++++++++++++++++++++++++++");
                    DatagramPacket remotePacket = mRemoteReceivedPackets.poll();
                    if (remotePacket != null) {
                        byte[] address = remotePacket.getAddress().getAddress();
                        String hostName = "";
                        if (address.length > 3) {
                            hostName = (address[0] & 0xFF) + "."
                                    + (address[1] & 0xFF) + "."
                                    + (address[2] & 0xFF) + "."
                                    + (address[3] & 0xFF);
                        }

                        if (localListenerSocket != null)
                            localListenerSocket.send(remotePacket);
                        Log.d("代理转发测试", "发往 本地 的 包:" + hostName + ":" + remotePacket.getPort() + "  +++++++++++++++++++++++++++++++++++++++++");

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                Log.e("exception", "udpTunnel   send error");
            }
        }
    };


    /**
     * Stops the tunneling application (no more waiting for new connections to open tunnels).
     */
    public void stop() {
        canWork = false;
        if (localListenerSocket != null) {
            localListenerSocket.close();
            localListenerSocket = null;
        }
    }
}

