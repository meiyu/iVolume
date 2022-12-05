package com.ivolume.ivolume;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;



@RequiresApi(api = Build.VERSION_CODES.S)
public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
    };

    Context context;
    LocalBroadcastManager localBroadcastManager;

//    SeekBar lightBar;
//    TextView textView;
//    TextView logTextView;
    ImageButton noise_button;
    int noise_button_status = 0; //0-off 1-on

    ImageButton service_status_button;
    int service_status = 0; //0-off 1-on
    TextView service_status_text;
    String service_status_text1 = "当前状态：暂停服务";
    String service_status_text2 = "当前状态：正在服务";
    TextView service_status_info_text;
    String service_status_info_text1 = "点击上部按钮开始服务~";
    String service_status_info_text2 = "点击上部按钮暂停服务~";


    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MainService.ACTION_RECORD_MSG.equals(action)) {
                String msg = intent.getStringExtra(MainService.EXTRA_MSG);
                Log.i("broadReceiver", msg);
                //addMessage(logTextView, msg);
            }
        }
    };

    public void createNotification(String title, String content) {
        Intent intent = new Intent(this, Questionnaire_Activity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "问卷通知")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        createNotificationChannel();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            String description = getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("问卷通知", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Append a string to a TextView as a new line
    // 1. erase excessive lines
    // 2. scroll to the bottom if needed
    public void addMessage(TextView mTextView, String msg) {
//        // append the new string
//        mTextView.append("\n" + msg);
//
//        // Erase excessive lines
//        // ref: https://stackoverflow.com/a/10312621/11854304
//        final int MAX_LINES = 1000;
//        int excessLineNumber = mTextView.getLineCount() - MAX_LINES;
//        if (excessLineNumber > 0) {
//            int eolIndex = -1;
//            CharSequence charSequence = mTextView.getText();
//            for (int i = 0; i < excessLineNumber; i++) {
//                do {
//                    eolIndex++;
//                } while (eolIndex < charSequence.length() && charSequence.charAt(eolIndex) != '\n');
//            }
//            if (eolIndex < charSequence.length()) {
//                mTextView.getEditableText().delete(0, eolIndex + 1);
//            } else {
//                mTextView.setText("");
//            }
//        }
//
//        // find the amount we need to scroll.  This works by
//        // asking the TextView's internal layout for the position
//        // of the final line and then subtracting the TextView's height
//        // ref: https://stackoverflow.com/a/7350267/11854304
//        Layout layout = mTextView.getLayout();
//        if (layout == null)
//            return;
//        final int scrollAmount = layout.getLineTop(mTextView.getLineCount()) - mTextView.getHeight();
//        // if there is no need to scroll, scrollAmount will be <=0
//        if (scrollAmount > 0)
//            mTextView.scrollTo(0, scrollAmount);
    }

    public void clickStartService(View view) {
        ComponentName ret = startService(new Intent(this, MainService.class));
//        if (ret != null) {
//            addMessage(logTextView, "SERVICE started!");
//        } else {
//            addMessage(logTextView, "SERVICE failed to start!!!");
//        }
    }

    public void clickStopService(View view) {
        boolean ret = stopService(new Intent(this, MainService.class));
//        if (ret) {
//            addMessage(logTextView, "SERVICE stopped!");
//        } else {
//            addMessage(logTextView, "SERVICE already stopped!");
//        }
    }

    void initialize() {
        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(MainService.ACTION_RECORD_MSG);
        localBroadcastManager.registerReceiver(broadcastReceiver, filter);

        // start service
        clickStartService(null);
    }

    void terminate() {
        // unregister broadcast receiver
        localBroadcastManager.unregisterReceiver(broadcastReceiver);
    }

    // ref: https://stackoverflow.com/a/14923144/11854304
    public boolean isAccessibilityServiceEnabled(Class<? extends AccessibilityService> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
            if (enabledServiceInfo.packageName.equals(context.getPackageName()) && enabledServiceInfo.name.equals(service.getName()))
                return true;
        }

        return false;
    }

    void checkPermissions() {
        try {
            boolean request = false;
            for (String per : PERMISSIONS) {
                int permission = checkSelfPermission(per);
                Log.e(per, Boolean.toString(permission == PackageManager.PERMISSION_GRANTED));
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    request = true;
                }
            }
            if (request)
                requestPermissions(PERMISSIONS, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        //ui查找与设置
        noise_button = findViewById(R.id.noise_button);
        noise_button.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.noise_button_1,null));

        service_status_button = findViewById(R.id.service_status_button);
        service_status_text = findViewById(R.id.service_status_text);
        service_status_info_text = findViewById(R.id.service_status_info_text);


        //恢复上次的服务状态
        if(VolumeUpdater.getInstance().getStatus()){
            service_status_button.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.on_button,null));
            service_status_text.setText(service_status_text2);
            service_status_info_text.setText(service_status_info_text2);
        }
        else{
            service_status_button.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.off_button,null));
            service_status_text.setText(service_status_text1);
            service_status_info_text.setText(service_status_info_text1);
        }

//        lightBar = findViewById(R.id.seekBar);
//        textView = findViewById(R.id.textView);
//        logTextView = findViewById(R.id.textView_contentObserver);

        // set scrollable
//        logTextView.setMovementMethod(new ScrollingMovementMethod());
//        logTextView.setScrollbarFadingEnabled(false);

        // save text when frozen
        // ref: https://stackoverflow.com/a/31541484/11854304
//        logTextView.setFreezesText(true);

//        int progress = Math.round((float)brightness*100/256);
//        lightBar.setProgress(progress);
//        textView.setText("进度值：" + progress + "  / 100 \n亮度值：" + brightness);

        // Listen to SeekBar changes
//        lightBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @SuppressLint("SetTextI18n")
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                int brightness = Math.round((float) progress * 256 / 100);
//                textView.setText("进度值：" + progress + "  / 100 \n亮度值：" + brightness);
//                if (Settings.System.canWrite(context)) {
//                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
//                }
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//                // require WRITE SETTINGS permission
//                if (!Settings.System.canWrite(context)) {
//                    Toast.makeText(context, "Cannot write to system settings", Toast.LENGTH_SHORT).show();
//                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
//                    intent.setData(Uri.parse("package:" + context.getPackageName()));
//                    startActivity(intent);
//                }
//                if (Settings.System.canWrite(context) && Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0) != Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) {
//                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
//                }
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//            }
//        });

        checkPermissions();

        // jump to accessibility settings
        if (!isAccessibilityServiceEnabled(MainService.class)) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        }

        // initialization
        initialize();
    }

    @Override
    protected void onDestroy() {
        terminate();
        super.onDestroy();
    }

    public void onNoiseButtonClick(View view){
        Log.d("onNoiseButtonClick",Integer.toString(noise_button_status));
        if(noise_button_status == 0){
            //开始进入检测状态
            noise_button_status = 1;
            noise_button.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.noise_button_2,null));
            noise_button.invalidate();
//            noise_button.invalidateDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.noise_button_2,null));
            //todo 噪音矫正
            Log.d("onNoiseButtonClick","setImageDrawable");
            new Thread () {
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    noise_button_status = 0;
                    noise_button.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.noise_button_1,null));
                }
            }.start();
//            NoiseDetector noiseDetector = new NoiseDetector();
//            noiseDetector.getNoise();

            //结束噪音检测

//            noise_button.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.noise_button_1,null));
        }
    }

    public void onServiceStatusButtonClick(View view) {
        if(service_status == 0){
            //重新开始服务
            service_status = 1;
            //todo 改图片
            service_status_button.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.on_button,null));
            service_status_text.setText(service_status_text2);
            service_status_info_text.setText(service_status_info_text2);
            //启动服务
            VolumeUpdater.getInstance().changeStatus(true);

        }
        else{
            //暂停服务
            service_status = 0;
            //todo 改图片
            service_status_button.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.off_button,null));
            service_status_text.setText(service_status_text1);
            service_status_info_text.setText(service_status_info_text1);
            //暂停服务
            VolumeUpdater.getInstance().changeStatus(false);
        }
    }
}