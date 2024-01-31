package com.madao.simplebeat;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MetronomeActivity extends AppCompatActivity {

    enum MenusType {
        MenuStatusBar, MenuKeepScreen, MenuSoundBooster, MenuTimerSetting, MenuAbout
    }

    private Metronome metronome;
    private AudioManager audioManager;
    private Profile profile;
    private boolean isPlaying = false;
    private boolean showStatusBar = false;
    private boolean soundBooster = false;
    private boolean isKeepScreen;
    private int audioInitPosition;
    private TextView statusBar;
    private TextView timerBar;
    private long startTime;

    private long timeCounter = 0;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metronome);
        setTitle("");

        profile = new Profile(this);
        isKeepScreen = profile.getKeepScreen();
        soundBooster = profile.getSoundBooster();
        audioManager = new AudioManager(this);

        mHandler = new Handler(Looper.getMainLooper(), msg -> {
            if (msg.what == Messages.MsgTickTime) {
                this.updateStatusBar(msg.arg1, msg.arg2);
//                    Log.d(tag, String.format("delta %d count %d", msg.arg1, msg.arg2));
            } else if (msg.what == Messages.MsgUpdateTimer) {
                updateTimerBar();
            }
            return false;
        });

        audioInitPosition = audioManager.getPosition(profile.getAudioKey());

        initStatusBar();
        initBpmPicker();
        initAudioSelector();
        initTimerBar();
    }

    private void resetMetronome() {
//        isPlaying = false;
//        ImageButton view = findViewById(R.id.startButton);
//        ((AnimatedVectorDrawable) (view).getDrawable()).reset();

        metronome = new Metronome(mHandler);
        metronome.setBpm(profile.getBPM());
        metronome.setBooster(soundBooster);
        metronome.start();
        updateAudio(profile.getAudioKey());
        stop();
    }

    private void initTimerBar() {
        timerBar = findViewById(R.id.timerBar);
        new Thread(() -> {
            while (true) {
                if (isPlaying) {
                    mHandler.sendMessage(Messages.UpdateTimer());
                }
                try {
                    //noinspection BusyWait
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initStatusBar() {
        statusBar = findViewById(R.id.TicksCounter);
        updateStatusBar(0, 0);
        statusBar.setVisibility(View.INVISIBLE);
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

    @SuppressLint("DefaultLocale")
    public void updateStatusBar(int delta, int ticks) {
        statusBar.setText(String.format("Ticks: %d  -  Time: %d ms", ticks, delta));
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    public void updateTimerBar() {
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

            long minutes = lastTime / 60000;
            long seconds = (lastTime % 60000) / 1000;
            long milliseconds = lastTime % 1000;
            timerBar.setText(String.format("%d : %02d.%03d", minutes, seconds, milliseconds));
            return;
        }
        long ts = System.currentTimeMillis() - startTime;
        long minutes = ts / 60000;
        long seconds = (ts % 60000) / 1000;
        long milliseconds = ts % 1000;
        timerBar.setText(String.format("%d : %02d.%03d", minutes, seconds, milliseconds));
    }

    public void updateAudio(String selected) {
        try {
            AudioData audioData = audioManager.getAudio(selected);
            metronome.setUpbeat(audioData.getUpbeat());
            metronome.setDownbeat(audioData.getDownbeat());
            profile.setAudioKey(selected);
        } catch (AudioManager.AudioDataNotFound | IOException exception) {
            exception.printStackTrace();
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
        ImageButton view = findViewById(R.id.startButton);
        AnimatedVectorDrawable drawable = (AnimatedVectorDrawable) (view).getDrawable();
        float right = Math.min(timerBar.getRight() + 20, getWindow().getDecorView().getWidth()/2 - (view.getWidth()/2));
        metronome.pause();
        isPlaying = false;
        timeCounter = 0;
        drawable.reset();
        ObjectAnimator.ofFloat(view, "translationX", right, 0)
                .setDuration(500).start();
        ObjectAnimator.ofFloat(timerBar, "alpha", 1f, 0f)
                .setDuration(400).start();
    }

    private void play() {
        ImageButton view = findViewById(R.id.startButton);
        AnimatedVectorDrawable drawable = (AnimatedVectorDrawable) (view).getDrawable();
        updateStatusBar(0, 0);
        metronome.play();
        isPlaying = true;
        drawable.start();

        if (timeCounter == 0) {
            // has timer
            float right = Math.min(timerBar.getRight() + 20, getWindow().getDecorView().getWidth() / 2 - (view.getWidth() / 2));
            ObjectAnimator.ofFloat(view, "translationX", 0, right)
                    .setDuration(500).start();
        }

        ObjectAnimator.ofFloat(timerBar, "alpha", 0f, 1f)
                .setDuration(400).start();
        startTime = System.currentTimeMillis();
        updateTimerBar();
    }

    private void setTimerOn(int minutes, int seconds) {
        ImageButton view = findViewById(R.id.startButton);
        float right = Math.min(timerBar.getRight() + 20, getWindow().getDecorView().getWidth()/2 - (view.getWidth()/2));
        updateStatusBar(0, 0);
        ObjectAnimator.ofFloat(view, "translationX", 0, right)
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

    public void showMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (MenusType.values()[item.getItemId()]) {
                case MenuStatusBar -> {
                    showStatusBar = !showStatusBar;
                    statusBar.setVisibility(showStatusBar ? View.VISIBLE : View.INVISIBLE);
                }
                case MenuKeepScreen -> toggleKeepScreen();
                case MenuSoundBooster -> toggleSoundBooster();
                case MenuTimerSetting -> showTimerSetting();
                case MenuAbout -> showAbout();
                default ->
                        throw new IllegalStateException("Unexpected value: " + MenusType.values()[item.getItemId()]);
            }
            return true;
        });

        Menu menu = popupMenu.getMenu();
        menu.clear();
        if (showStatusBar) {
            menu.add(1,  MenusType.MenuStatusBar.ordinal(), 1, R.string.hidden_ticks);
        } else {
            menu.add(1, MenusType.MenuStatusBar.ordinal(), 1, R.string.show_ticks);
        }

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

    private void showAbout() {
        String title = getString(R.string.app_name);
        PackageManager pm = getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), 0);
            title = title + "  v" + packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        final AlertDialog.Builder aboutDialog =
                new AlertDialog.Builder(this);
        aboutDialog.setTitle(title);
        aboutDialog.setMessage(Constant.About);
        aboutDialog.setPositiveButton(R.string.ok,
                (dialog, which) -> {

                });
        aboutDialog.setNegativeButton(R.string.source_code, (dialog, which) -> {
            Uri uri = Uri.parse(Constant.SourceCodeUrl);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        });
        aboutDialog.show();
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

    @Override
    protected void onResume() {
        super.onResume();
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
