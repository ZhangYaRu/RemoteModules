package com.chasing.networkroute.forwarder;

import android.content.Context;
import android.net.Network;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.chasing.networkroute.AngencyBean;
import com.chasing.networkroute.Main;
import com.chasing.networkroute.Params;
import com.chasing.networkroute.RouteConfig;
import com.chasing.networkroute.observers.TCPObserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.BufferOverflowException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by AlexZhuo on 2017/11/12.
 */
public class UDPTunnel extends Thread {

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MMM.dd HH:mm:ss");
    /**
     * Configuration parameters.
     */
    private final Params params;
    private boolean active = false;
    private DatagramSocket clientSocket;
    private Network mNetwork;
    private Context mContext;
    private Main.DiscoveringListener mDisConveringListener;
    private ArrayList<String> tunnelPool = new ArrayList<>();


    public UDPTunnel(Context context, Params params, DatagramSocket clientSocket,
                     Network network, Main.DiscoveringListener discoveringListener) {
        this.mContext = context;
        this.params = params;
        this.clientSocket = clientSocket;
        this.mNetwork = network;
        this.mDisConveringListener = discoveringListener;
        if (RouteConfig.isDebug)
            Log.d("重连测试", "UDPTunnel   構造   clientSocket=" + clientSocket);

        HandlerThread thread = new HandlerThread("udptunnel-sendremote");
        thread.start();
        subHandler = new Handler(thread.getLooper());

        HandlerThread thread1 = new HandlerThread("udptunnel-sendlocal");
        thread1.start();
        subHandler1 = new Handler(thread1.getLooper());

    }

    private int remotePort = 8000;


    public synchronized void run() {
        if (RouteConfig.isDebug)
            Log.d("重连测试", "UDPTunnel   run   params.getRemoteHost()="
                    + params.getRemoteHost() + "===" + params.getRemotePort());
        active = true;

        // 启动转发端
        while (active) {

            if (interrupted() || !active) {
                active = false;
                break;
            }
            try {
                byte[] buffer = new byte[65536];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//                Log.d("重连测试", "UDPTunnel   run：  while: clientSocket="+clientSocket);
                if (clientSocket == null) {
                    if (RouteConfig.isDebug)
                        Log.d("重连测试", "UDPTunnel   clientSocket   null");
                    return;
                }

//                    packet.setData(buffer);
//                    packet.setLength(buffer.length);
                try {
                    if (!clientSocket.isClosed()) {
                        clientSocket.receive(packet);
                    }
                } catch (Exception socketException) {
                    socketException.printStackTrace();
                }

//                    if (RouteConfig.isDebug)
//                        Log.d("网络转发测试", "阻塞问题   rov  数据发送  UDPTunnel   接收包==" + packet.getAddress().getHostName() + ":" + packet.getPort());
                InetAddress sourceAddress = packet.getAddress();
                int packetSize = packet.getLength();
                if (sourceAddress != null && packetSize > 0) {
                    //                    if (params.getRemoteHost().equals(sourceAddress.getHostName())) {
                    if (RouteConfig.LOCAL_ADDRESS.equals(sourceAddress.getHostName()) ||
                            "localhost".equals(sourceAddress.getHostName())) {
                        if (RouteConfig.isDebug)
                            Log.d("网络转发测试", "阻塞问题   rov  数据发送  UDPTunnel   本地包==" + remotePort);
                        DatagramPacket datagramPacket = new DatagramPacket(buffer, 0, packetSize,
                                InetAddress.getByName(params.getRemoteHost()), remotePort);
//                        mLocalReceivedPackets.offer(datagramPacket);
                        if (active) {
                            try {
                                mLocalReceivedPackets.put((datagramPacket));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                interrupt();
                                active = false;
                                return;
                            }
                        }


//                            Log.d("网络转发测试1      ", bytesToHexString(datagramPacket.getData()));

                    } else {
                        if (RouteConfig.isDebug)
                            Log.d("网络转发测试", "阻塞问题   rov  数据发送  UDPTunnel   远程包==" + remotePort);
                        DatagramPacket datagramPacket = new DatagramPacket(buffer, 0, packetSize,
                                InetAddress.getByName(RouteConfig.LOCAL_ADDRESS), params.getLocalSendPort());
                        remotePort = packet.getPort();
                        if (active) {
                            try {
                                mRemoteReceivedPackets.put(datagramPacket);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                active = false;
                                interrupt();

                                return;
                            }
                        }
//                            Log.d("网络转发测试12", new String(datagramPacket.getData(),0,datagramPacket.getLength()));

//                        mRemoteReceivedPackets.offer(datagramPacket);
                    }
                    String tunnelInfo = sourceAddress.getHostName() + ":" + packet.getPort();
                    if (RouteConfig.isDebug)
                        Log.d("扩展坞连接测试", "UDPTunnel    run");

                    if (!tunnelPool.contains(tunnelInfo) && active) {
                        if (RouteConfig.isDebug)
                            Log.d("扩展坞连接测试", "UDPTunnel    run   not contains:"+tunnelInfo);
                        tunnelPool.add(tunnelInfo);
                        if (mDisConveringListener != null) {
                            AngencyBean angencyBean = new AngencyBean();
                            angencyBean.setRemoteAds(sourceAddress.getHostName());
                            angencyBean.setRemotePort(packet.getPort());
                            angencyBean.setConnectType(AngencyBean.UDP);
                            angencyBean.setSourcePort(params.getLocalRecievePort());
                            if (RouteConfig.isDebug)
                                Log.d("扩展坞连接测试", "UDPTunnel    run   回调:" + angencyBean.toString());
                            mDisConveringListener.onDiscovering(angencyBean);
                        }
                    }

                }
                if (!subHandlerRun) {
                    subHandlerRun = true;
                    subHandler.post(sendLocalRunnable);
                }
                if (!subHandler1Run) {
                    subHandler1Run = true;
                    subHandler1.post(sendRemoteRunnable);
                }
            } catch (Throwable e) {
                if (RouteConfig.isDebug)
                    Log.d("网络转发测试", "发送失败 ：" + e.getMessage());
                e.printStackTrace();
//                connectionBroken();
            }
        }


    }

    private final LinkedBlockingQueue<DatagramPacket> mLocalReceivedPackets = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<DatagramPacket> mRemoteReceivedPackets = new LinkedBlockingQueue<>();
    private final Handler subHandler;
    private final Handler subHandler1;
    private boolean subHandlerRun = false;
    private boolean subHandler1Run = false;
    private final Runnable sendLocalRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                while (active) {
                    synchronized (sendLocalRunnable) {

                        if (RouteConfig.isDebug)
                            Log.d("网络转发测试", "阻塞问题   获取mLocalReceivedPackets  before");
                        if (!active) {
                            return;
                        }
                        DatagramPacket localPacket = mLocalReceivedPackets.take();
                        if (RouteConfig.isDebug)
                            Log.d("网络转发测试", "阻塞问题   获取mLocalReceivedPackets  after");

                        if (localPacket != null) {
//                            Log.d("网络转发测试12   after", new String(localPacket.getData(),0,localPacket.getLength()));

                            String request = new String(localPacket.getData(), 0, localPacket.getLength());
                            if (RouteConfig.isDebug) {
                                Log.d("网络转发测试", "阻塞问题   远程包 to 本地  " + localPacket.getAddress().getHostName() + ":" + localPacket.getPort());
                                Log.d("网络转发测试", "阻塞问题   本地包 to  本地  request content is ：" + request + clientSocket);
                            }
                            if (clientSocket != null)
                                clientSocket.send(localPacket);
                        }
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
                while (active) {
                    synchronized (sendRemoteRunnable) {

                        if (RouteConfig.isDebug)
                            Log.d("网络转发测试", "阻塞问题   获取mRemoteReceivedPackets  before");
                        if (!active) {
                            return;
                        }
                        DatagramPacket remotePacket = mRemoteReceivedPackets.take();
                        if (RouteConfig.isDebug)
                            Log.d("网络转发测试", "阻塞问题   获取mRemoteReceivedPackets  after");
                        if (remotePacket != null) {
//                            Log.d("网络转发测试1  after", bytesToHexString(remotePacket.getData()));

                            String request = new String(remotePacket.getData(), 0, remotePacket.getLength(), "utf-8");
                            if (RouteConfig.isDebug) {
                                Log.d("网络转发测试", "阻塞问题   远程包 to 本地  1" + remotePacket.getAddress().getHostName() + ":" + remotePacket.getPort());
                                Log.d("网络转发测试", "阻塞问题   远程包 to 本地  request content is ：" + request);
                            }

                            remotePacket.setAddress(InetAddress.getByName("127.0.0.1"));
                            if (clientSocket != null)
                                clientSocket.send(remotePacket);
                        }
                    }

                }
            } catch (InterruptedException e) {
                Log.e("exception", "Dispatching received data thread was interrupted.");
            } catch (IOException e) {
                Log.e("exception", "udpTunnel   send error");
            }
        }
    };



    public void close() {
        if (RouteConfig.isDebug)
            Log.d("网络转发测试", "UDPTunnel    close");
        connectionBroken();
    }

    public void connectionBroken() {
        if (RouteConfig.isDebug)
            Log.d("网络转发测试", "UDPTunnel    connectionBroken");
        if (active) {
            String dateStr = sdf.format(new Date());
            if (params.isPrint())
                Log.d("网络转发测试", dateStr + ": UDP Forwarding clientsocket  " + " <--> " + " stopped.");
            active = false;
        }
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
//                clientSocket.disconnect();
                clientSocket.close();
                clientSocket = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d("网络转发测试", "UDPTunnel    connectionBroken11");

    }
}
