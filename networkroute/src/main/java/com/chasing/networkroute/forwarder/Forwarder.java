package com.chasing.networkroute.forwarder;

import android.util.Log;

import com.chasing.networkroute.Params;
import com.chasing.networkroute.RouteConfig;
import com.chasing.networkroute.observers.TCPObserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Forwards TCP traffic between two sockets.
 * Sends everything from socket 1 input stream to socket 2 outputstream.
 * In case of any error on either socket, the parent tunnel is closed.
 */
public class Forwarder extends Thread {
    /**
     * Socket 1 inputstream.
     */
    private InputStream is;
    /**
     * Socket 2 outputstream.
     */
    private OutputStream os;
    /**
     * The TCP tunnel this forwarder is part of.
     */
    private TCPTunnel parent;
    /**
     * Configuration parameters.
     */
    private final Params params;
    /**
     * The observers to pass all data through. Logging the data etc.
     */
    private final List<TCPObserver> observers;

    /**
     * @param parent     The TCP tunnel containing this forwarder.
     * @param is         Inputstream for socket 1, to connect to the outputstream.
     * @param os         Outputstream for socket 2, to connect to the inputstream.
     * @param params     Configuration parameters.
     * @param up         If true, we pass all data going through to upstream observers.
     * @param sourceAddr Source address of the stream (up- or down-stream). For logging purposes...
     */
    public Forwarder(TCPTunnel parent, InputStream is, OutputStream os, Params params, boolean up, String sourceAddr) {
        this.parent = parent;
        this.is = is;
        this.os = os;
        this.params = params;
        if (up) this.observers = params.createUpObservers(sourceAddr);
        else this.observers = params.createDownObservers(sourceAddr);
    }

    /**
     * Continously reads the input stream and writes the data to the output stream.
     * In between passes all the data to any registered observers.
     * In case of error on either socket, notifies the parent TCP tunnel to close.
     */
    public void run() {
        byte[] buffer = new byte[params.getBufferSize()];
        try {
            while (true) {
                int bytesRead = is.read(buffer);

                if (bytesRead == -1) break; // End of stream is reached --> exit
                for (TCPObserver observer : observers) {
                    observer.observe(buffer, 0, bytesRead);
                }

                if (RouteConfig.isDebug)
                    Log.d("类型赋值测试", "Forwarder   " + new String(buffer, 0, bytesRead));

                os.write(buffer, 0, bytesRead);
                os.flush();
            }
        } catch (IOException e) {
            // Read/write failed --> connection is broken
        }
//    parent.connectionBroken();
    }
}