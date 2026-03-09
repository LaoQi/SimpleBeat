package com.madao.simplebeat;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class MetronomeService extends Service {

    private static final String Tag = "MetronomeService";
    private static final String ChannelId = "metronome_playback";
    private static final int NotificationId = 1001;

    public class LocalBinder extends Binder {
        public MetronomeService getService() {
            return MetronomeService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable stopRunnable = this::stop;

    private Metronome metronome;
    private AudioManager audioManager;
    private Profile profile;

    private boolean isPlaying;
    private boolean soundBooster;
    private boolean backgroundPlaybackEnabled;
    private int bpm;
    private String audioKey;

    // Use elapsedRealtime so playback/timer state survives rotation and wall-clock changes.
    // 使用 elapsedRealtime，避免旋转屏幕或系统时间变化打断播放/计时状态。
    private long playStartElapsed;
    private long timerDurationMillis;
    private long timerEndElapsed;

    @Override
    public void onCreate() {
        super.onCreate();
        profile = new Profile(this);
        audioManager = new AudioManager(this);
        createNotificationChannel();

        bpm = profile.getBPM();
        soundBooster = profile.getSoundBooster();
        backgroundPlaybackEnabled = profile.getBackgroundPlayback();
        audioKey = profile.getAudioKey();
        timerDurationMillis = profile.getTimer() * 1000L;

        ensureMetronome();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ensureMetronome();
        return START_NOT_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (!isPlaying) {
            stopForegroundCompat();
        }
        return false;
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        if (metronome != null) {
            metronome.close();
            metronome = null;
        }
        stopForegroundCompat();
        super.onDestroy();
    }

    private void ensureMetronome() {
        if (metronome != null) {
            return;
        }
        // The audio engine lives in the service so playback survives activity recreation.
        // 将音频引擎放在 Service 中，确保 Activity 重建后播放不会中断。
        metronome = new Metronome(new Handler(Looper.getMainLooper(), msg -> true));
        metronome.setBpm(bpm);
        metronome.setBooster(soundBooster);
        metronome.start();
        try {
            metronome.setAudioData(audioManager.getAudio(audioKey));
        } catch (AudioManager.AudioDataNotFound | IOException exception) {
            Log.w(Tag, "Failed to load audio " + audioKey, exception);
            audioKey = Constant.AudioDefault;
            profile.setAudioKey(audioKey);
            try {
                metronome.setAudioData(audioManager.getAudio(audioKey));
            } catch (AudioManager.AudioDataNotFound | IOException fallbackException) {
                Log.e(Tag, "Failed to load default audio", fallbackException);
            }
        }
    }

    public void handleActivityStart() {
        stopForegroundCompat();
    }

    public void handleActivityStop() {
        if (!isPlaying) {
            stopForegroundCompat();
            return;
        }
        if (backgroundPlaybackEnabled) {
            // Promote to a foreground service only when playback must outlive the UI.
            // 仅在播放需要脱离界面继续运行时提升为前台服务。
            startForeground(NotificationId, buildNotification());
            return;
        }
        stop();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public int getBpm() {
        return bpm;
    }

    public String getAudioKey() {
        return audioKey;
    }

    public boolean isSoundBoosterEnabled() {
        return soundBooster;
    }

    public boolean isBackgroundPlaybackEnabled() {
        return backgroundPlaybackEnabled;
    }

    public boolean hasTimer() {
        return timerDurationMillis > 0;
    }

    public long getDisplayTimeMillis() {
        if (timerDurationMillis > 0) {
            if (!isPlaying) {
                return timerDurationMillis;
            }
            return Math.max(0, timerEndElapsed - SystemClock.elapsedRealtime());
        }
        if (!isPlaying) {
            return 0;
        }
        return Math.max(0, SystemClock.elapsedRealtime() - playStartElapsed);
    }

    public void setBpm(int value) {
        bpm = value;
        profile.setBpm(value);
        ensureMetronome();
        metronome.setBpm(value);
        refreshNotification();
    }

    public void setAudioKey(String value) throws AudioManager.AudioDataNotFound, IOException {
        ensureMetronome();
        metronome.setAudioData(audioManager.getAudio(value));
        audioKey = value;
        profile.setAudioKey(value);
        refreshNotification();
    }

    public void setSoundBooster(boolean value) {
        soundBooster = value;
        profile.setSoundBooster(value);
        ensureMetronome();
        metronome.setBooster(value);
    }

    public void setBackgroundPlaybackEnabled(boolean value) {
        backgroundPlaybackEnabled = value;
        profile.setBackgroundPlayback(value);
        if (!value) {
            stopForegroundCompat();
        } else if (isPlaying) {
            refreshNotification();
        }
    }

    public void setTimerDurationMillis(long value) {
        timerDurationMillis = Math.max(0, value);
        profile.setTimer((int) (timerDurationMillis / 1000));
        if (isPlaying) {
            // Store the absolute deadline so countdown resumes correctly after rotation/background.
            // 记录绝对截止时间，确保旋转屏幕或切到后台后倒计时仍能正确恢复。
            timerEndElapsed = SystemClock.elapsedRealtime() + timerDurationMillis;
            scheduleStop();
            refreshNotification();
        }
    }

    public void play() {
        ensureMetronome();
        metronome.play();
        isPlaying = true;
        playStartElapsed = SystemClock.elapsedRealtime();
        if (timerDurationMillis > 0) {
            timerEndElapsed = playStartElapsed + timerDurationMillis;
            scheduleStop();
        } else {
            mainHandler.removeCallbacks(stopRunnable);
        }
        refreshNotification();
    }

    public void stop() {
        if (metronome != null) {
            metronome.pause();
        }
        isPlaying = false;
        playStartElapsed = 0;
        timerDurationMillis = 0;
        timerEndElapsed = 0;
        profile.setTimer(0);
        mainHandler.removeCallbacks(stopRunnable);
        stopForegroundCompat();
    }

    private void scheduleStop() {
        mainHandler.removeCallbacks(stopRunnable);
        if (timerDurationMillis <= 0) {
            return;
        }
        long delay = Math.max(0, timerEndElapsed - SystemClock.elapsedRealtime());
        mainHandler.postDelayed(stopRunnable, delay);
    }

    private void refreshNotification() {
        if (!isPlaying || !backgroundPlaybackEnabled) {
            return;
        }
        if (!canPostNotifications()) {
            return;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NotificationId, buildNotification());
        }
    }

    private boolean canPostNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MetronomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String contentText = getString(R.string.notification_playing, bpm, audioKey);
        if (timerDurationMillis > 0) {
            long seconds = getDisplayTimeMillis() / 1000;
            contentText = getString(R.string.notification_playing_with_timer, bpm, audioKey, seconds);
        }

        return new NotificationCompat.Builder(this, ChannelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                ChannelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        manager.createNotificationChannel(channel);
    }

    private void stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
    }
}
