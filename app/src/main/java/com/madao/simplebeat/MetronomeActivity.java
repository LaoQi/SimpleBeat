package com.madao.simplebeat;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.IOException;

public class MetronomeActivity extends AppCompatActivity {

    private static final String Tag = "MetronomeActivity";

    private Metronome metronome;
    private AudioManager audioManager;
    private Profile profile;
    private boolean isPlaying = false;
    private boolean soundBooster = false;
    private boolean isKeepScreen;
    private int audioInitPosition;
    private TextView timerBar;
    private ImageButton startButton;
    private ConstraintLayout timerAndPlay;
    private long startTime;

    private long timeCounter = 0;
    private Handler mHandler;

    private GestureDetector mGestureDetector;
    private Animator mAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metronome);
        setTitle("");

        GestureListener mGestureListener = new GestureListener();
        mGestureDetector = new GestureDetector(this, mGestureListener);

        profile = new Profile(this);
        isKeepScreen = profile.getKeepScreen();
        soundBooster = profile.getSoundBooster();
        audioManager = new AudioManager(this);

        mHandler = new Handler(Looper.getMainLooper(), msg -> {
            switch (msg.what) {
                case Messages.MsgTickTime:
                    break;
                case Messages.MsgUpdateTimer:
                    updateTimerBar();
                    break;
            }
            return false;
        });

        audioInitPosition = audioManager.getPosition(profile.getAudioKey());

        initBpmPicker();
        initAudioSelector();
        initTimerBar();

        mAnimator = new Animator(this);
        startButton = findViewById(R.id.startButton);
        timerAndPlay = findViewById(R.id.TimerAndPlay);

        mAnimator.setTimerBar(timerBar)
                .setStartButton(startButton);
    }

    private void resizeWindow() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        Log.d(Tag, "Resize window " + width + " " + height);

        float unit;
        boolean isCompact = false;
        if (width < 600) {
           unit = width / 8f;
           isCompact = true;
        } else {
            unit = width / 2f / 8f;
        }
        unit = unit / 3f * 4f;
        Log.d(Tag, "text size " + unit);
        mAnimator.setUnit(unit);

        timerAndPlay.setMinHeight((int) (unit * 3.5));

        timerBar.setTextSize(TypedValue.COMPLEX_UNIT_PX, unit);
        startButton.setAdjustViewBounds(true);
        startButton.setMinimumWidth((int) unit * 2);
        startButton.setMaxWidth((int) unit * 2);


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    private static class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(Tag, "GestureListener onFling " + velocityX + " " + velocityY);
            return false;
        }
    }

    private void resetMetronome() {

        metronome = new Metronome(mHandler);
        metronome.setBpm(profile.getBPM());
        metronome.setBooster(soundBooster);
        metronome.start();
        updateAudio(profile.getAudioKey());
        stop();
    }

    @SuppressLint("SetTextI18n")
    private void initTimerBar() {
        timerBar = findViewById(R.id.timerBar);
        AssetManager mgr = getAssets();
        Typeface tf = Typeface.createFromAsset(mgr, Constant.TimeBarFont);
        timerBar.setTypeface(tf);
        timerBar.setText("00:00:00");
        new Thread(() -> {
            while (true) {
                if (isPlaying) {
                    mHandler.sendMessage(Messages.UpdateTimer());
                }
                try {
                    //noinspection BusyWait
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Log.w(Tag, e.toString(), e);
                }
            }
        }).start();
    }

    private void initBpmPicker() {
        BpmPicker bpmPicker = findViewById(R.id.BpmPicker);
        bpmPicker.setValue(profile.getBPM());

        bpmPicker.setOnValueChangedListener((oldVal, newVal) -> {
            metronome.setBpm(newVal);
            profile.setBpm(newVal);
        });
    }

    private void initAudioSelector() {
        AudioSelector audioSelector = findViewById(R.id.AudioSelector);
        audioSelector.bindData(audioInitPosition, audioManager.getAudioList(), (oldVal, newVal) -> updateAudio(audioManager.getAudioList().get(newVal)));
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    public void updateTimerBar() {
        long ts = System.currentTimeMillis() - startTime;
        long minutes = ts / 60000;
        long seconds = (ts % 60000) / 1000;
        long milliseconds = ts % 100;

        if (timeCounter > 0) {
            long lastTime = timeCounter;
            if (isPlaying) {
                long pass = System.currentTimeMillis() - startTime;
                lastTime -= pass;

                if (lastTime <= 0) {
                    lastTime = 0;
                    stop();
                }
            }

            minutes = lastTime / 60000;
            seconds = (lastTime % 60000) / 1000;
            milliseconds = lastTime % 100;
        }
        timerBar.setText(String.format("%02d:%02d:%02d", minutes, seconds, milliseconds));
    }

    public void updateAudio(String selected) {
        try {
            AudioData audioData = audioManager.getAudio(selected);
            metronome.setUpbeat(audioData.getUpbeat());
            metronome.setDownbeat(audioData.getDownbeat());
            profile.setAudioKey(selected);
        } catch (AudioManager.AudioDataNotFound | IOException exception) {
            Log.w(Tag, exception.toString(), exception);
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void toggleKeepScreen() {
        isKeepScreen = !isKeepScreen;
        if (isKeepScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public void toggleSoundBooster() {
        soundBooster = !soundBooster;
        metronome.setBooster(soundBooster);
        profile.setSoundBooster(soundBooster);
    }

    private void stop() {
        mAnimator.Stop();
        metronome.pause();
        isPlaying = false;
        timeCounter = 0;
    }

    private void play() {
        metronome.play();
        isPlaying = true;

        mAnimator.Play();
        startTime = System.currentTimeMillis();
        updateTimerBar();
    }

    private void setTimerOn(int minutes, int seconds) {
        ImageButton view = findViewById(R.id.startButton);
        float right = Math.min(timerBar.getRight() + 20, getWindow().getDecorView().getWidth()/2 - (view.getWidth()/2));
//        updateStatusBar(0, 0);
        float bottom = (float) timerBar.getHeight();
//        ObjectAnimator.ofFloat(view, "translationX", 0, right)
        ObjectAnimator.ofFloat(view, "translationY", 0, bottom)
                .setDuration(500).start();

        ObjectAnimator.ofFloat(timerBar, "alpha", 0f, 1f)
                .setDuration(400).start();
        timeCounter = (minutes * 60L + seconds) * 1000;

        updateTimerBar();
    }


    public void onStartStopClick(View view) {
        if (isPlaying) {
            stop();
        } else {
            play();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        resizeWindow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resizeWindow();
        if (isKeepScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        resetMetronome();
    }

    @Override
    protected void onPause() {
        super.onPause();
        profile.setKeepScreen(isKeepScreen);
        if (isKeepScreen) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        metronome.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        profile.setKeepScreen(isKeepScreen);
        metronome.close();
    }
}
