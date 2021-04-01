package com.madao.simplebeat;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AudioManager {

    private final Context mContext;
    private final Map<String, AudioData> audios = new HashMap<>();
    private final List<String> audioList = new ArrayList<>();

    public static class AudioDataNotFound extends Exception {
        public AudioDataNotFound(String s) {
            super(s);
        }
    }

    private void putAudio(String name, String upbeatPath, String downbeatPath) {
        audios.put(name, new AudioData(name, upbeatPath, downbeatPath));
        audioList.add(name);
    }

    public AudioManager(Context context) {
        mContext = context;
        putAudio(Constant.AudioDefault, "audios/upbeat.wav", "audios/downbeat.wav");
        putAudio(Constant.AudioClap, "audios/downbeat.wav", "audios/upbeat.wav");
        putAudio(Constant.AudioClaves, "audios/downbeat.wav", "audios/upbeat.wav");
        putAudio(Constant.AudioRimshot, "audios/downbeat.wav", "audios/upbeat.wav");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private byte[] LoadData(String path) throws IOException {
        InputStream in = mContext.getAssets().open(path);
//        byte[] header = new byte[Constant.WavHeaderSize];
//        in.read(header);
        in.skip(Constant.WavHeaderSize);

        byte[] buffer = new byte[1024];
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        while(in.read(buffer) != -1) {
            bo.write(buffer);
        }
        return bo.toByteArray();
    }

    public AudioData getAudio(String key) throws AudioDataNotFound, IOException {
        AudioData audioData = audios.get(key);
        if (audioData == null) {
            throw new AudioDataNotFound(String.format("AudioData %s Not Found", key));
        }
        if (!audioData.isLoaded()) {
            byte[] upbeat = LoadData(audioData.getUpbeatPath());
            if (audioData.isSingle()) {
                audioData.setBeat(upbeat);
            } else {
                byte[] downbeat = LoadData(audioData.getDownbeatPath());
                audioData.setBeat(upbeat, downbeat);
            }
        }
        return audioData;
    }

    public List<String> getAudioList() {
        return audioList;
    }

    public int getPosition(String key) {
        return audioList.indexOf(key);
    }
}
