package com.madao.simplebeat;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;

import java.util.Arrays;

public class Metronome extends Thread {
	private final static String Tag = "Metronome";
	private AudioTrack audioTrack;
	private boolean playing = false;
	private boolean quit = false;

	private final Handler mHandler;

	private int bpm = 120;
	private int notes = 4;

	private boolean changed = false;
	private boolean booster = false;

	private byte[] wave;

	private long startTime;
	private int tickCount;

	private AudioData audioData;

	public Metronome(Handler handler) {
		setDaemon(true);
		this.mHandler = handler;
		tickCount = 0;
		createPlayer();
	}

	private void createPlayer() {
		audioTrack = new AudioTrack.Builder()
				.setAudioAttributes(new AudioAttributes.Builder()
						.setUsage(AudioAttributes.USAGE_MEDIA)
						.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
						.build())
				.setAudioFormat(new AudioFormat.Builder()
						.setEncoding(AudioFormat.ENCODING_PCM_16BIT)
						.setSampleRate(Constant.SampleRate)
						.setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
						.build())
				.setTransferMode(AudioTrack.MODE_STREAM)
//				.setBufferSizeInBytes(Constant.SampleRate)
//				.setBufferSizeInBytes(sampleRate * 2)
				.build();
	}

	private byte[] soundBooster(byte[] origin) {
		byte[] target = new byte[origin.length];

		// add 20db
		double multiple = Math.pow(10, 20.0/20);

		for (int i = 0; i < origin.length; i += 2) {
			short volume = (short)((origin[i] & 0xFF) | (origin[i + 1] << 8));
			int temp = (int)(volume * multiple);
			if (temp > Short.MAX_VALUE) {
				volume = Short.MAX_VALUE;
			} else if (temp < Short.MIN_VALUE) {
				volume = Short.MIN_VALUE;
			} else {
				volume = (short)temp;
			}

			target[i] = (byte)(volume & 0xFF);
			target[i + 1] = (byte)((volume >> 8) & 0xFF);
		}
		return target;
	}

	private void generateSection() {
		// default sampleRate 44100Hz 16 bit
		int total = (int) (Constant.SampleRate * 2 * 60 * notes * 1f / bpm);
		// align 4 byte
		if (total % 2 > 0) {
			total = total - (total % 2);
		}
		int unit = (int) (total * 1f / notes);
		if (unit % 2 > 0) {
			unit = unit - (unit % 2);
		}
		wave = new byte[total];
		Arrays.fill(wave, (byte)0);

		byte[] beat1 = audioData.getDownbeat();
		byte[] beat2 = audioData.getUpbeat();
		if (booster) {
			beat1 = soundBooster(beat1);
			beat2 = soundBooster(beat2);
		}

		System.arraycopy(beat1, 0, wave, 0, Math.min(beat1.length, unit));

		for (int i = 1; i < notes; i++) {
			System.arraycopy(beat2, 0, wave, i * unit, Math.min(beat2.length, unit));
		}

		changed = false;
		Log.d(getName(), String.format("section total %d unit %d wave length %d time %f", total, unit, wave.length, wave.length * 1f / Constant.SampleRate ));
	}

	private void pushAudioStream() {
		audioTrack.write(wave, 0, wave.length);
		tickCount += notes;
		long endTime = System.currentTimeMillis();
		mHandler.sendMessage(Messages.TickTime((int)(endTime - startTime), tickCount));
	}

	@Override
	@SuppressWarnings({"BusyWait"})
	public void run() {
		while(!quit) {
			if (playing) {
				if (changed) {
					generateSection();
				}
				pushAudioStream();
			} else {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Log.w(Tag, e.toString(), e);
				}
			}
		}
	}

	public void pause() {
		playing = false;
		audioTrack.pause();
		audioTrack.flush();
	}

	public void play() {
		playing = true;
		audioTrack.play();
		tickCount = 0;
		startTime = System.currentTimeMillis();
	}

	public void close() {
		if (quit) {
			return;
		}
		playing = false;
		audioTrack.stop();
		audioTrack.release();
		quit = true;
	}

	public void setBpm(int bpm) {
		this.changed = true;
		this.bpm = bpm;
	}

	public void setNotes(int notes) {
		this.changed = true;
		this.notes = notes;
	}

	public void setAudioData(AudioData audioData) {
		this.audioData = audioData;
		this.changed = true;
	}

	public void setBooster(boolean value) {
		this.changed = true;
		this.booster = value;
	}
}
