package com.ivolume.ivolume;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

@RequiresApi(api = Build.VERSION_CODES.S)
public class Questionnaire_Activity extends AppCompatActivity {
    private static final String LOG_TAG = Questionnaire_Activity.class.getSimpleName();
    public static final String Questionnaire_Answer =
            "Questionnaire_Activity.extra.Questionnaire_Answer";
    private int answer = -1; //    问卷选择结果

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);
    }

    ///点击提交按钮时
    public void launchSecondActivity(View view) {
        //调节音量设置
        int gps = MainService.gps;
        int app_index = MainService.getApp();
        boolean plugged = MainService.plugged;
        double noise = 0;
        //todo get noise
        VolumeUpdater.getInstance().feedback(this, gps, app_index, plugged, noise, answer);

        //切换到MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        Log.d(LOG_TAG, "Button clicked! Jump to MainActivity");
        startActivity(intent);
    }

    public void click_answer_1(View view) {
        answer = 1;
    }

    public void click_answer_2(View view) {
        answer = 2;
    }

    public void click_answer_3(View view) {
        answer = 3;
    }

    public void click_answer_4(View view) {
        answer = 4;
    }

    public void click_answer_5(View view) {
        answer = -1;
    }

}
