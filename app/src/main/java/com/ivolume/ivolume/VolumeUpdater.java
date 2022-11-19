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
    public final int gps;
    public final int app;
    public final boolean plugged;
    public final float noise;
    private final int hash;

    ContextInfo(int gps, int app, boolean plugged, float noise) {
        this.gps = gps;
        this.app = app;
        this.plugged = plugged;
        this.noise = noise;
        this.hash = 193 * gps + 24593 * app + (plugged ? 53 : 0);  // does not consider noise
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) return false;
        ContextInfo o = (ContextInfo) other;
        int noiseErr = 1;  // TODO change noise error tolerance
        return this.gps == o.gps && this.app == o.app && this.plugged == o.plugged
                && Math.abs(this.noise - o.noise) < noiseErr;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }
}


public class VolumeUpdater extends Service {
    public static VolumeUpdater mVolumeUpdater;
    private AudioManager mAudioManager;
    private final HashMap<ContextInfo, Integer> mMap;
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
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        this.mAudioManager = (AudioManager) this.mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    public void updateTable(Context context, int gps, int app, boolean plugged, float noise, int qa_result) {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        }
        // single entry
        Integer old_target = this.mMap.get(new ContextInfo(gps, app, plugged, noise));
        int current_volume = this.mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (old_target != null) {
            int new_target = (int) Math.round(current_volume * lambda + old_target * (1 - lambda));
            this.mMap.replace(new ContextInfo(gps, app, plugged, noise), new_target);
            Log.d("VUT", String.format("change context <gps=%d, app=%d, plugged=%b, noise=%.2f> from <%d> to <%d>",
                    gps, app, plugged, noise, old_target, new_target));
        } else {  // regard as not-hit context and we don't want to insert it
            return;
        }

        // volume *= (1 + alpha * mu)
        // alpha = delta / old_target
        switch (qa_result) {
            case -1:
                return;
            case 0:  // gps
            case 1:  // app
            case 2:  // plugged
                for (HashMap.Entry<ContextInfo, Integer> entry : this.mMap.entrySet()) {
                    if (entry.getKey().plugged == plugged) {
                        entry.setValue((int) Math.round(entry.getValue() * (1 + mu * (current_volume - old_target) / old_target)));
                        Log.d("VUT", String.format("change context <gps=%d, app=%d, plugged=%b, noise=%.2f> to <%d>",
                                entry.getKey().gps, entry.getKey().app, entry.getKey().plugged, 0.0, entry.getValue()));
                    }
                }
                break;
            case 3:  // noise
        }

        // if q != -1
        // map context
    }

    // new_volume = target + f(noise, plugged)
    public void update(Context context, int gps, int app, boolean plugged, float noise) {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }
        Integer target = this.mMap.get(new ContextInfo(gps, app, plugged, noise));
        int current_volume = this.mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (target == null) {  // does not contain current context
            this.mMap.put(new ContextInfo(gps, app, plugged, noise), current_volume);
            Log.d("VU", String.format("add new context <gps=%d, app=%d, plugged=%b, noise=%.2f>, now contains %d entries",
                    gps, app, plugged, noise, this.mMap.size()));

        } else {
            int steps = Math.abs(target - current_volume);
            int direction = target > current_volume ? AudioManager.ADJUST_RAISE
                    : AudioManager.ADJUST_LOWER;
            for (int i = 0; i < steps; ++i) {
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction,
                        AudioManager.FLAG_SHOW_UI);
            }
            Log.d("VU", String.format("map context <gps=%d, app=%d, plugged=%b, noise=%.2f> to volume <%d>",
                    gps, app, plugged, noise, target));
        }
    }
}
