package com.madao.simplebeat;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;

public class Metronome extends Thread {

	private double upbeat = 2440;
	private double downbeat = 3440;

	private final int sampleRate = 24000;
	private AudioTrack audioTrack;
	private boolean playing = false;

	private final Handler mHandler;

	private int bpm = 120;
	private int beat = 4;

	private int lastBpm = bpm;
	private int lastBeat = beat;

	private byte[] wave;

	private long debugStartTime;
	private int tickCount;
	
	public Metronome(Handler handler) {
		this.mHandler = handler;
		tickCount = 0;
		createPlayer();
		generateSection();
	}

	private void createPlayer() {
		audioTrack = new AudioTrack.Builder()
				.setAudioAttributes(new AudioAttributes.Builder()
						.setUsage(AudioAttributes.USAGE_MEDIA)
						.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
						.build())
				.setAudioFormat(new AudioFormat.Builder()
						.setEncoding(AudioFormat.ENCODING_PCM_16BIT)
						.setSampleRate(sampleRate)
						.setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
						.build())
				.setTransferMode(AudioTrack.MODE_STREAM)
//				.setBufferSizeInBytes(sampleRate)
//				.setBufferSizeInBytes(sampleRate * 2)
				.build();
	}

	private double[] getWave(int samples, double frequencyOfTone) {
		double[] sample = new double[samples];
		for (int i = 0; i < samples; i++) {
			sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/frequencyOfTone));
		}
		return sample;
	}

	private void generateSection() {
		int tick = sampleRate / 8;
		int i;
		int length = 0;
		int skip = (int) (sampleRate * 60 / bpm - tick);
		double[] beatsBuffer = new double[sampleRate * 8];
		double[] downbeatSound = getWave(tick, downbeat);
		for(double t : downbeatSound) {
			beatsBuffer[length++] = t;
		}
		for (i = 0; i < skip; i++) {
			beatsBuffer[length++] = 0;
		}
		double[] upbeatSound = getWave(tick, upbeat);
		for (i = 1; i < beat; i++) {
			for(double t : upbeatSound) {
				beatsBuffer[length++] = t;
			}
			for (int j = 0; j < skip; j++) {
				beatsBuffer[length++] = 0;
			}
		}

		wave = new byte[2 * length];
		int index = 0;
		for (i = 0; i < length; i++) {
			double sample = beatsBuffer[i];
			// scale to maximum amplitude
			short maxSample = (short) ((sample * Short.MAX_VALUE));
			// in 16 bit wav PCM, first byte is the low order byte
			wave[index++] = (byte) (maxSample & 0x00ff);
			wave[index++] = (byte) ((maxSample & 0xff00) >>> 8);
		}
		Log.d(getName(), String.format("section length %d wave size %d", length, wave.length));
	}

	private void pushAudioStream() {
		if (tickCount % 10 == 0) {
			long endTime = System.currentTimeMillis();
			Log.d(getName(), String.format("start %d delta %d count %d", debugStartTime, endTime - debugStartTime, tickCount));
		}
		int offset = 0;
		while (offset < wave.length) {
			int end = Math.min(wave.length - offset, sampleRate);
			audioTrack.write(wave, offset, end);
			offset += sampleRate;
		}
		tickCount += beat;
	}

	@Override
	@SuppressWarnings({"InfiniteLoopStatement", "BusyWait"})
	public void run() {
		super.run();
		while(true) {
			if (playing) {
				if (lastBpm != bpm || lastBeat != beat) {
					bpm = lastBpm;
					beat = lastBeat;
					generateSection();
				}
				pushAudioStream();
			} else {
				try {
					Thread.sleep(100);
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
		audioTrack.play();
		tickCount = 0;
		debugStartTime = System.currentTimeMillis();
	}

	public void close() {
		playing = false;
		audioTrack.stop();
		audioTrack.release();
	}

	public void setBpm(int bpm) {
		lastBpm = bpm;
	}

	public void setBeat(int beat) {
		lastBeat = beat;
	}
}
