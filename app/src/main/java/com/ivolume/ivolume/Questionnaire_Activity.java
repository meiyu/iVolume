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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);
    }


    public void launchSecondActivity(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        Log.d(LOG_TAG, "Button clicked! Jump to MainActivity");
        startActivity(intent);
    }
}
