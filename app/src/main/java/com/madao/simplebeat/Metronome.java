package com.madao.simplebeat;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;

import java.util.Arrays;

public class Metronome extends Thread {
	private AudioTrack audioTrack;
	private boolean playing = false;

	private final Handler mHandler;

	private int bpm = 120;
	private int notes = 4;

	private boolean changed = false;

	private byte[] wave;

	private long startTime;
	private int tickCount;

	private byte[] upbeat;
	private byte[] downbeat;
	
	public Metronome(Handler handler) {
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
//				.setBufferSizeInBytes(sampleRate)
//				.setBufferSizeInBytes(sampleRate * 2)
				.build();
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
		System.arraycopy(downbeat, 0, wave, 0, downbeat.length);

		for (int i = 1; i < notes; i++) {
			System.arraycopy(upbeat, 0, wave, i * unit, downbeat.length);
		}
		changed = false;
		Log.d(getName(), String.format("section total %d unit %d wave length %d time %f", total, unit, wave.length, wave.length * 1f / Constant.SampleRate ));
	}

	private void pushAudioStream() {
		audioTrack.write(wave, 0, wave.length);
		tickCount += notes;
		long endTime = System.currentTimeMillis();
		mHandler.sendMessage(Messages.TickTime((int)(endTime - startTime), tickCount));
//		int offset = 0;
//		while (offset < wave.length) {
//			int end = Math.min(wave.length - offset, Constant.SampleRate);
//			audioTrack.write(wave, offset, end);
//			offset += sampleRate;
//		}
	}

	@Override
	@SuppressWarnings({"InfiniteLoopStatement", "BusyWait"})
	public void run() {
		super.run();
		while(true) {
			if (playing) {
				if (changed) {
					generateSection();
				}
				pushAudioStream();
			} else {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void pause() {
		playing = false;
		audioTrack.pause();
	}

	public void play() {
		playing = true;
		generateSection();
		audioTrack.play();
		tickCount = 0;
		startTime = System.currentTimeMillis();
	}

	public void close() {
		playing = false;
		audioTrack.stop();
		audioTrack.release();
	}

	public void setBpm(int bpm) {
		this.changed = true;
		this.bpm = bpm;
	}

	public void setNotes(int notes) {
		this.changed = true;
		this.notes = notes;
	}

	public void setUpbeat(byte[] upbeat) {
		this.changed = true;
		this.upbeat = upbeat;
	}

	public void setDownbeat(byte[] downbeat) {
		this.changed = true;
		this.downbeat = downbeat;
	}
}
