package com.ivolume.ivolume;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

//import com.ivolume.ivolume.VolumeUpdater;

@RequiresApi(api = Build.VERSION_CODES.R)
public class MainService extends AccessibilityService {

    static final public String ACTION_RECORD_MSG = "com.ivolume.ivolume.mainservice.record_msg";
    static final public String EXTRA_MSG = "com.ivolume.ivolume.mainservice.msg";
    static final public String CONTEXT_LOG_TAG = "mainservice.getcontext.log";
    static final public String LOCATION_LOG_TAG = "mainservice.getlocation.log";
    static final public String BLUETOOTH_LOG_TAG = "mainservice.getbluetooth.log";

    private final AtomicInteger mLogID = new AtomicInteger(0);
    private final IntUnaryOperator operator = x -> (x < 999) ? (x + 1) : 0;

    // TODO add gps map table, app map table

    // listening
    final Uri[] listenedURIs = {
            Settings.System.CONTENT_URI,
            Settings.Global.CONTENT_URI,
    };
    final String[] listenedActions = {
            Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED,
            Intent.ACTION_HEADSET_PLUG,
            Intent.ACTION_USER_BACKGROUND,
            Intent.ACTION_USER_FOREGROUND,
            // Bluetooth related
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            BluetoothDevice.ACTION_ALIAS_CHANGED,
            BluetoothDevice.ACTION_BOND_STATE_CHANGED,
            BluetoothDevice.ACTION_NAME_CHANGED,
    };

    // recording related
    String filename = "log.tsv";
    FileWriter writer;
    int brightness;
    public static int gps;
    public static boolean plugged;
    public static double noise;
    private int curr_volume;
    static final HashMap<String, Integer> volume = new HashMap<>();

    static {
        // speaker
        volume.put("volume_music_speaker", 0);
        volume.put("volume_ring_speaker", 0);
        volume.put("volume_alarm_speaker", 0);
        volume.put("volume_voice_speaker", 0);
        volume.put("volume_tts_speaker", 0);
        // headset
        volume.put("volume_music_headset", 0);
        volume.put("volume_voice_headset", 0);
        volume.put("volume_tts_headset", 0);
        // headphone
        volume.put("volume_music_headphone", 0);
        volume.put("volume_voice_headphone", 0);
        volume.put("volume_tts_headphone", 0);
        // Bluetooth A2DP
        volume.put("volume_music_bt_a2dp", 0);
        volume.put("volume_voice_bt_a2dp", 0);
        volume.put("volume_tts_bt_a2dp", 0);
    }

    String packageName = "";

    Context context;
    LocalBroadcastManager localBroadcastManager;

    //监测APP
    public static String CurrentPackage; //当前app
    public static Map<String, Integer> AppPackageMap = new HashMap<String, Integer>() {{
        put("com.tencent.wemeet.app", 0); //腾讯会议
//        put("com.tencent.mm", 1);  //微信
        put("tv.danmaku.bili", 2);  //b站
        put("com.netease.cloudmusic", 3);  //网易云
        put("cn.ledongli.ldl", 4);  //乐动力
    }};

    //这1s内是否进行了音量调节
    public static boolean volume_key_pressed = false;
    public static boolean volume_change_pending = false;


    // TODO judge whether context changed, if so:
    // 1. call all four context getter
    // 2. call volume updater

    void jsonSilentPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    public void createNotification(String title, String content) {
        Intent intent = new Intent(this, Questionnaire_Activity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
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

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            JSONObject json = new JSONObject();
            String action = intent.getAction();

            // get extra paras into JSON string
            Bundle extras = intent.getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    Object obj = JSONObject.wrap(extras.get(key));
                    if (obj == null) {
                        obj = JSONObject.wrap(extras.get(key).toString());
                    }
                    jsonSilentPut(json, key, obj);
                }
            }

            // record additional information for some special actions
            switch (action) {
                case Intent.ACTION_CONFIGURATION_CHANGED:
                    Configuration config = getResources().getConfiguration();
                    jsonSilentPut(json, "configuration", config.toString());
                    jsonSilentPut(json, "orientation", config.orientation);
                    break;
                case Intent.ACTION_SCREEN_OFF:
                case Intent.ACTION_SCREEN_ON:
                    // ref: https://stackoverflow.com/a/17348755/11854304
                    DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
                    if (dm != null) {
                        Display[] displays = dm.getDisplays();
                        int[] states = new int[displays.length];
                        for (int i = 0; i < displays.length; i++) {
                            states[i] = displays[i].getState();
                        }
                        jsonSilentPut(json, "displays", states);
                    }
                    break;
                case Intent.ACTION_HEADSET_PLUG:
                    Log.d(BLUETOOTH_LOG_TAG,"headset plug in");
                    plugged = true;
                    break;
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    Log.d(BLUETOOTH_LOG_TAG,"bluetooth headset plug in");
                    plugged = true;
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    Log.d(BLUETOOTH_LOG_TAG,"bluetooth headset plug out");
                    plugged = false;
                    break;
            }

            doUpdate();
            jsonSilentPut(json, "package", packageName);

            // record data
            record("BroadcastReceive", action, "", json.toString());
        }
    };

    // ref: https://stackoverflow.com/a/67355428/11854304
    ContentObserver contentObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            JSONObject json = new JSONObject();
            String key;
            int value = 0;
            String tag = "";

            if (uri == null) {
                key = "uri_null";
            } else {
                key = uri.toString();
                String database_key = uri.getLastPathSegment();
                String inter = uri.getPathSegments().get(0);
                if ("system".equals(inter)) {
                    value = Settings.System.getInt(getContentResolver(), database_key, value);
                    tag = Settings.System.getString(getContentResolver(), database_key);
                } else if ("global".equals(inter)) {
                    value = Settings.Global.getInt(getContentResolver(), database_key, value);
                    tag = Settings.Global.getString(getContentResolver(), database_key);
                }

                // record special information
                if (Settings.System.SCREEN_BRIGHTNESS.equals(database_key)) {
                    // record brightness value difference and update
                    int diff = value - brightness;
                    jsonSilentPut(json, "diff", diff);
                    brightness = value;
                    // record brightness mode
                    int mode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, -1);
                    if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) {
                        jsonSilentPut(json, "mode", "man");
                    } else if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                        jsonSilentPut(json, "mode", "auto");
                    } else {
                        jsonSilentPut(json, "mode", "unknown");
                    }
                }
                if (database_key.startsWith("volume_")) {
                    if (!volume.containsKey(database_key)) {
                        // record new volume value
                        volume.put(database_key, value);
                    }
                    // record volume value difference and update
                    int diff = value - volume.put(database_key, value);
                    jsonSilentPut(json, "diff", diff);
                }
            }

            jsonSilentPut(json, "package", packageName);

            // record data
//            record("ContentChange", key, tag, json.toString());

//            volumeUpdater.update();
        }
    };

    public MainService() {
    }

    //location info
    protected LocationManager locationManager;
    protected Location mlocation;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        CurrentPackage = "";
        Log.d("TEST","step1");
        initialize();

        //检测位置变化
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //首先判断网络是否可用
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } else {
            //将手机位置服务中--基于网络的位置服务关闭后，则获取不到数据
            showgps("NETWORK_PROVIDER不可用，无法获取GPS信息！");
        }
        //获取当前设备说有的Provider
        List<String> allprovides = locationManager.getAllProviders();
        for (String allprovide : allprovides) {
            Log.d("Test", allprovide);
        }
        Log.d("TEST","step2");
        //检测设备
        checkConnectState();

        //合并音量调节事件
        Thread key_merge_thread = new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.S)
            @Override
            public void run() {
                // 合并音量调节按键事件
                while(true){
                    while(volume_key_pressed){
                        volume_key_pressed = false;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if(volume_change_pending){
                        //完成一次音量调节事件，弹出通知
                        NoiseDetector noiseDetector = new NoiseDetector();
                        noise = noiseDetector.getNoise();
                        volume_change_pending = false;
                        createNotification("检测到您的音量调节", "为了更好地为您服务，邀请您填写反馈问卷！");
                    }
                }
            }
        });
        key_merge_thread.start();
    }

    private int gps2place(double Latitude, double Longitude) {
        // TODO
        // 操场
        // 六教
        // 李文正馆
        // 其他
        return 0;
    }

    protected final LocationListener locationListener = new LocationListener() {

        // Provider的在可用、暂时不可用和无服务三个状态直接切换时触发此函数
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            showgps("status" + status);
        }

        //  Provider被enable时触发此函数，比如GPS被打开
        @Override
        public void onProviderEnabled(String provider) {
            showgps("enabled");
        }

        // Provider被disable时触发此函数，比如GPS被关闭
        @Override
        public void onProviderDisabled(String provider) {
            showgps("disabled");
        }

        //当坐标改变时触发此函数
        @Override
        public void onLocationChanged(Location location) {
            mlocation = location;
            //解除监听
            locationManager.removeUpdates(locationListener);
            double lat = mlocation.getLatitude(), longti = mlocation.getLongitude();
            String gpsinfo = "GPS: Latitude=" + lat + "   Longitude=" + longti + "   place:" + gps2place(lat, longti);
            showgps(gpsinfo);
            gps = gps2place(lat, longti);
            doUpdate();
        }
    };

    private void doUpdate() {
        NoiseDetector noiseDetector = new NoiseDetector();
        VolumeUpdater.getInstance().update(this, gps, getApp(), plugged, noiseDetector.getNoise());
    }

    private void checkConnectState() {
        ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Class<BluetoothAdapter> bluetoothAdapterClass = BluetoothAdapter.class;//得到BluetoothAdapter的Class对象
        try {
            //得到连接状态的方法
            Method method = bluetoothAdapterClass.getDeclaredMethod("getConnectionState", (Class[]) null);
            //打开权限
            method.setAccessible(true);
            int state = (Integer) method.invoke(adapter, (Object[]) null);
            if (state == BluetoothAdapter.STATE_CONNECTED) {
                Log.d("BLUETOOTH", "BluetoothAdapter.STATE_CONNECTED");
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                Set<BluetoothDevice> devices = adapter.getBondedDevices();
                Log.d("BLUETOOTH", "devices:" + devices.size());

                for (BluetoothDevice device : devices) {
                    Method isConnectedMethod = BluetoothDevice.class.getDeclaredMethod("isConnected", (Class[]) null);
                    method.setAccessible(true);
                    boolean isConnected = (Boolean) isConnectedMethod.invoke(device, (Object[]) null);
                    if (isConnected) {
                        Log.d("BLUETOOTH", "connected:" + device.getName());
                        deviceList.add(device);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void showgps(String info){
        Log.d(LOCATION_LOG_TAG, "location" + info);
        broadcast("location" + info);
    }

    @Override
    public void onDestroy() {
        terminate();
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence pkg = event.getPackageName();
        if (pkg != null) {
            packageName = event.getPackageName().toString();
        }

        int type=event.getEventType();
        //监测app变化
        if(type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
            String tmpPackage = event.getPackageName()==null? "": event.getPackageName().toString();
            if(!tmpPackage.equals(CurrentPackage)){
                CurrentPackage = tmpPackage;
                //当前app包名改变时
                //只针对AppPackageMap的5个app进行处理，忽略其他包
                if(AppPackageMap.containsKey(tmpPackage)) {
                    int cur_index = AppPackageMap.get(CurrentPackage);
                    Log.d("app_log_tag", "CurrentPackage changed, name:" + CurrentPackage
                    + ", index:" + cur_index);
                    doUpdate();
                }
            }
        }


    }

    @Override
    public void onInterrupt() {

    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.S)
    protected boolean onKeyEvent(KeyEvent event) {
        //未开启服务，直接返回
        if(!VolumeUpdater.getInstance().getStatus())
            return super.onKeyEvent(event);
        //24-volume_up 25-volume_down
        if(event.getKeyCode() == 24 || event.getKeyCode() == 25){
            Log.d("volume key", "volume up 1");
            volume_key_pressed = true;
            volume_change_pending = true;
        }
        JSONObject json = new JSONObject();
        jsonSilentPut(json, "code", event.getKeyCode());
        jsonSilentPut(json, "action", event.getAction());
        jsonSilentPut(json, "source", event.getSource());
        jsonSilentPut(json, "eventTime", event.getEventTime());
        jsonSilentPut(json, "downTime", event.getDownTime());
        jsonSilentPut(json, "package", packageName);
        jsonSilentPut(json, "keycodeString", KeyEvent.keyCodeToString(event.getKeyCode()));

//        createNotification("KeyEvent", String.valueOf(event.getAction()));
        record("KeyEvent", "KeyEvent://" + event.getAction() + "/" + event.getKeyCode(), "", json.toString());
        return super.onKeyEvent(event);
    }

    void initialize() {
        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        for (String action : listenedActions) {
            filter.addAction(action);
        }
        registerReceiver(broadcastReceiver, filter);

        // register content observer
        for (Uri uri : listenedURIs) {
            getContentResolver().registerContentObserver(uri, true, contentObserver);
        }

        // recording related
        try {
            File file;
            if (isExternalStorageWritable()) {
                file = new File(getExternalFilesDir(null), filename);
            } else {
                file = new File(getFilesDir(), filename);
            }
            // append to file
            writer = new FileWriter(file, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // record all current values
        record_all();
    }

    void terminate() {
        // unregister broadcast receiver
        unregisterReceiver(broadcastReceiver);
        // unregister content observer
        getContentResolver().unregisterContentObserver(contentObserver);

        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Checks if a volume containing external storage is available
    // for read and write.
    private boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    void record_all() {
        JSONObject json = new JSONObject();

        // store brightness
        brightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
        jsonSilentPut(json, "brightness", brightness);

        // store volumes
        volume.replaceAll((k, v) -> Settings.System.getInt(getContentResolver(), k, 0));
        volume.forEach((k, v) -> jsonSilentPut(json, k, v));

        // store configuration and orientation
        Configuration config = getResources().getConfiguration();
        jsonSilentPut(json, "configuration", config.toString());
        jsonSilentPut(json, "orientation", config.orientation);

        // store system settings
        jsonPutSettings(json, "system", Settings.System.class);

        // store global settings
        jsonPutSettings(json, "global", Settings.Global.class);

        // store secure settings
        jsonPutSettings(json, "secure", Settings.Secure.class);

        // record
        record("static", "start", "", json.toString());
    }

    void jsonPutSettings(JSONObject json, String key, Class<?> c) {
        JSONArray jsonArray = new JSONArray();
        Field[] fields_glb = c.getFields();
        for (Field f : fields_glb) {
            if (Modifier.isStatic(f.getModifiers())) {
                try {
                    String name = f.getName();
                    Object obj = f.get(null);
                    if (obj != null) {
                        String database_key = obj.toString();
                        Method method = c.getMethod("getString", ContentResolver.class, String.class);
                        String value_s = (String) method.invoke(null, getContentResolver(), database_key);
                        jsonArray.put(new JSONArray().put(name).put(database_key).put(value_s));
                    }
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        jsonSilentPut(json, key, jsonArray);
    }

    private int incLogID() {
        return mLogID.getAndUpdate(operator);
    }

    // record data to memory and file
    public void record(String type, String action, String tag, String other) {
        long cur_timestamp = System.currentTimeMillis();
        // record to memory
        String[] paras = {Long.toString(cur_timestamp), Integer.toString(incLogID()), type, action, tag, other};
        String line = String.join("\t", paras);
        // record to file
        if (writer != null) {
            try {
                writer.write(line + "\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // broadcast to update UI
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA);
        String cur_datetime = format.format(new Date(cur_timestamp));
        paras[0] = " -------- " + cur_datetime + " -------- ";
        broadcast(String.join("\n", paras));
    }

    // send broadcast to notify
    private void broadcast(String msg) {
        Intent intent = new Intent(ACTION_RECORD_MSG);
        if (msg != null)
            intent.putExtra(EXTRA_MSG, msg);
        localBroadcastManager.sendBroadcast(intent);
    }

    ///<summary>获得当前app信息
    ///返回0-4
    // 如果当前app不在5个的范围内，返回5
    public static Integer getApp() {
        if (AppPackageMap.containsKey(CurrentPackage))
            return AppPackageMap.get(CurrentPackage);
        return 5;
    }
}
