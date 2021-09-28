package com.chasing.networkroute;

import android.net.Network;

/**
 * Create By Olive at 2021/9/9
 */
public abstract class BaseAngecy {
    protected int angecyPortFlag = 7000;
    protected AngecyActionListener mAngecyActionListener;

    public interface AngecyActionListener {

        void onAngecyStarted(int angecyPort);

        void onAngecyClosed();
        /**
         * 警告信息  不影响程序运行, 用户端可处理, 可不处理
         *
         * @param msg
         */
        void onWarning(String msg);

        /**
         * 错误信息  影响程序运行, 会导致程序终止
         *
         * @param msg
         */
        void onError(String msg);

    }

    public void setAngecyActionListener(AngecyActionListener angecyActionListener) {
        mAngecyActionListener = angecyActionListener;
    }


    public abstract void start();

    public abstract void stop();
}
