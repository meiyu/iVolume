package com.ivolume.ivolume;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.lang.Math;
import java.util.Locale;

// when context changes, call this updater to update phone volume
// this updater should maintain a table from context (tuple) to volume (int, etc.)
// if the table miss current context, insert <current context, current volume> to table
// maybe consider implementing as singleton for easy use?


class ContextInfo implements Serializable {
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

    @NonNull
    @Override
    public String toString() {
        return "ContextInfo{" +
                "gps=" + gps +
                ", app=" + app +
                ", plugged=" + plugged +
                '}';
    }
}


class NoiseAdjuster implements Serializable {
    // TODO decide init parameter through experiment
    private double noise_a_plugged = 0.2;
    private double noise_b_plugged = -40;
    private double noise_a_unplugged = 0.2;
    private double noise_b_unplugged = -40;
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

    public void calibrate(double zero) {
        noise_b_plugged = -zero;
        noise_b_unplugged = -zero;
    }

    @Override
    public String toString() {
        return "NoiseAdjuster{" +
                "noise_a_plugged=" + noise_a_plugged +
                ", noise_b_plugged=" + noise_b_plugged +
                ", noise_a_unplugged=" + noise_a_unplugged +
                ", noise_b_unplugged=" + noise_b_unplugged +
                '}';
    }
}


public class VolumeUpdater extends Service implements Serializable {
    transient public static VolumeUpdater mVolumeUpdater;
    private HashMap<ContextInfo, Integer> mMap;
    private NoiseAdjuster mNoiseAdjuster;
    private double lambda_p = 0.4;
    private double lambda_n = 0.8;
    private double mu = 0.2;
    transient private boolean service_status = false; //是否开启服务
    private boolean noise_calibrate_done = false; //是否进行了噪音矫正

    private static String getTime() {
        return (new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss", Locale.US)).format(new Date());
    }

    private void writeLog(Context context, String log) {
        try {
            File file = new File(context.getExternalFilesDir(null), "VolumeUpdater.log");
            FileWriter writer = new FileWriter(file, true);
            writer.write(getTime() + log + "\n");
            writer.flush();
            writer.close();
//            FileOutputStream fos = context.openFileOutput("VolumeUpdater.log", Context.MODE_APPEND);
//            ObjectOutputStream os = new ObjectOutputStream(fos);
//            os.writeBytes(getTime() + log + "\n");
//            fos.write((getTime() + log + "\n").getBytes(StandardCharsets.UTF_8));
//            os.close();
//            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeConfig(Context context) {
        Log.d("VU", context.getFilesDir().toString());
        try {
//            config.createNewFile();
            FileOutputStream fos = context.openFileOutput("VolumeUpdater-v2.dat", Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(this);
            os.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
//        Gson gson = new Gson();
//        String j = gson.toJson(this);
//        JSONObject json = new JSONObject();
//        try {
//            json.put("service_status", this.service_status);
//            json.put("noise_calibrate_done", this.noise_calibrate_done);
//            json.put("mMap", new JSONObject(this.mMap));
//            json.put("mNoiseAdjuster", this.mNoiseAdjuster);
////            FileOutputStream fout = openFileOutput(config, Context.MODE_PRIVATE);
////            fout.write(json.toString().getBytes());
////            fout.flush();
////            fout.close();
//            BufferedWriter writer = new BufferedWriter(new FileWriter(config));
//            writer.write(json.toString());
//            writer.flush();
//            writer.close();
//            Log.d("VU", "config file written");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    private void readConfig(Context context) {
        try {
            FileInputStream fis = context.openFileInput("VolumeUpdater-v2.dat");
            ObjectInputStream is = new ObjectInputStream(fis);
            VolumeUpdater saved = (VolumeUpdater) is.readObject();
            this.mMap = saved.mMap;
            this.mNoiseAdjuster = saved.mNoiseAdjuster;
//            this.lambda = saved.lambda;
//            this.mu = saved.mu;
            this.noise_calibrate_done = saved.noise_calibrate_done;
            is.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
        int current_volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (!service_status) {
            this.writeLog(context, String.format(Locale.getDefault(),
                    "[feedback]service=0,gps=%d,app=%d,plugged=%b,noise=0,qa_result=0,vol=%d,hit=0,target=0,delta=0",
                    gps, app, plugged, current_volume));
            return;
        }
        this.readConfig(context);
        // single entry
        Integer old_target = this.mMap.get(new ContextInfo(gps, app, plugged));
        int current_target = current_volume - this.mNoiseAdjuster.adjust(noise, plugged);
        if (old_target != null) {
            int new_target;
            if (old_target < current_target) {
                new_target = (int) Math.round(current_target * lambda_p + old_target * (1 - lambda_p));
            } else {
                new_target = (int) Math.round(current_target * lambda_n + old_target * (1 - lambda_n));
            }
            this.mMap.replace(new ContextInfo(gps, app, plugged), new_target);
            this.writeConfig(context);
            Log.d("VU", String.format("change context <gps=%d, app=%d, plugged=%b, noise=%.2f> from <%d> to <%d>",
                    gps, app, plugged, noise, old_target, new_target));
            this.writeLog(context, String.format(Locale.getDefault(),
                    "[feedback]service=1,gps=%d,app=%d,plugged=%b,noise=%.2f,qa_result=%d,vol=%d,hit=1,target=%d,new_target=%d",
                    gps, app, plugged, noise, qa_result, current_volume, old_target, new_target));
        } else {  // regard as not-hit context and we don't want to insert it
            Log.d("VU", "feedback target miss");
            this.writeLog(context, String.format(Locale.getDefault(),
                    "[feedback]service=1,gps=%d,app=%d,plugged=%b,noise=%.2f,qa_result=%d,vol=%d,hit=0,target=0,delta=0",
                    gps, app, plugged, noise, qa_result, current_volume));
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
        this.readConfig(context);
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int current_volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (!service_status) {
            this.writeLog(context, String.format(Locale.getDefault(),
                    "[update]service=0,gps=%d,app=%d,plugged=%b,noise=%.2f,old_vol=%d,hit=0,delta=0;",
                    gps, app, plugged, noise, current_volume));
            return;
        }
        Integer target = this.mMap.get(new ContextInfo(gps, app, plugged));
        if (target == null) {  // does not contain current context
            this.mMap.put(new ContextInfo(gps, app, plugged), current_volume - this.mNoiseAdjuster.adjust(noise, plugged));
            this.writeConfig(context);
            Log.d("VU", String.format("add new context <gps=%d, app=%d, plugged=%b> (noise=%.2f), now contains %d entries",
                    gps, app, plugged, noise, this.mMap.size()));
            this.writeLog(context, String.format(Locale.getDefault(),
                    "[update]service=1,gps=%d,app=%d,plugged=%b,noise=%.2f,old_vol=%d,hit=0,delta=0;",
                    gps, app, plugged, noise, current_volume));

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
            this.writeLog(context, String.format(Locale.getDefault(),
                    "[update]service=1,gps=%d,app=%d,plugged=%b,noise=%.2f,old_vol=%d,hit=1,delta=%d;",
                    gps, app, plugged, noise, current_volume, target - current_volume));
        }
    }

    public void setStatus(boolean newStatus) {
        Log.d("VU", String.format("Set status to <%b>", newStatus));
        this.service_status = newStatus;
    }

    public boolean getStatus() {
        return this.service_status;
    }

    public boolean getNoiseCalibrateDone(Context context) {
        this.readConfig(context);
        return this.noise_calibrate_done;
    }

    public void setNoiseCalibrate(double zero) {
        Log.d("VU", String.format("cal noise to %.2f", zero));
        this.noise_calibrate_done = true;
        this.mNoiseAdjuster.calibrate(zero);
    }

    @Override
    public String toString() {
        return "VolumeUpdater{" +
                "mMap=" + mMap +
                ", mNoiseAdjuster=" + mNoiseAdjuster +
//                ", lambda=" + lambda +
//                ", mu=" + mu +
                ", noise_calibrate_done=" + noise_calibrate_done +
                '}';
    }
}
