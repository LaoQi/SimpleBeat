package com.madao.simplebeat;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    private final static String tag = "SimpleBeat";

    private Metronome metronome;
    private boolean isPlaying = false;
    private boolean showStatusBar = false;
    private TextView statusBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("");

        statusBar = (TextView) findViewById(R.id.statusBar);
        updateStatusBar(0, 0);
        statusBar.setVisibility(View.INVISIBLE);

        AudioManager audios = new AudioManager(this);
        metronome = new Metronome(new Handler(Looper.getMainLooper(), msg -> {
            switch (msg.what) {
                case Messages.MsgTickTime:
                    this.updateStatusBar(msg.arg1, msg.arg2);
//                    Log.d(tag, String.format("delta %d count %d", msg.arg1, msg.arg2));
                    break;
                default:
                    break;
            }
            return false;
        }));
        try {
            AudioData audioData = audios.getAudio("classic");
            metronome.setUpbeat(audioData.getUpbeat());
            metronome.setDownbeat(audioData.getDownbeat());
        } catch (AudioManager.AudioDataNotFound | IOException audioDataNotFound) {
            audioDataNotFound.printStackTrace();
        }
        metronome.start();

        NumberPicker bpmPicker = (NumberPicker) findViewById(R.id.bpmPicker);
        // 设置NumberPicker属性
        bpmPicker.setMinValue(60);
        bpmPicker.setMaxValue(240);
        bpmPicker.setValue(120);
        bpmPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        // 监听数值改变事件
        bpmPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            metronome.setBpm(newVal);
        });
    }

    @SuppressLint("DefaultLocale")
    public void updateStatusBar(int delta, int ticks) {
        statusBar.setText(String.format("Ticks: %d  -  Time: %d ms", ticks, delta));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (showStatusBar) {
            menu.add(1, 1, 1, R.string.hidden_ticks);
        } else {
            menu.add(1, 1, 1, R.string.show_ticks);
        }
        menu.add(1, 2, 1, R.string.about);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuItem m;
        if (showStatusBar) {
            menu.add(1, 1, 1, R.string.hidden_ticks);
        } else {
            menu.add(1, 1, 1, R.string.show_ticks);
        }
        menu.add(1, 2, 1, R.string.about);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                showStatusBar = !showStatusBar;
                statusBar.setVisibility(showStatusBar ? View.VISIBLE : View.INVISIBLE);
                break;
            case 2:
                showAbout();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    public void onStartStopClick(View view) {
        if (isPlaying) {
            metronome.pause();
            isPlaying = false;
        } else {
            updateStatusBar(0, 0);
            metronome.play();
            isPlaying = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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