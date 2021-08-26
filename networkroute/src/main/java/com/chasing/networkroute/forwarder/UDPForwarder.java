package com.chasing.networkroute.forwarder;

import android.net.Network;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.chasing.networkroute.Params;
import com.chasing.networkroute.RouteConfig;
import com.chasing.networkroute.observers.TCPObserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by AlexZhuo on 2017/11/12.
 */
public class UDPForwarder extends Thread {
    private InetAddress clientAddr;
    private int clientPort;
    private DatagramSocket serverSocket;
    private DatagramSocket clientSocket;
    private DatagramPacket sendData;
    Params params;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MMM.dd HH:mm:ss");
    private List<TCPObserver> downObservers;
    UDPTunnel parent;
    private Network mNetwork;

    /** The observers to pass all data through. Logging the data etc. */


    /**
     * Construct for Symmetric UDP forwarder
     *
     * @param clientSocket
     * @param clientAddr
     * @param clientPort
     * @param params
     */
    public UDPForwarder(UDPTunnel parent, DatagramSocket clientSocket, InetAddress clientAddr, int clientPort, Params params, Network network) {
        try {
            this.clientAddr = InetAddress.getByAddress(clientAddr.getAddress());
            this.clientPort = clientPort;
            this.clientSocket = clientSocket;
            this.params = params;
            this.downObservers = params.createDownObservers(params.getRemoteHost());
            this.mNetwork = network;
            serverSocket = new DatagramSocket(params.getLocalSendPort());
            serverSocket.setSoTimeout(30000);//30s UDP tunnel TimeOut
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                if (RouteConfig.isDebug)
                    Log.d("网络转发测试", "UDPForwarder   bindSocket");
                mNetwork.bindSocket(serverSocket);
            }
            this.parent = parent;
            HandlerThread thread = new HandlerThread("udpforwarder-send");
            thread.start();
            subHandler = new Handler(thread.getLooper());

        } catch (Exception e) {
            e.printStackTrace();
            if (RouteConfig.isDebug)
                Log.e("网络转发测试", "UDPForwarder  构造  error:" + e.getMessage());
        }
    }

    public DatagramSocket getServerSocket() {
        return this.serverSocket;
    }

    public InetAddress getClientAddr() {
        return clientAddr;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    @Override
    public void run() {
        //receiving the data from remote server
        DatagramPacket response = new DatagramPacket(new byte[12800], 12800);
        try {
            while (true) {
                if (RouteConfig.isDebug)
                    Log.d("网络转发测试", "UDPForwarder   run：  while:");
                if (serverSocket == null) {
                    if (RouteConfig.isDebug)
                        Log.d("网络转发测试", "UDPForwarder   serverSocket  is null:");
                    return;
                }
                if (serverSocket.isClosed()) {
                    if (RouteConfig.isDebug)
                        Log.d("网络转发测试", "UDPForwarder    socket    is   closed:");
                    return;
                }
                if (RouteConfig.isDebug)
                    Log.d("网络转发测试", "UDPForwarder    socket    not  null:");

                serverSocket.receive(response);
                if (RouteConfig.isDebug)
                    Log.d("网络转发测试", "UDPForwarder    receive    ok:");

//                params.setRemotePort(response.getPort());
                if (params.isPrint()) {
                    String dateStr = sdf.format(new Date());
                    if (RouteConfig.isDebug) {
                        Log.d("网络转发测试", "\n" + dateStr + ": UDPForwarder  "
                                + response.getAddress().getHostAddress() + ":" + response.getPort() + " <--> "
                                + clientAddr + ":" + clientPort);
                        String result = new String(response.getData(), 0, response.getLength());
                        Log.d("网络转发测试", "UDPForwarder    remote server response length=" + response.getLength()
                                + "\n" + result);
                    }
                }

                //send the response from remote server to client
                response.setAddress(clientAddr);
                response.setPort(clientPort);
                if (RouteConfig.isDebug)
                    Log.d("网络转发测试", "rov  数据发送  UDPForwarder  发送包==" + clientAddr + "===" + clientPort);
                subHandler.post(mDispatchReceivedData);
                for (TCPObserver observer : downObservers) {
                    observer.observe(response.getData(), 0, response.getLength());
                }
            }
        } catch (SocketTimeoutException e) {
            if (RouteConfig.isDebug)
                Log.d("网络转发测试", "UDPForwarder    server  发送   SocketTimeoutException   " + e.getMessage());
            if (RouteConfig.isDebug)
                Log.d("网络转发测试", "UDPForwarder    this thread is dead" + clientAddr + ":" + clientPort);
            close();
        } catch (IOException e) {
            if (RouteConfig.isDebug)
                Log.d("网络转发测试", "UDPForwarder    server  发送   IOException   " + e.getMessage());
            e.printStackTrace();
        }
    }


    private DatagramPacket mSendPacket;
    private final LinkedBlockingQueue<ByteBuffer> mReceivedPackets = new LinkedBlockingQueue<>();
    private Handler subHandler;

    private final Runnable mDispatchReceivedData = new Runnable() {
        @Override
        public void run() {
            try {
                while (clientSocket != null) {
                    if (RouteConfig.isDebug)
                        Log.d("网络转发测试", "UDPForwarder    mReceivedPackets.length=" + mReceivedPackets.size() + "   before");
                    final ByteBuffer data = mReceivedPackets.take();
                    if (RouteConfig.isDebug)
                        Log.d("网络转发测试", "UDPForwarder    mReceivedPackets.length=" + mReceivedPackets.size() + "   after");

                    if (mSendPacket == null) {
                        mSendPacket = new DatagramPacket(data.array(), data.array().length, clientAddr, clientPort);
                    } else {
                        mSendPacket.setData(data.array(), 0, data.array().length);
                        mSendPacket.setAddress(clientAddr);
                        mSendPacket.setPort(clientPort);
                    }
                    if (clientSocket != null)
                        clientSocket.send(mSendPacket);
                }
            } catch (InterruptedException e) {
                Log.e("exception", "Dispatching received data thread was interrupted.");
            } catch (BufferOverflowException e) {//mMAVLinkParser.mavlink_parse_char()出现异常概率很小
                Log.e("exception", "Dispatching received data - BufferOverflowException");
            } catch (IOException e) {
                Log.e("exception", "udpTunnel   send error");
            } finally {
                Log.e("exception", "Exiting received data dispatcher thread.");
            }
        }
    };


    public void close() {
        if (!serverSocket.isClosed()) {
            if (RouteConfig.isDebug)
                Log.d("网络转发测试", "UDPForwarder    close   ");
            serverSocket.close();
        }
    }
}
