package com.madao.simplebeat;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.os.LocaleListCompat;

import java.io.IOException;

public class MetronomeActivity extends AppCompatActivity {

    private static final String Tag = "MetronomeActivity";
    private static final long UiRefreshIntervalMillis = 50L;

    private MetronomeService metronomeService;
    private boolean serviceBound;

    private AudioManager audioManager;
    private Profile profile;
    private boolean soundBooster;
    private boolean isKeepScreen;
    private boolean backgroundPlaybackEnabled;
    private ImageButton settingsButton;
    private TextView timerBar;
    private ImageButton startButton;
    private ConstraintLayout timerAndPlay;
    private TextView bpmButton;
    private TextView audioSelectorButton;
    private BpmPicker bpmPicker;
    private AudioSelector audioSelector;
    private View bpmAndAudioButtons;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    // The existing UI/animation layer is activity-owned, so it polls the service for display state.
    // 现有 UI/动画层仍归 Activity 管理，因此通过轮询 Service 同步显示状态。
    private final Runnable uiUpdater = new Runnable() {
        @Override
        public void run() {
            refreshUiFromService();
            uiHandler.postDelayed(this, UiRefreshIntervalMillis);
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MetronomeService.LocalBinder binder = (MetronomeService.LocalBinder) service;
            metronomeService = binder.getService();
            serviceBound = true;
            metronomeService.handleActivityStart();
            syncControlsFromService();
            refreshUiFromService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            metronomeService = null;
        }
    };

    private GestureDetector mGestureDetector;
    private Animator mAnimator;

    private Typeface typeface;
    private boolean lastRenderedPlaying;
    private boolean timerPreparedVisible;
    // Avoid resetting scroll-based controls on every 50 ms refresh tick.
    // 避免在每 50ms 的刷新周期里反复重置滚动控件位置。
    private int renderedBpm = -1;
    private String renderedAudioKey;

    public enum StateType {
        Normal, ShowBPM, ShowAudio
    }

    private StateType mState = StateType.Normal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Keep the app on the Chinese resource set to match the original UX.
        // 固定使用中文资源，保持与原始应用体验一致。
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh"));
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
        backgroundPlaybackEnabled = profile.getBackgroundPlayback();
        audioManager = new AudioManager(this);

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
            showTimerSetting();
            return true;
        });

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
            ConstraintLayout.LayoutParams params1 = (ConstraintLayout.LayoutParams) timerAndPlay.getLayoutParams();
            params1.verticalBias = 0.5f;
            timerAndPlay.setLayoutParams(params1);

            ConstraintLayout.LayoutParams params2 = (ConstraintLayout.LayoutParams) bpmAndAudioButtons.getLayoutParams();
            params2.verticalBias = 0.5f;
            bpmAndAudioButtons.setLayoutParams(params2);
        }

        if (isCompact) {
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
        }

        bpmPicker.Resize(unit, isCompact, typeface);
        audioSelector.Resize(unit, isCompact, typeface);
    }

    public void ChangeState(StateType state) {
        if (mState == state) {
            return;
        }
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

    @SuppressLint("SetTextI18n")
    private void initTimerBar() {
        timerBar = findViewById(R.id.timerBar);
        timerBar.setTypeface(typeface);
        timerBar.setText("00:00:00");
        mAnimator.setTimerBar(timerBar);
    }

    @SuppressLint("SetTextI18n")
    private void initBpmPicker() {
        bpmPicker = findViewById(R.id.BpmPicker);
        bpmPicker.setValue(profile.getBPM());

        bpmButton = findViewById(R.id.BpmPickerButton);
        bpmButton.setTypeface(typeface);
        bpmButton.setText(String.valueOf(profile.getBPM()));

        bpmPicker.setOnValueChangedListener((oldVal, newVal) -> {
            if (!serviceBound) {
                return;
            }
            metronomeService.setBpm(newVal);
            bpmButton.setText(String.valueOf(newVal));
        });

        bpmButton.setOnClickListener((view) -> ChangeState(StateType.ShowBPM));

        mAnimator.setBpmPicker(bpmPicker)
                .setBpmButton(bpmButton);
    }

    private void initAudioSelector() {
        audioSelector = findViewById(R.id.AudioSelector);
        audioSelector.bindData(
                audioManager.getPosition(profile.getAudioKey()),
                audioManager.getAudioList(),
                (oldVal, newVal) -> updateAudio(audioManager.getAudioList().get(newVal))
        );

        audioSelectorButton = findViewById(R.id.AudioSelectorButton);
        audioSelectorButton.setTypeface(typeface);
        audioSelectorButton.setText(profile.getAudioKey());
        audioSelectorButton.setOnClickListener((view) -> ChangeState(StateType.ShowAudio));

        mAnimator.setAudioSelector(audioSelector)
                .setAudioSelectorButton(audioSelectorButton);
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void updateTimerBar(long durationMillis) {
        long minutes = durationMillis / 60000;
        long seconds = (durationMillis % 60000) / 1000;
        long milliseconds = durationMillis % 100;
        timerBar.setText(String.format("%02d:%02d:%02d", minutes, seconds, milliseconds));
    }

    public void updateAudio(String selected) {
        if (!serviceBound) {
            return;
        }
        try {
            metronomeService.setAudioKey(selected);
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
        if (!serviceBound) {
            return;
        }
        metronomeService.setSoundBooster(!metronomeService.isSoundBoosterEnabled());
        syncControlsFromService();
    }

    public void toggleBackgroundPlayback() {
        if (!serviceBound) {
            return;
        }
        metronomeService.setBackgroundPlaybackEnabled(!metronomeService.isBackgroundPlaybackEnabled());
        syncControlsFromService();
    }

    private void setTimerOn(int minutes, int seconds) {
        if (!serviceBound) {
            return;
        }
        long durationMillis = (minutes * 60L + seconds) * 1000L;
        metronomeService.setTimerDurationMillis(durationMillis);
        refreshUiFromService();
    }

    private void applyPreparedTimerUi() {
        float bottom = (float) timerBar.getHeight();
        startButton.setTranslationY(bottom);
        timerBar.setAlpha(1f);
        timerPreparedVisible = true;
    }

    private void applyIdleUi() {
        startButton.setTranslationY(0f);
        timerBar.setAlpha(0f);
        timerPreparedVisible = false;
    }

    private void syncControlsFromService() {
        if (!serviceBound) {
            return;
        }
        soundBooster = metronomeService.isSoundBoosterEnabled();
        backgroundPlaybackEnabled = metronomeService.isBackgroundPlaybackEnabled();

        // Only touch controls when the bound service state actually changed.
        // 仅在 Service 状态实际变化时更新控件，避免无意义刷新。
        int currentBpm = metronomeService.getBpm();
        if (renderedBpm != currentBpm) {
            renderedBpm = currentBpm;
            bpmButton.setText(String.valueOf(currentBpm));
            bpmPicker.setValue(currentBpm);
        }

        String currentAudioKey = metronomeService.getAudioKey();
        if (!currentAudioKey.equals(renderedAudioKey)) {
            renderedAudioKey = currentAudioKey;
            audioSelectorButton.setText(currentAudioKey);
        }
    }

    private void refreshUiFromService() {
        if (!serviceBound) {
            return;
        }

        syncControlsFromService();

        boolean isPlaying = metronomeService.isPlaying();
        boolean hasTimer = metronomeService.hasTimer();
        updateTimerBar(metronomeService.getDisplayTimeMillis());

        if (isPlaying != lastRenderedPlaying) {
            if (isPlaying) {
                timerPreparedVisible = false;
                mAnimator.Play();
            } else if (hasTimer) {
                // Keep the countdown visible after playback stops so the user can restart it.
                // 播放停止后保留倒计时显示，方便用户直接重新开始。
                applyPreparedTimerUi();
            } else {
                mAnimator.Stop();
                timerPreparedVisible = false;
            }
            lastRenderedPlaying = isPlaying;
        } else if (!isPlaying) {
            if (hasTimer && !timerPreparedVisible) {
                applyPreparedTimerUi();
            } else if (!hasTimer && timerPreparedVisible) {
                applyIdleUi();
            }
        }
    }

    private void showTimerSetting() {
        String title = getString(R.string.timer_setting);

        final AlertDialog.Builder timeDialog = new AlertDialog.Builder(this);
        timeDialog.setTitle(title);

        View selector = View.inflate(this, R.layout.time_selector, null);
        timeDialog.setView(selector);

        final NumberPicker minutesPicker = selector.findViewById(R.id.timer_picker_minutes);
        final NumberPicker secondsPicker = selector.findViewById(R.id.timer_picker_seconds);

        minutesPicker.setMaxValue(120);
        minutesPicker.setMinValue(0);

        secondsPicker.setMaxValue(59);
        secondsPicker.setMinValue(0);

        timeDialog.setPositiveButton(R.string.ok, (dialog, which) -> {
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
        if (!serviceBound) {
            return;
        }
        if (metronomeService.isPlaying()) {
            metronomeService.stop();
        } else {
            metronomeService.play();
        }
        refreshUiFromService();
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

        final AlertDialog.Builder aboutDialog = new AlertDialog.Builder(this);
        aboutDialog.setTitle(title);
        aboutDialog.setMessage(Constant.About);
        aboutDialog.setPositiveButton(R.string.ok, (dialog, which) -> { });
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
        MenuKeepScreen, MenuSoundBooster, MenuBackgroundPlayback, MenuTimerSetting, MenuAbout
    }

    public void showMenu(View view) {
        PopupMenu popupMenu = getPopupMenu(view);

        Menu menu = popupMenu.getMenu();
        menu.clear();

        if (isKeepScreen) {
            menu.add(1, MenusType.MenuKeepScreen.ordinal(), 1, R.string.keep_screen_off);
        } else {
            menu.add(1, MenusType.MenuKeepScreen.ordinal(), 1, R.string.keep_screen_on);
        }

        if (soundBooster) {
            menu.add(1, MenusType.MenuSoundBooster.ordinal(), 1, R.string.sound_booster_off);
        } else {
            menu.add(1, MenusType.MenuSoundBooster.ordinal(), 1, R.string.sound_booster_on);
        }

        if (backgroundPlaybackEnabled) {
            menu.add(1, MenusType.MenuBackgroundPlayback.ordinal(), 1, R.string.background_playback_off);
        } else {
            menu.add(1, MenusType.MenuBackgroundPlayback.ordinal(), 1, R.string.background_playback_on);
        }

        menu.add(1, MenusType.MenuTimerSetting.ordinal(), 1, R.string.timer_setting);
        menu.add(1, MenusType.MenuAbout.ordinal(), 1, R.string.about);
        popupMenu.show();
    }

    @NonNull
    private PopupMenu getPopupMenu(View view) {
        ContextThemeWrapper themedContext = new ContextThemeWrapper(this, R.style.ThemeOverlay_SimpleBeat_PopupMenu);
        PopupMenu popupMenu = new PopupMenu(themedContext, view);
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (MenusType.values()[item.getItemId()]) {
                case MenuKeepScreen -> toggleKeepScreen();
                case MenuSoundBooster -> toggleSoundBooster();
                case MenuBackgroundPlayback -> toggleBackgroundPlayback();
                case MenuTimerSetting -> showTimerSetting();
                case MenuAbout -> showAbout();
                default -> throw new IllegalStateException("Unexpected value: " + MenusType.values()[item.getItemId()]);
            }
            return true;
        });
        return popupMenu;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        resizeWindow();
        refreshUiFromService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MetronomeService.class);
        // Start + bind lets the service keep running only when background playback requires it.
        // 同时 start 和 bind，只有在后台播放需要时 Service 才会独立继续运行。
        startService(intent);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        uiHandler.removeCallbacks(uiUpdater);
        uiHandler.post(uiUpdater);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resizeWindow();
        if (isKeepScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        profile.setKeepScreen(isKeepScreen);
        if (isKeepScreen) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onStop() {
        uiHandler.removeCallbacks(uiUpdater);
        if (serviceBound) {
            boolean shouldStopService = false;
            if (!isChangingConfigurations()) {
                // Configuration changes should rebind to the same playback session instead of tearing it down.
                // 配置变化时应重新绑定同一播放会话，而不是把当前播放直接销毁。
                metronomeService.handleActivityStop();
                shouldStopService = !metronomeService.isPlaying();
            }
            unbindService(serviceConnection);
            serviceBound = false;
            metronomeService = null;
            if (shouldStopService) {
                stopService(new Intent(this, MetronomeService.class));
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        profile.setKeepScreen(isKeepScreen);
    }
}
