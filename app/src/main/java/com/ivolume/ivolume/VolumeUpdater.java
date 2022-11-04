package com.ivolume.ivolume;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;

// when context changes, call this updater to update phone volume
// this updater should maintain a table from context (tuple) to volume (int, etc.)
// if the table miss current context, insert <current context, current volume> to table
// maybe consider implementing as singleton for easy use?


public class VolumeUpdater extends Service {
//    public static VolumeUpdater mVolumeUpdater;
    private AudioManager mAudioManager;
    private final Context mContext;

//    public static VolumeUpdater getInstance() {
//        if (mVolumeUpdater == null) {
//            mVolumeUpdater = new VolumeUpdater();
//        }
//        return mVolumeUpdater;
//    }

    public VolumeUpdater (Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void update() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        }
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }
}
