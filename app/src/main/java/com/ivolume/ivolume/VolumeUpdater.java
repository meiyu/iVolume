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
    private final int gps;
    private final int app;
    private final boolean plugged;
    private final float noise;
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
    //    public static VolumeUpdater mVolumeUpdater;
    private AudioManager mAudioManager;
    private final Context mContext;
    private final HashMap<ContextInfo, Integer> mMap;

//    public static VolumeUpdater getInstance() {
//        if (mVolumeUpdater == null) {
//            mVolumeUpdater = new VolumeUpdater();
//        }
//        return mVolumeUpdater;
//    }

    public VolumeUpdater(Context mContext) {
        this.mContext = mContext;
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

    public void update(int gps, int app, boolean plugged, float noise) {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
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
