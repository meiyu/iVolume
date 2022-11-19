package com.ivolume.ivolume;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class NoiseDetector {

    private final long initialDelay = 5000;
    private final long samplePeriod = 10;  // sample max amplitude every 10ms
    private final int intervalDetection = 2000; // detect db every 2s
    private static final String TAG = "NoiseDetector";
    public double lastNoise = 0;
    private double lastTriggerNoise = 0;
    private boolean hasFirstDetection = false;
    private long lastTimestamp = 0;
    private final double threshold = 20;
    public static Integer latest_noiseLevel;

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
