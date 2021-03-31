package com.madao.simplebeat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.NumberPicker;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    private Metronome metronome;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        metronome = new Metronome(new Handler(Looper.getMainLooper(), msg -> {
            Log.d(getClass().getName(), msg.toString());
            return false;
        }));

        metronome.start();

        NumberPicker bpmPicker = (NumberPicker) findViewById(R.id.bpmPicker);
        // 设置NumberPicker属性
        bpmPicker.setMinValue(60);
        bpmPicker.setMaxValue(240);
        bpmPicker.setValue(120);

        // 监听数值改变事件
        bpmPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            metronome.setBpm(newVal);
        });
    }

    public void onStartStopClick(View view) {
        if (isPlaying) {
            metronome.pause();
            isPlaying = false;
        } else {
            metronome.play();
            isPlaying = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        metronome.close();
    }
}