package com.madao.simplebeat;

import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class Animator {

    private final static String Tag = "Animator";

    private final Activity mActivity;
    private TextView timerBar;
    private ImageButton startButton;
    private TextView bpmButton;
    private TextView audioSelectorButton;
    private View timeAndPlay;
    private float unit;
    private View bpmPicker;
    private View audioSelector;
    private View bpmAndAudioButtons;

    public void setBpmAndAudioButtons(View bpmAndAudioButtons) {
        this.bpmAndAudioButtons = bpmAndAudioButtons;
    }

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

    public Animator setBpmButton(TextView bpmButton) {
        this.bpmButton = bpmButton;
        return this;
    }

    public Animator setAudioSelectorButton(TextView audioSelectorButton) {
        this.audioSelectorButton = audioSelectorButton;
        return this;
    }

    public Animator setTimeAndPlay(View timeAndPlay) {
        this.timeAndPlay = timeAndPlay;
        return this;
    }

    public Animator setBpmPicker(View bpmPicker) {
        this.bpmPicker = bpmPicker;
        return this;
    }

    public Animator setAudioSelector(View audioSelector) {
        this.audioSelector = audioSelector;
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

    public void TimeAndPlayUP() {
        ObjectAnimator.ofFloat(timeAndPlay, "translationY", 0, 0 - unit * 2)
                .setDuration(500).start();
    }

    public void TimeAndPlayDOWN() {
        ObjectAnimator.ofFloat(timeAndPlay, "translationY", 0 - unit * 2, 0)
                .setDuration(500).start();
    }

    public void ShowBpmPicker() {
        bpmPicker.setVisibility(View.VISIBLE);
        TimeAndPlayUP();
        ObjectAnimator.ofFloat(bpmPicker, "alpha", 0f, 1f)
                .setDuration(500).start();
        ObjectAnimator.ofFloat(bpmAndAudioButtons, "alpha", 1f, 0f)
                .setDuration(300).start();
        ObjectAnimator animator = ObjectAnimator.ofFloat(bpmPicker, "translationY", 0, 0 - unit)
                .setDuration(500);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                super.onAnimationEnd(animation);
                bpmAndAudioButtons.setVisibility(View.GONE);
            }
        });
        animator.start();
    }

    public void HideBpmPicker() {
        TimeAndPlayDOWN();
        bpmAndAudioButtons.setVisibility(View.VISIBLE);
        ObjectAnimator.ofFloat(bpmPicker, "alpha", 1f, 0f)
                .setDuration(500).start();
        ObjectAnimator.ofFloat(bpmAndAudioButtons, "alpha", 0f, 1f)
                .setDuration(300).start();
        ObjectAnimator animator = ObjectAnimator.ofFloat(bpmPicker, "translationY", 0 - unit, 0)
                .setDuration(500);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                super.onAnimationEnd(animation);
                bpmPicker.setVisibility(View.GONE);
            }
        });
        animator.start();
    }

    public void ShowAudioSelector() {
        audioSelector.setVisibility(View.VISIBLE);
        TimeAndPlayUP();
        ObjectAnimator.ofFloat(audioSelector, "alpha", 0f, 1f)
                .setDuration(500).start();
        ObjectAnimator.ofFloat(bpmAndAudioButtons, "alpha", 1f, 0f)
                .setDuration(300).start();
        ObjectAnimator animator = ObjectAnimator.ofFloat(audioSelector, "translationY", 0, 0 - unit)
                .setDuration(500);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                super.onAnimationEnd(animation);
                bpmAndAudioButtons.setVisibility(View.GONE);
            }
        });
        animator.start();
    }

    public void HideAudioSelector() {
        TimeAndPlayDOWN();
        bpmAndAudioButtons.setVisibility(View.VISIBLE);
        ObjectAnimator.ofFloat(audioSelector, "alpha", 1f, 0f)
                .setDuration(500).start();
        ObjectAnimator.ofFloat(bpmAndAudioButtons, "alpha", 0f, 1f)
                .setDuration(300).start();
        ObjectAnimator animator = ObjectAnimator.ofFloat(audioSelector, "translationY", 0 - unit, 0)
                .setDuration(500);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                super.onAnimationEnd(animation);
                audioSelector.setVisibility(View.GONE);
            }
        });
        animator.start();
    }
}
