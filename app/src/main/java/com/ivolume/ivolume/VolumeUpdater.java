package com.ivolume.ivolume;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;
import java.lang.Math;

// when context changes, call this updater to update phone volume
// this updater should maintain a table from context (tuple) to volume (int, etc.)
// if the table miss current context, insert <current context, current volume> to table
// maybe consider implementing as singleton for easy use?


class ContextInfo {
    public int gps;
    public int app;
    public boolean plugged;

    ContextInfo(int gps, int app, boolean plugged) {
        this.gps = gps;
        this.app = app;
        this.plugged = plugged;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) return false;
        ContextInfo o = (ContextInfo) other;
        return this.gps == o.gps && this.app == o.app && this.plugged == o.plugged;
    }

    @Override
    public int hashCode() {
        return 193 * gps + 24593 * app + (plugged ? 53 : 0);
    }
}


class NoiseAdjuster {
    // TODO decide init parameter through experiment
    private double noise_a_plugged = 0.2;
    private final double noise_b_plugged = -40;
    private double noise_a_unplugged = 0.2;
    private final double noise_b_unplugged = -40;
    private final static double lambda = 0.5;

    // f(noise) = round(a * (noise + b))
    public int adjust(double noise, boolean plugged) {
        return (int) Math.round((plugged ? noise_a_plugged : noise_a_unplugged)
                * (noise + (plugged ? noise_b_plugged : noise_b_unplugged)));
    }

    public void feedback(double noise, int delta_volume, boolean plugged) {
        if (plugged) {
            noise_a_plugged += noise_a_plugged * lambda * delta_volume / (noise + noise_b_plugged);
        } else {
            noise_a_unplugged += noise_a_unplugged * lambda * delta_volume / (noise + noise_b_unplugged);
        }
    }
}


public class VolumeUpdater extends Service {
    public static VolumeUpdater mVolumeUpdater;
//    private AudioManager mAudioManager;
    private final HashMap<ContextInfo, Integer> mMap;
    private final NoiseAdjuster mNoiseAdjuster;
    private final static double lambda = 0.5;
    private final static double mu = 0.2;

    public static VolumeUpdater getInstance() {
        if (mVolumeUpdater == null) {
            mVolumeUpdater = new VolumeUpdater();
        }
        return mVolumeUpdater;
    }

    public VolumeUpdater() {
        this.mMap = new HashMap<>();
        this.mNoiseAdjuster = new NoiseAdjuster();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void feedback(Context context, int gps, int app, boolean plugged, double noise, int qa_result) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // single entry
        Integer old_target = this.mMap.get(new ContextInfo(gps, app, plugged));
        int current_volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int current_target = current_volume - this.mNoiseAdjuster.adjust(noise, plugged);
        if (old_target != null) {
            int new_target = (int) Math.round(current_target * lambda + old_target * (1 - lambda));
            this.mMap.replace(new ContextInfo(gps, app, plugged), new_target);
            Log.d("VUT", String.format("change context <gps=%d, app=%d, plugged=%b, noise=%.2f> from <%d> to <%d>",
                    gps, app, plugged, noise, old_target, new_target));
        } else {  // regard as not-hit context and we don't want to insert it
            return;
        }

        // volume *= (1 + alpha * mu)
        // alpha = delta / old_target
        switch (qa_result) {
            case 0:  // gps
                for (HashMap.Entry<ContextInfo, Integer> entry : this.mMap.entrySet()) {
                    if (entry.getKey().gps == gps) {
                        entry.setValue((int) Math.round(entry.getValue() * (1 + mu * (current_target - old_target) / old_target)));
                        Log.d("VUT", String.format("change context <gps=%d, app=%d, plugged=%b, noise=%.2f> to <%d>",
                                entry.getKey().gps, entry.getKey().app, entry.getKey().plugged, 0.0, entry.getValue()));
                    }
                }
                break;
            case 1:  // app
                for (HashMap.Entry<ContextInfo, Integer> entry : this.mMap.entrySet()) {
                    if (entry.getKey().app == app) {
                        entry.setValue((int) Math.round(entry.getValue() * (1 + mu * (current_target - old_target) / old_target)));
                        Log.d("VUT", String.format("change context <gps=%d, app=%d, plugged=%b, noise=%.2f> to <%d>",
                                entry.getKey().gps, entry.getKey().app, entry.getKey().plugged, 0.0, entry.getValue()));
                    }
                }
                break;
            case 2:  // plugged
                for (HashMap.Entry<ContextInfo, Integer> entry : this.mMap.entrySet()) {
                    if (entry.getKey().plugged == plugged) {
                        entry.setValue((int) Math.round(entry.getValue() * (1 + mu * (current_target - old_target) / old_target)));
                        Log.d("VUT", String.format("change context <gps=%d, app=%d, plugged=%b, noise=%.2f> to <%d>",
                                entry.getKey().gps, entry.getKey().app, entry.getKey().plugged, 0.0, entry.getValue()));
                    }
                }
                break;
            case 3:  // noise
                this.mNoiseAdjuster.feedback(noise, current_target - old_target, plugged);
                break;
            default:
                break;
        }
    }

    // new_volume = target + f(noise, plugged)
    public void update(Context context, int gps, int app, boolean plugged, double noise) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        Integer target = this.mMap.get(new ContextInfo(gps, app, plugged));
        int current_volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (target == null) {  // does not contain current context
            this.mMap.put(new ContextInfo(gps, app, plugged), current_volume - this.mNoiseAdjuster.adjust(noise, plugged));
            Log.d("VU", String.format("add new context <gps=%d, app=%d, plugged=%b> (noise=%.2f), now contains %d entries",
                    gps, app, plugged, noise, this.mMap.size()));

        } else {
            target = target + this.mNoiseAdjuster.adjust(noise, plugged);
            int steps = Math.abs(target - current_volume);
            int direction = target > current_volume ? AudioManager.ADJUST_RAISE
                    : AudioManager.ADJUST_LOWER;
            for (int i = 0; i < steps; ++i) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction,
                        AudioManager.FLAG_SHOW_UI);
            }
            Log.d("VU", String.format("map context <gps=%d, app=%d, plugged=%b> (noise=%.2f) to volume <%d>",
                    gps, app, plugged, noise, target));
        }
    }
}
