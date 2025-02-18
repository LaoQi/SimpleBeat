package com.madao.simplebeat;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
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
    private ImageButton settingsButton;
    private TextView timerBar;
    private ImageButton startButton;
    private ConstraintLayout timerAndPlay;
    private TextView bpmButton;
    private TextView audioSelectorButton;
    private BpmPicker bpmPicker;
    private AudioSelector audioSelector;
    private View bpmAndAudioButtons;

    private long startTime;

    private long timeCounter = 0;
    private Handler mHandler;

    private GestureDetector mGestureDetector;
    private Animator mAnimator;

    private Typeface typeface;

    public enum StateType {
        Normal, ShowBPM, ShowAudio
    }

    private StateType mState = StateType.Normal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metronome);
        setTitle("");
        AssetManager mgr = getAssets();
        typeface = Typeface.createFromAsset(mgr, Constant.CommonFont);

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
        mAnimator = new Animator(this);

        initBpmPicker();
        initAudioSelector();
        initTimerBar();

        settingsButton = findViewById(R.id.settingsButton);
        startButton = findViewById(R.id.startButton);
        timerAndPlay = findViewById(R.id.TimerAndPlay);
        bpmAndAudioButtons = findViewById(R.id.BpmAndAudioButtons);

        mAnimator.setStartButton(startButton)
                .setTimeAndPlay(timerAndPlay)
                .setBpmAndAudioButtons(bpmAndAudioButtons);

        startButton.setOnLongClickListener(v -> {
            Log.d(Tag, "Start at long click");
            showTimerSetting();
            return true;
        });

        //            startActivity(new Intent(this, SettingsActivity.class));
        settingsButton.setOnClickListener(this::showMenu);
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
           unit = width / 9f;
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

        bpmAndAudioButtons.getLayoutParams().height = (int) (unit * 2);

        bpmButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, unit * 0.8f);
        audioSelectorButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, unit * 0.8f);

        if (height / (width * 1f) > 1.8f) {
            Log.d(Tag, "Long screen resize");
            // long screen
            ConstraintLayout.LayoutParams params1 = (ConstraintLayout.LayoutParams) timerAndPlay.getLayoutParams();
            params1.verticalBias = 0.5f;
            timerAndPlay.setLayoutParams(params1);

            ConstraintLayout.LayoutParams params2 = (ConstraintLayout.LayoutParams) bpmAndAudioButtons.getLayoutParams();
            params2.verticalBias = 0.5f;
            bpmAndAudioButtons.setLayoutParams(params2);
        }

        if (isCompact) {
            Log.d(Tag, "Compact reset buttons");
            ConstraintLayout.LayoutParams params1 = (ConstraintLayout.LayoutParams) bpmButton.getLayoutParams();
            params1.horizontalBias = 0.1f;
            bpmButton.setLayoutParams(params1);

            ConstraintLayout.LayoutParams params2 = (ConstraintLayout.LayoutParams) audioSelectorButton.getLayoutParams();
            params2.horizontalBias = 0.9f;
            audioSelectorButton.setLayoutParams(params2);

            ConstraintLayout.LayoutParams params3 = (ConstraintLayout.LayoutParams) settingsButton.getLayoutParams();
            params3.topMargin = 4;
            params3.rightMargin = 4;
            settingsButton.setLayoutParams(params3);
            settingsButton.setScaleX(0.5f);
            settingsButton.setScaleY(0.5f);
//            settingsButton.setMaxWidth((int) (unit * 0.5));
//            settingsButton.setMaxHeight((int) (unit * 0.5));

        }

        bpmPicker.Resize(unit, isCompact, typeface);
        audioSelector.Resize(unit, isCompact, typeface);
    }

    public void ChangeState(StateType state) {
        if (mState == state) return;
        switch (state) {
            case Normal:
                if (mState == StateType.ShowBPM) {
                    mAnimator.HideBpmPicker();
                } else {
                    mAnimator.HideAudioSelector();
                }
                break;
            case ShowBPM:
                mAnimator.ShowBpmPicker();
                break;
            case ShowAudio:
                mAnimator.ShowAudioSelector();
                break;
        }
        mState = state;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            Log.d(Tag, "GestureListener onFling " + velocityX + " " + velocityY);
            return false;
        }

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            if (mState != StateType.Normal) {
                ChangeState(StateType.Normal);
            }
            return super.onDown(e);
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
        timerBar.setTypeface(typeface);
        timerBar.setText("00:00:00");
        mAnimator.setTimerBar(timerBar);
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

    @SuppressLint("SetTextI18n")
    private void initBpmPicker() {
        bpmPicker = findViewById(R.id.BpmPicker);
        bpmPicker.setValue(profile.getBPM());

        bpmButton = findViewById(R.id.BpmPickerButton);
        bpmButton.setTypeface(typeface);
        bpmButton.setText("" + profile.getBPM());

        bpmPicker.setOnValueChangedListener((oldVal, newVal) -> {
            metronome.setBpm(newVal);
            profile.setBpm(newVal);
            bpmButton.setText("" + newVal);
        });

        bpmButton.setOnClickListener((view) -> ChangeState(StateType.ShowBPM));

        mAnimator.setBpmPicker(bpmPicker)
                .setBpmButton(bpmButton);
    }

    private void initAudioSelector() {
        audioSelector = findViewById(R.id.AudioSelector);
        audioSelector.bindData(audioInitPosition, audioManager.getAudioList(), (oldVal, newVal) -> updateAudio(audioManager.getAudioList().get(newVal)));

        audioSelectorButton = findViewById(R.id.AudioSelectorButton);
        audioSelectorButton.setTypeface(typeface);
        audioSelectorButton.setText(profile.getAudioKey());
        audioSelectorButton.setOnClickListener((view) -> ChangeState(StateType.ShowAudio));

        mAnimator.setAudioSelector(audioSelector)
                .setAudioSelectorButton(audioSelectorButton);
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
            metronome.setAudioData(audioData);
            profile.setAudioKey(selected);
            audioSelectorButton.setText(selected);
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

    private void showTimerSetting() {
        String title = getString(R.string.timer_setting);

        final AlertDialog.Builder timeDialog =
                new AlertDialog.Builder(this);
        timeDialog.setTitle(title);

        View selector = View.inflate(this, R.layout.time_selector, null);
        timeDialog.setView(selector);

        final NumberPicker minutesPicker = selector.findViewById(R.id.timer_picker_minutes);
        final NumberPicker secondsPicker = selector.findViewById(R.id.timer_picker_seconds);

        minutesPicker.setMaxValue(120);
        minutesPicker.setMinValue(0);

        secondsPicker.setMaxValue(59);
        secondsPicker.setMinValue(0);

        timeDialog.setPositiveButton(R.string.ok,
                (dialog, which) -> {
                    int minutes = minutesPicker.getValue();
                    int seconds = secondsPicker.getValue();
                    if (minutes > 0 || seconds > 0) {
                        setTimerOn(minutes, seconds);
                    }
                });
        timeDialog.setNegativeButton(R.string.cancel, (dialog, which) -> { });

        timeDialog.show();
    }


    public void onStartStopClick(View view) {
        if (isPlaying) {
            stop();
        } else {
            play();
        }
    }

    private void showAbout() {
        String title = getString(R.string.app_name);
        PackageManager pm = getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), 0);
            title = title + "  v" + packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(Tag, e.toString(), e);
        }

        final AlertDialog.Builder aboutDialog =
                new AlertDialog.Builder(this);
        aboutDialog.setTitle(title);
        aboutDialog.setMessage(Constant.About);
        aboutDialog.setPositiveButton(R.string.ok,
                (dialog, which) -> {

                });
        aboutDialog.setNegativeButton(R.string.source_code, (dialog, which) -> {
            try {
                Uri uri = Uri.parse(Constant.SourceCodeUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            } catch (Exception e) {
                Log.w(Tag, e.toString(), e);
            }
        });
        aboutDialog.show();
    }

    enum MenusType {
        MenuStatusBar, MenuKeepScreen, MenuSoundBooster, MenuTimerSetting, MenuAbout
    }

    public void showMenu(View view) {
        PopupMenu popupMenu = getPopupMenu(view);

        Menu menu = popupMenu.getMenu();
        menu.clear();

        if (isKeepScreen) {
            menu.add(1,  MenusType.MenuKeepScreen.ordinal(), 1, R.string.keep_screen_off);
        } else {
            menu.add(1, MenusType.MenuKeepScreen.ordinal(), 1, R.string.keep_screen_on);
        }

        if (soundBooster) {
            menu.add(1,  MenusType.MenuSoundBooster.ordinal(), 1, R.string.sound_booster_off);
        } else {
            menu.add(1, MenusType.MenuSoundBooster.ordinal(), 1, R.string.sound_booster_on);
        }

        menu.add(1, MenusType.MenuTimerSetting.ordinal(), 1, R.string.timer_setting);

        menu.add(1, MenusType.MenuAbout.ordinal(), 1, R.string.about);
        popupMenu.show();
    }

    @NonNull
    private PopupMenu getPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (MenusType.values()[item.getItemId()]) {
                case MenuKeepScreen -> toggleKeepScreen();
                case MenuSoundBooster -> toggleSoundBooster();
                case MenuTimerSetting -> showTimerSetting();
                case MenuAbout -> showAbout();
                default ->
                        throw new IllegalStateException("Unexpected value: " + MenusType.values()[item.getItemId()]);
            }
            return true;
        });
        return popupMenu;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        resizeWindow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Tag, "onResume");
        resizeWindow();
        if (isKeepScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        resetMetronome();
    }

    @Override
    protected void onPause() {
        Log.d(Tag, "onPause");
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
