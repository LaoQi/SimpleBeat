package com.madao.simplebeat;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

public class Animator {

    private final static String Tag = "Animator";

    private final Activity mActivity;
    private TextView timerBar;
    private ImageButton startButton;
    private float unit;

    public Animator(Activity activity) {
        mActivity = activity;
    }

    public Animator setTimerBar(TextView timerBar) {
        this.timerBar = timerBar;
        return this;
    }

    public Animator setStartButton(ImageButton startButton) {
        this.startButton = startButton;
        return this;
    }

    public void setUnit(float unit) {
        this.unit = unit;
    }

    public void Play() {
        AnimatedVectorDrawable drawable = (AnimatedVectorDrawable) (startButton).getDrawable();
        drawable.start();
        float bottom = unit * 1.1f;
        ObjectAnimator.ofFloat(startButton, "translationY", 0, bottom)
                .setDuration(500).start();
        ObjectAnimator.ofFloat(timerBar, "alpha", 0f, 1f)
                .setDuration(400).start();
        Log.d(Tag, "timer bar " + timerBar.getTextSize());
    }

    public void Stop() {
        AnimatedVectorDrawable drawable = (AnimatedVectorDrawable) (startButton).getDrawable();
        drawable.reset();
        float bottom = unit * 1.1f;
        ObjectAnimator.ofFloat(startButton, "translationY", bottom, 0)
                .setDuration(500).start();
        ObjectAnimator.ofFloat(timerBar, "alpha", 1f, 0f)
                .setDuration(400).start();
    }
}
