package com.madao.simplebeat;

import androidx.annotation.NonNull;

public class AudioData {
    private byte[] upbeat;
    private byte[] downbeat;

    private final String upbeatPath;
    private final String downbeatPath;
    private final String name;

    private boolean isLoaded = false;
    private boolean isSingle = false;

    public AudioData(String name, String upbeatPath, String downbeatPath) {
        this.name = name;
        this.upbeatPath = upbeatPath;
        this.downbeatPath = downbeatPath;
        isSingle = false;
    }

    public AudioData(String name, String path) {
        this.name = name;
        this.upbeatPath = path;
        this.downbeatPath = path;
        isSingle = true;
    }

    public boolean isSingle() {
        return isSingle;
    }

    public byte[] getUpbeat() {
        return upbeat;
    }

    public byte[] getDownbeat() {
        return downbeat;
    }

    public void setBeat(byte[] upbeat, byte[] downbeat) {
        isLoaded = true;
        this.upbeat = upbeat;
        this.downbeat = downbeat;
    }

    public void setBeat(byte[] beat) {
        isLoaded = true;
        this.upbeat = beat;
        this.downbeat = beat;
    }

    public String getUpbeatPath() {
        return upbeatPath;
    }

    public String getDownbeatPath() {
        return downbeatPath;
    }

    public String getName() {
        return name;
    }

    public boolean isLoaded() {
        return isLoaded;
    }
}
