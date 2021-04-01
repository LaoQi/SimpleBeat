package com.madao.simplebeat;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AudioManager {

    private final Context mContext;
    private final Map<String, AudioData>dict = new HashMap<>();

    public static class AudioDataNotFound extends Exception {
        public AudioDataNotFound(String s) {
            super(s);
        }
    }

    public AudioManager(Context context) {
        mContext = context;
        dict.put("classic", new AudioData("classic", "audios/upbeat.wav", "audios/downbeat.wav"));
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
        AudioData audioData = dict.get(key);
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
}
