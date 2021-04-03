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
import android.os.HandlerThread;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

//    private final static String tag = "SimpleBeat";

    enum MenusType {
        MenuStatusBar, MenuKeepScreen, MenuAbout
    }

    private Metronome metronome;
    private AudioManager audioManager;
    private Profile profile;
    private boolean isPlaying = false;
    private boolean showStatusBar = false;
    private boolean isKeepScreen;
    private int audioInitPosition;
    private TextView statusBar;
    private TextView timerBar;
    private RecyclerView audioSelector;
    private long startTime;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("");

        profile = new Profile(this);
        isKeepScreen = profile.getKeepScreen();
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

        metronome = new Metronome(mHandler);

        audioInitPosition = audioManager.getPosition(profile.getAudioKey());
        updateAudio(profile.getAudioKey());

        initStatusBar();
        initBpmPicker();
        initAudioSelector();
        initTimerBar();

        new Handler(getMainLooper()).postDelayed(() -> audioSelector.smoothScrollToPosition(audioInitPosition + 1), 100);
        metronome.start();
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
        statusBar = findViewById(R.id.statusBar);
        updateStatusBar(0, 0);
        statusBar.setVisibility(View.INVISIBLE);
    }

    private void initBpmPicker() {
        NumberPicker bpmPicker = findViewById(R.id.bpmPicker);
        bpmPicker.setMinValue(Constant.MinBPM);
        bpmPicker.setMaxValue(Constant.MaxBPM);
        bpmPicker.setValue(profile.getBPM());
        bpmPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        bpmPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            metronome.setBpm(newVal);
            profile.setBPM(newVal);
        });
    }

    private void initAudioSelector() {
        audioSelector = findViewById(R.id.audioSelector);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false);
        audioSelector.setLayoutManager(linearLayoutManager);
        audioSelector.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            private View lastItem;
            private int selectedPosition = 0;

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.audio_selector_item, parent, false);
                v.getLayoutParams().width = parent.getWidth() / 3;
                return new RecyclerView.ViewHolder(v) {};
            }

            public void scroll() {
                int first = linearLayoutManager.findFirstVisibleItemPosition();
                if (first == selectedPosition && selectedPosition > 0) {
                    audioSelector.smoothScrollToPosition(selectedPosition - 1);
                } else {
                    audioSelector.smoothScrollToPosition(selectedPosition + 1);
                }
            }

            protected void onClickItem(View view, int position) {
                selectedPosition = position;
                scroll();
                updateAudio(position);
                highlight(view);
                resetItem(lastItem);

                lastItem = view;
            }

            protected void highlight(View view) {
                view.setBackground(getDrawable(R.drawable.audio_item_checked_shape));
                ((TextView) view).setTextColor(getColor(R.color.blue_700));
            }

            protected void resetItem(View view) {
                view.setBackground(getDrawable(R.drawable.audio_item_normal_shape));
                ((TextView) view).setTextColor(getColor(R.color.gray_300));
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                TextView textView = holder.itemView.findViewById(R.id.audioSelectorItemName);
                textView.setText(audioManager.getAudioList().get(position));
                textView.setOnClickListener(v -> onClickItem(v, position));
                if (position == audioInitPosition) {
                    highlight(textView);
                    lastItem = textView;
                    selectedPosition = position;
                }
            }

            @Override
            public int getItemCount() {
                return audioManager.getAudioList().size();
            }
        });
    }

    @SuppressLint("DefaultLocale")
    public void updateStatusBar(int delta, int ticks) {
        statusBar.setText(String.format("Ticks: %d  -  Time: %d ms", ticks, delta));
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    public void updateTimerBar() {
        long ts = System.currentTimeMillis() - startTime;
        long minutes = ts / 60000;
        long seconds = (ts % 60000) / 1000;
        long milliseconds = ts % 1000;
        timerBar.setText(String.format("%d : %02d.%03d", minutes, seconds, milliseconds));
    }

    public void updateAudio(int position) {
        updateAudio(audioManager.getAudioList().get(position));
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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
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

        menu.add(1, MenusType.MenuAbout.ordinal(), 1, R.string.about);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (MenusType.values()[item.getItemId()]) {
            case MenuStatusBar:
                showStatusBar = !showStatusBar;
                statusBar.setVisibility(showStatusBar ? View.VISIBLE : View.INVISIBLE);
                break;
            case MenuKeepScreen:
                toggleKeepScreen();
                break;
            case MenuAbout:
                showAbout();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + MenusType.values()[item.getItemId()]);
        }
        return super.onOptionsItemSelected(item);
    }


    public void onStartStopClick(View view) {
        AnimatedVectorDrawable drawable = (AnimatedVectorDrawable) ((ImageButton)view).getDrawable();

        if (isPlaying) {
            metronome.pause();
            isPlaying = false;
            drawable.reset();
            ObjectAnimator.ofFloat(view, "translationX", 400, 0)
                    .setDuration(500).start();
            ObjectAnimator.ofFloat(timerBar, "alpha", 1f, 0f)
                    .setDuration(400).start();
        } else {
            updateStatusBar(0, 0);
            metronome.play();
            isPlaying = true;
            drawable.start();

            ObjectAnimator.ofFloat(view, "translationX", 0, 400)
                    .setDuration(500).start();

            ObjectAnimator.ofFloat(timerBar, "alpha", 0f, 1f)
                    .setDuration(400).start();

            updateTimerBar();
            startTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
    protected void onDestroy() {
        super.onDestroy();
        profile.setKeepScreen(isKeepScreen);
        metronome.close();
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
                new AlertDialog.Builder(MainActivity.this);
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
}