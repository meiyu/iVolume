package com.ivolume.ivolume;

import android.util.Log;
import android.media.MediaRecorder;

import java.util.List;
import java.io.File;

public class NoiseDetector_backup {

    private final long initialDelay = 5000;
    private final long samplePeriod = 10;  // sample max amplitude every 10ms
    private final int intervalDetection = 2000; // detect db every 2s
    private static final String TAG = "NoiseDetector";
    public double lastNoise = 0;
    public double lastTriggerNoise = 0;
    public boolean hasFirstDetection = false;
    public long lastTimestamp = 0;
    private final double threshold = 20;
    public static Integer latest_noiseLevel;
    public MediaRecorder mMediaRecorder;

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
    /* set noise */
    private void setPresentNoise(double noise) {
        lastNoise = noise;
        latest_noiseLevel = getNoiseLevel(noise);
    }

    public MediaRecorder startNewMediaRecorder(int audioSource, String outputFilePath) throws Exception {
        MediaRecorder mediaRecorder = new MediaRecorder();
        // may throw IllegalStateException due to lack of permission
        mediaRecorder.setAudioSource(audioSource);
        mediaRecorder.setAudioChannels(2);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setAudioEncodingBitRate(16 * 44100);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(outputFilePath);
        mediaRecorder.prepare();
        mediaRecorder.start();
        return mediaRecorder;
    }

    private void startRecording(File file) throws Exception{
        mMediaRecorder = startNewMediaRecorder(MediaRecorder.AudioSource.MIC, file.getAbsolutePath());
    }

    private void stopMediaRecorder(MediaRecorder mediaRecorder) {
        if (mediaRecorder != null) {
            try {
                // may throw IllegalStateException because no valid audio data has been received
                mediaRecorder.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        stopMediaRecorder(mMediaRecorder);
    }

    public int getMaxAmplitude() {
        try {
            // Returns the maximum absolute amplitude that was sampled since the last call to this method
            return mMediaRecorder.getMaxAmplitude();
        } catch (Exception e) {
            return -1;
        }
    }



    private double getAvgNoiseFromSeq(List<Integer> seq) {
        double BASE = 1.0; // 32768
        double sum = 0.0;
        double sum_squared = 0.0;
        int count = 0;

        int idx = 0;
        double db;
        int next_idx;
        double next_db;
        int maxAmplitude = 0;
        int current_maxAmplitude;

        // 找到第一个非零值
        while (idx < seq.size() && (maxAmplitude = seq.get(idx)) == 0) {
            idx++;
        }
        db = 20 * Math.log10(maxAmplitude / BASE);
        current_maxAmplitude = maxAmplitude;
        next_idx = idx + 1;
//            Log.e(TAG, "getNoiseLevel: " + String.format("idx: %d maxAmplitude: %d db: %f", idx, maxAmplitude, db));
        // 采样为0时使用两边非零值线性插值
        while (true) {
            while (next_idx < seq.size() && (maxAmplitude = seq.get(next_idx)) == 0) {
                next_idx++;
            }
            if (next_idx >= seq.size()) {
                sum += db;
                sum_squared += current_maxAmplitude * current_maxAmplitude;
                count += 1;
                break;
            }
            next_db = 20 * Math.log10(maxAmplitude / BASE);
            sum += db + (db + next_db) * 0.5 * (next_idx - idx - 1);
            double interp = (current_maxAmplitude + maxAmplitude) * 0.5;
            sum_squared += current_maxAmplitude * current_maxAmplitude + interp * interp * (next_idx - idx - 1);
            count += next_idx - idx;

            idx = next_idx++;
            db = next_db;
            current_maxAmplitude = maxAmplitude;
        }

        double rms = Math.sqrt(sum_squared / count);
        double average_noise = (count > 0)? (sum / count) : 0.0;

        Log.e(TAG, String.format("getAvgNoiseFromSeq: %d sampled, average %fdb", count, average_noise));
        return average_noise;
    }
    
    float getNoise() {  // main entrance, call this on
        return latest_noiseLevel;
    }
}
