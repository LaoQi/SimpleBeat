package com.madao.simplebeat;

import android.content.Context;
import android.content.SharedPreferences;

public class Profile {

    private final static String name = "profile";

    private final static String KeyBPM = "BPM";
    private final static String KeyAudio = "Audio";

    private final SharedPreferences preferences;

    public Profile(Context context) {
        preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    public int getBPM() {
        return preferences.getInt(KeyBPM, 120);
    }

    public void setBPM(int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KeyBPM, value);
        editor.apply();
    }

    public String getAudioKey() {
        return preferences.getString(KeyAudio, Constant.AudioDefault);
    }

    public void setAudioKey(String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KeyAudio, value);
        editor.apply();
    }
}
