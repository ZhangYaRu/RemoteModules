package com.chasing.ifupgrade.bean;

import com.google.gson.annotations.SerializedName;

/**
 * @author olivia
 *
 * @data 2019/7/1 17:52
 *
 */
public class CameraStatusBean {

    /**
     * sys : 2
     * controlrole : true
     * is_recording : false
     * recordingtime : 0
     * media : {"all":7928520704,"avail":7910367232}
     */

    @SerializedName("sys")
    private int sys;
    @SerializedName("controlrole")
    private boolean controlrole;
    @SerializedName("is_recording")
    private boolean isRecording;
    @SerializedName("recordingtime")
    private int recordingtime;
    @SerializedName("media")
    private MediaBean media;

    public int getSys() {
        return sys;
    }

    public void setSys(int sys) {
        this.sys = sys;
    }

    public boolean isControlrole() {
        return controlrole;
    }

    public void setControlrole(boolean controlrole) {
        this.controlrole = controlrole;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void setIsRecording(boolean isRecording) {
        this.isRecording = isRecording;
    }

    public int getRecordingtime() {
        return recordingtime;
    }

    public void setRecordingtime(int recordingtime) {
        this.recordingtime = recordingtime;
    }

    public MediaBean getMedia() {
        return media;
    }

    public void setMedia(MediaBean media) {
        this.media = media;
    }

    public static class MediaBean {
        /**
         * all : 7928520704
         * avail : 7910367232
         */

        @SerializedName("all")
        private long all;
        @SerializedName("avail")
        private long avail;

        public long getAll() {
            return all;
        }

        public void setAll(long all) {
            this.all = all;
        }

        public long getAvail() {
            return avail;
        }

        public void setAvail(long avail) {
            this.avail = avail;
        }

        @Override
        public String toString() {
            return "MediaBean{" +
                    "all=" + all +
                    ", avail=" + avail +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "CameraStatusBean{" +
                "sys=" + sys +
                ", controlrole=" + controlrole +
                ", isRecording=" + isRecording +
                ", recordingtime=" + recordingtime +
                ", media=" + media +
                '}';
    }
}
