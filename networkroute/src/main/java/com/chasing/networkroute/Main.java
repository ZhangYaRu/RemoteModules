package com.chasing.networkroute;

import android.content.Context;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.chasing.networkroute.forwarder.DNSTunnel;
import com.chasing.networkroute.forwarder.TCPTunnel;
import com.chasing.networkroute.forwarder.UDPTunnel;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Main starting point for the tunnel application. Either directly from command line or programmatically.
 * When running programmatically, create the configuration with the {@link Params} object, create an instance of this class and invoke the start() method.
 */
public class Main implements Runnable {
    /**
     * The configuration for the tunnel.
     */
    private Params params = null;
    /**
     * As long as this is true, we wait for new connections for the tunnel.
     */
    private boolean shouldRun = true;
    /**
     * The main thread running this tunnel.
     */
//    private Thread thread = null;
    /**
     * List of active tunnels.
     */
    private final List<TCPTunnel> tunnels = new ArrayList<>();
    private ServerSocket serverSocket;
    private DatagramSocket udpServerSocket;

    private Network mNetwork;
    private Context mContext;
    private UDPTunnel mUdpTunnel;

    public Main(Context context, Network network) {
        this.mContext = context;
        this.mNetwork = network;
    }

    public boolean checkParams(String[] args) {
        if (RouteConfig.isDebug)
            Log.d("网络转发测试", "checkParams");
        Params params = ArgumentParser.parseArgs(args);
        if (params.getErrors().length() > 0) {
            if (RouteConfig.isDebug)
                Log.d("网络转发测试", "checkParams   error" + params.getErrors());
        }
        //if the parametesr do not parse correctly or something required is missing, exit
        if (params.shouldRun()) {
            if (RouteConfig.isDebug)
                Log.d("网络转发测试", "checkParams   params.shouldRun()");
            this.params = params;
            return true;
        }
        return false;
    }

    /**
     * Use this to start the actual tunneling.
     */
//    public void start() {
//        thread = new Thread(this);
//        thread.start();
//    }


    private WifiManager.MulticastLock multicastLock_2;

    /**
     * 改配置用于接收UDP广播, 不调用可能导致接收不到
     */
    private void allowMulticast() {
        if (mContext == null) {
            return;
        }

        if (multicastLock_2 != null && multicastLock_2.isHeld()) {
            if (RouteConfig.isDebug)
                Log.d("App", ">>> multicastLock_2.isHeld() = " + multicastLock_2.isHeld());
            multicastLock_2.release();
        }

        WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        multicastLock_2 = wifiManager.createMulticastLock("multicast.ifdive.2");
        multicastLock_2.acquire();
    }

    @Override
    public void run() {
        if (RouteConfig.isDebug)
            Log.d("重连测试", "Main   run===" + params.getLocalRecievePort());

        if (params.isDns()) {
            try {
                if (RouteConfig.isDebug)
                    Log.d("网络转发测试", "Main   run    params.isDns()");
                udpServerSocket = new DatagramSocket(params.getLocalRecievePort());
                DNSTunnel tunnel = new DNSTunnel(params, udpServerSocket);
                tunnel.start();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        } else if (params.isUdptun()) {
            if (RouteConfig.isDebug)
                Log.d("重连测试", "Main   run    params.isUdptun()  ==" + params.getLocalRecievePort());

            try {
                if (mNetwork == null) {
                    if (RouteConfig.isDebug)
                        Log.d("重连测试", "Main   run   mNetwork==null ===" + params.getLocalRecievePort());
                    return;
                }
                // 广播锁, 决定是否可以接收广播, 切不可取消
                allowMulticast();
                if (udpServerSocket == null) {
                    udpServerSocket = new DatagramSocket(params.getLocalRecievePort());
//                    udpServerSocket.setSoTimeout(3000);
                    if (RouteConfig.isDebug)
                        Log.d("重连测试", "Main   run   ===" + params.getLocalRecievePort());
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    if (RouteConfig.isDebug)
                        Log.d("重连测试", "Main.run()   bindSocket ===" + params.getLocalRecievePort());
                    mNetwork.bindSocket(udpServerSocket);
                }

                mUdpTunnel = new UDPTunnel(mContext, params, udpServerSocket, mNetwork, mDiscoveringListener);
                mUdpTunnel.start();
            } catch (IOException e) {
                e.printStackTrace();

            }

        } else {
            if (RouteConfig.isDebug)
                Log.d("类型赋值测试", "Main   run   else ==" + params.getLocalRecievePort());
            try {
                if (serverSocket == null)
                    serverSocket = new ServerSocket(params.getLocalRecievePort());
                while (shouldRun) {
                    if (RouteConfig.isDebug)
                        Log.d("类型赋值测试", "Main   run   else   serverSocket.accept  before");
                    if (!serverSocket.isClosed()){
                        Socket clientSocket = serverSocket.accept();
                        if (RouteConfig.isDebug)
                            Log.d("类型赋值测试", "Main   run   else   serverSocket.accept   after");

                        TCPTunnel tunnel = new TCPTunnel(params, clientSocket, this, mNetwork);
                        tunnel.start();
                        tunnels.add(tunnel);
                    }

                }
            } catch (IOException e) {
                if (RouteConfig.isDebug)
                    Log.d("类型赋值测试", "Error while trying to forward TCP with params:" + params, e);
            }
        }
    }

    private DiscoveringListener mDiscoveringListener;

    public interface DiscoveringListener {
        void onDiscovering(AngencyBean angencyBean);
    }

    public void setDiscoveringListener(DiscoveringListener discoveringListener) {
        mDiscoveringListener = discoveringListener;
    }


    /**
     * Called when a tunnel is closed to remove it from active list.
     *
     * @param tunnel The closed tunnel.
     */
    public void closed(TCPTunnel tunnel) {
//        Iterator<TCPTunnel> iterator = tunnels.iterator();
//        while (iterator.hasNext()) {
//            TCPTunnel next = iterator.next();
//            if (next.equals(tunnel))
//                iterator.remove();
//        }
    }

    /**
     * Stops the tunneling application (no more waiting for new connections to open tunnels).
     */
    public void stop() {
        shouldRun = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            if (params.isPrint()) {
                System.err.println("Error closing server socket");
                e.printStackTrace();
            }
        }
        for (TCPTunnel tunnel : tunnels) {
            tunnel.close();
        }
        if (mUdpTunnel != null) {
            mUdpTunnel.interrupt();
            mUdpTunnel.close();
        }

        if (udpServerSocket != null) {
            if (RouteConfig.isDebug)
                Log.d("重连测试", "Main   stop");
            udpServerSocket.close();
        }
    }
}

