package com.ivolume.ivolume;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class NoiseDetector {

    private static final String TAG = "AudioRecord";
    static final int SAMPLE_RATE_IN_HZ = 8000;
    static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
            AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
    AudioRecord mAudioRecord;
    boolean isGetVoiceRun;
    Object mLock;
    public Integer volume_level;
    public double volume;

    public NoiseDetector() {
        mLock = new Object();
    }

    //default : 0; low : 1; mid : 2; high : 3
    public Integer getNoiseLevel(double db) {
        if (db <= 0)
            return 0;
        else if (db <= 35)
            return 1;
        else if (db <= 60)
            return 2;
        else
            return 3;
    }

    // 从麦克风采集声音数据，一段时间后拿平均值作为背景声音大小
    public void get_volume() {
        if (isGetVoiceRun) {
            Log.e(TAG, "还在录着呢");
        }
        try {
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT,
                    AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        if (mAudioRecord == null) {
            Log.e("sound", "mAudioRecord初始化失败");
        }
        isGetVoiceRun = true;
        mAudioRecord.startRecording();
        short[] buffer = new short[BUFFER_SIZE];
        int noise_count = 1;
        volume = 0;
        Log.d("Audio", String.format("%b %d", isGetVoiceRun, noise_count));
        while (noise_count <= 10) {
            int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
            long v = 1;
            for (int i = 0; i < buffer.length; i++) {
                v += buffer[i] * buffer[i];
                Log.d("buffer",String.valueOf(buffer[i]));
            }
            double mean = v / (double) r;
            Log.d("volume_1",String.valueOf(volume));
            Log.d("volume_count",String.valueOf(noise_count));
            volume = (10 * Math.log10(mean) + volume * noise_count) / (noise_count + 1);
            volume_level = getNoiseLevel(volume);
            Log.d("volume_mean",String.valueOf(mean));
            Log.d("volume_2",String.valueOf(volume));
            Log.d("volume_level",String.valueOf(volume_level));
            // 大概一秒十次
            synchronized (mLock) {
                try {
                    mLock.wait(100);
                    noise_count++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Log.d("Audio", "Start recording");
//                mAudioRecord.startRecording();
//                short[] buffer = new short[BUFFER_SIZE];
//                int noise_count = 1;
//                volume = 0;
//                Log.d("Audio", String.format("%b %d", isGetVoiceRun, noise_count));
//                while (noise_count <= 10) {
//                    Log.d("Audio", "is get voice run");
//                    int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
//                    long v = 0;
//                    for (int i = 0; i < buffer.length; i++) {
//                        v += buffer[i] * buffer[i];
//                    }
//                    double mean = v / (double) r;
//                    volume = (10 * Math.log10(mean) + volume * noise_count) / (noise_count + 1);
//                    volume_level = getNoiseLevel(volume);
//                    Log.d(TAG, "db:" + volume);
//                    Log.d(TAG, "db level:" + volume_level);
//                    // 大概一秒十次
//                    synchronized (mLock) {
//                        Log.d("Audio", "syn mlock");
//                        try {
//                            mLock.wait(100);
//                            noise_count++;
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//                mAudioRecord.stop();
//                mAudioRecord.release();
//                mAudioRecord = null;
//                Log.d("Audio", "End recording");
//            }
//        }).start();
        isGetVoiceRun = false;
    }

    // 调用这个函数获取背景声音大小
    public double getNoise() {  // main entrance, call this on
        get_volume();
        return volume;
    }
}