package com.chasing.networkroute.forwarder;

import android.net.Network;
import android.util.Log;

import com.chasing.networkroute.Main;
import com.chasing.networkroute.Params;
import com.chasing.networkroute.RouteConfig;
import com.chasing.networkroute.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Creates a TCP tunnel between two endpoints via two Forwarder instances.
 * Data is forwarded in both directions using separate sockets.
 * Any error on either socket causes the whole tunnel (both sockets) to be closed.
 */
public class TCPTunnel extends Thread {
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MMM.dd HH:mm:ss");
    /**
     * Configuration parameters.
     */
    private final Params params;
    /**
     * Local endpoint for the tunnel.
     */
    private Socket localSocket;
    /**
     * Remote endpoint for the tunnel.
     */
    private Socket serverSocket;
    /**
     * True if this tunnel is actively forwarding. False if stopped or not yet started.
     */
    private boolean active = false;
    /**
     * Parent to notify when connection is broken.
     */
    private final Main parent;
    private Network mNetwork;

    /**
     * @param params      Configuration parameters.
     * @param localSocket Socket for the local port (endpoint 1 for tunnel).
     */
    public TCPTunnel(Params params, Socket localSocket, Main parent, Network network) {
        this.params = params;
        this.localSocket = localSocket;
        this.parent = parent;
        this.mNetwork = network;
    }

    /**
     * Connects to the remote host and starts bidirectional forwarding (the tunnel).
     */
    public void run() {
        String dateStr = sdf.format(new Date());
        try {
            if (RouteConfig.isDebug)
                Log.d("网络转发测试", "TCPTunnel: run");
            if (mNetwork == null) {
                if (RouteConfig.isDebug)
                    Log.d("网络转发测试", "TCPTunnel: run   mnetwork = null");
                return;
            }
            // Connect to the destination server
            serverSocket = mNetwork.getSocketFactory().createSocket(params.getRemoteHost(), params.getRemotePort());
//            serverSocket = new Socket(params.getRemoteHost(), params.getRemotePort());
            if (RouteConfig.isDebug)
                Log.d("类型赋值测试", "TCPTunnel: run   serverSocket=="+
                        params.getRemoteHost()+":"+params.getRemotePort()+
                        ",localport="+localSocket.getLocalPort());
            // Turn on keep-alive for both the sockets
            serverSocket.setKeepAlive(true);
            localSocket.setKeepAlive(true);

            // Obtain client & server input & output streams
            InputStream clientIn = localSocket.getInputStream();
            OutputStream clientOut = localSocket.getOutputStream();
            InputStream serverIn = serverSocket.getInputStream();
            OutputStream serverOut = serverSocket.getOutputStream();

            // Start forwarding data between server and client
            active = true;
            String clientAddr = toStr(localSocket);
            String serverAddr = toStr(serverSocket);
//            String hummanClientAddr = Utils.mapAddrToHumanReadable(clientAddr);
//            String hummanServerAddr = Utils.mapAddrToHumanReadable(serverAddr);
//            clientAddr = clientAddr + " (" + hummanClientAddr + ")";
//            serverAddr = serverAddr + " (" + hummanServerAddr + ")";
            Forwarder clientForward = new Forwarder(this, clientIn, serverOut, params, true, clientAddr);
            clientForward.start();
            Forwarder serverForward = new Forwarder(this, serverIn, clientOut, params, false, serverAddr);
            serverForward.start();

            if (RouteConfig.isDebug) {
                Log.d("类型赋值测试", "TCPTunnel: TCP Forwarding " + clientAddr + " <--> " + serverAddr);
            }

        } catch (IOException ioe) {
            if (RouteConfig.isDebug) {
                String remoteAddr = params.getRemoteHost() + ":" + params.getRemotePort();
//                String humanRemoteAddr = Utils.mapAddrToHumanReadable(remoteAddr);
//                remoteAddr = remoteAddr + " (" + humanRemoteAddr + ")";
                Log.d("类型赋值测试", "TCPTunnel: Failed to connect to remote host (" + remoteAddr + ")");
                Log.d("类型赋值测试", dateStr + ": Failed to connect to remote host (" + remoteAddr + ")");

            }
//            connectionBroken();
        }
    }

    /**
     * @param socket The socket to describe.
     * @return A string representation of a socket (ip+port).
     */
    private String toStr(Socket socket) {
        String host = socket.getInetAddress().getHostAddress();
        int port = socket.getPort();
        return host + ":" + port;
    }

    /**
     * Closes the tunnel (the forwarding sockets..).
     */
    public void close() {
        connectionBroken();
    }

    /**
     * Called when an error is observed on one of the sockets making up the tunnel.
     * Terminates the tunnel by closing both sockets.
     */
    public synchronized void connectionBroken() {
        if (RouteConfig.isDebug)
            Log.d("重连测试", "TCPTunnel      connectionBroken");

        try {
            serverSocket.close();
        } catch (Exception e) {
        }
        try {
            localSocket.close();
        } catch (Exception e) {
        }

        if (active) {
            String dateStr = sdf.format(new Date());
            if (params.isPrint())
//                System.out.println(dateStr + ": TCP Forwarding " + toStr(localSocket) + " <--> " + toStr(serverSocket) + " stopped.");
                active = false;
        }
        parent.closed(this);
    }
}

