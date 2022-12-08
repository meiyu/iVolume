package com.ivolume.ivolume;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;

@SuppressLint("UseSwitchCompatOrMaterialCode")
public class SettingsActivity extends AppCompatActivity {
    Switch appButton1;
    Switch appButton2;
    Switch appButton3;
    Switch appButton4;

    Switch locationButton1;
    Switch locationButton2;
    Switch locationButton3;
    Switch locationButton4;

    Switch deviceButton1;
    Switch deviceButton2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        appButton1 = findViewById(R.id.appButton1);
        appButton2 = findViewById(R.id.appButton2);
        appButton3 = findViewById(R.id.appButton3);
        appButton4 = findViewById(R.id.appButton4);
        locationButton1 = findViewById(R.id.locationButton1);
        locationButton2 = findViewById(R.id.locationButton2);
        locationButton3 = findViewById(R.id.locationButton3);
        locationButton4 = findViewById(R.id.locationButton4);
        deviceButton1 = findViewById(R.id.deviceButton1);
        deviceButton2 = findViewById(R.id.deviceButton2);
        //加载上次的设置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            appButton1.setChecked(MainService.appEnable[0]);
            appButton2.setChecked(MainService.appEnable[1]);
            appButton3.setChecked(MainService.appEnable[2]);
            appButton4.setChecked(MainService.appEnable[3]);

            locationButton1.setChecked(MainService.locationEnable[0]);
            locationButton2.setChecked(MainService.locationEnable[1]);
            locationButton3.setChecked(MainService.locationEnable[2]);
            locationButton4.setChecked(MainService.locationEnable[3]);

            deviceButton1.setChecked(MainService.deviceEnable[0]);
            deviceButton2.setChecked(MainService.deviceEnable[1]);
        }
    }

    public void appButton1Click(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MainService.appEnable[0] = !(MainService.appEnable[0]);
        }
    }

    public void appButton2Click(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MainService.appEnable[1] = !(MainService.appEnable[1]);
        }
    }

    public void appButton3Click(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MainService.appEnable[2] = !(MainService.appEnable[2]);
        }
    }

    public void appButton4Click(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MainService.appEnable[3] = !(MainService.appEnable[3]);
        }
    }

    public void locationButton1Click(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MainService.locationEnable[0] = !(MainService.locationEnable[0]);
        }
    }

    public void locationButton2Click(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MainService.locationEnable[1] = !(MainService.locationEnable[1]);
        }
    }

    public void locationButton3Click(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MainService.locationEnable[2] = !(MainService.locationEnable[2]);
        }
    }

    public void locationButton4Click(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MainService.locationEnable[3] = !(MainService.locationEnable[3]);
        }
    }

    public void deviceButton1Click(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MainService.deviceEnable[0] = !(MainService.deviceEnable[0]);
        }
    }

    public void deviceButton2Click(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MainService.deviceEnable[1] = !(MainService.deviceEnable[1]);
        }
    }
}