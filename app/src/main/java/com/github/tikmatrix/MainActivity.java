package com.github.tikmatrix;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ImageView;

import com.github.tikmatrix.util.MemoryManager;
import com.github.tikmatrix.util.OkhttpManager;
import com.github.tikmatrix.util.Permissons4App;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

@SuppressLint({ "SetTextI18n", "UnspecifiedRegisterReceiverFlag" })
public class MainActivity extends AppCompatActivity {
    public static final String TAG = "TikMatrix";
    private TextView tvInStorage;
    private TextView textViewIP;
    private SwitchCompat switchNotification;
    private SwitchCompat switchFloatingWindow;
    private TextView tvWanIp;
    private TextView tvRunningStatus;
    private final OkhttpManager okhttpManager = OkhttpManager.getSingleton();
    private boolean isStubRunning = false;
    public static final String STUB_STATUS_ACTION = "com.github.tikmatrix.stub.STUB_RUNNING";
    private BroadcastReceiver mStubStatusReceiver;
    private TextView tvCurrentTime;
    private final Handler timeUpdateHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateCurrentTime();
            timeUpdateHandler.postDelayed(this, 1000); // 每秒更新一次
        }
    };
    private ImageView statusIcon;
    private TextView cpuInfoTextView;
    private TextView memoryInfoTextView;
    private TextView screenInfoTextView;
    private TextView batteryInfoTextView;
    private TextView networkTypeTextView;
    private TextView macAddressTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            setTitle("TikMatrix v" + versionName);
        } catch (Exception e) {
            setTitle("TikMatrix");
        }

        ((TextView) findViewById(R.id.product_name)).setText(Build.MANUFACTURER + " " + Build.MODEL);
        ((TextView) findViewById(R.id.android_system_version))
                .setText("Android " + Build.VERSION.RELEASE + " SDK " + Build.VERSION.SDK_INT);
        ((TextView) findViewById(R.id.language))
                .setText(Locale.getDefault().getCountry() + "-" + Locale.getDefault().getLanguage());
        ((TextView) findViewById(R.id.timezone)).setText(TimeZone.getDefault().getDisplayName());
        switchNotification = findViewById(R.id.notification_permission);
        switchFloatingWindow = findViewById(R.id.floating_window_permission);
        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestNotificationPermission();
            } else {
                NotificationManager notificationManager = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    notificationManager = getSystemService(NotificationManager.class);
                }
                if (notificationManager != null) {
                    notificationManager.cancelAll();
                }
            }
        });
        switchFloatingWindow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestFloatingWindowPermission();
            }
        });
        Intent intent = getIntent();
        String action = intent.getAction();
        Log.i(TAG, "action: " + action);
        boolean isHide = intent.getBooleanExtra("hide", false);
        if (isHide) {
            Log.i(TAG, "launch args hide:true, move to background");
            moveTaskToBack(true);
        }
        textViewIP = findViewById(R.id.ip_address);

        tvInStorage = findViewById(R.id.in_storage);
        tvWanIp = findViewById(R.id.wan_ip_address);
        tvRunningStatus = findViewById(R.id.running_status);
        statusIcon = findViewById(R.id.status_icon);
        String[] permissions = new String[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[] {
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
        } else {
            permissions = new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
        Permissons4App.initPermissions(this, permissions);
        mStubStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (STUB_STATUS_ACTION.equals(intent.getAction())) {
                    // Stub 正在运行
                    Log.i(TAG, "stub running");
                    if (isStubRunning) {
                        return;
                    }
                    tvRunningStatus.setText(R.string.status_success);
                    tvRunningStatus.setTextColor(Color.GREEN);
                    statusIcon.setImageResource(R.drawable.ic_status_active);
                    isStubRunning = true;
                }
            }
        };
        IntentFilter filter = new IntentFilter(STUB_STATUS_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.i(TAG, "registerReceiver >= 8");
            registerReceiver(mStubStatusReceiver, filter, null, null, Context.RECEIVER_EXPORTED);
        } else {
            Log.i(TAG, "registerReceiver < 8");
            registerReceiver(mStubStatusReceiver, filter);
        }

        tvCurrentTime = findViewById(R.id.current_time);
        startTimeUpdates();

        // 初始化新增的TextView引用
        cpuInfoTextView = findViewById(R.id.cpu_info);
        memoryInfoTextView = findViewById(R.id.memory_info);
        screenInfoTextView = findViewById(R.id.screen_info);
        batteryInfoTextView = findViewById(R.id.battery_info);
        networkTypeTextView = findViewById(R.id.network_type);
        macAddressTextView = findViewById(R.id.mac_address);

        // 只在启动时获取网络地址
        checkNetworkAddress(null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(TAG, "onNewIntent: " + intent.getAction());
        if ("android.intent.action.SEND_MULTIPLE".equals(intent.getAction())) {
            // get extras
            String extras = intent.getStringExtra(Intent.EXTRA_STREAM);
            Log.i(TAG, "extras: " + extras);
            if (extras == null || extras.isEmpty()) {
                return;
            }
            ArrayList<Uri> uris = new ArrayList<Uri>();
            String packagename = "com.zhiliaoapp.musically";
            for (String uriString : extras.split(",")) {
                Log.i(TAG, "uriString: " + uriString);
                if (uriString.startsWith("com.")) {
                    packagename = uriString;
                    continue;
                }
                File file = new File(uriString);
                Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider",
                        file);
                Log.i(TAG, "uri: " + uri);
                uris.add(uri);
            }

            // send to tiktok
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            sendIntent.setType("image/*");
            sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            sendIntent.setPackage(packagename);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(sendIntent);
            Log.d(TAG, "onReceive: sent to " + packagename);
            moveTaskToBack(true);

        }

    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.cancelAll();
            }
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        }
    }

    private void requestFloatingWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

    }

    private boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return NotificationManagerCompat.from(this).areNotificationsEnabled();
        }
        return true;
    }

    private boolean isFloatingWindowPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permissons4App.handleRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void testUiautomator() {
        if (isStubRunning) {
            tvRunningStatus.setText(R.string.agent_running);
            tvRunningStatus.setTextColor(Color.GREEN);
            statusIcon.setImageResource(R.drawable.ic_status_active);
            return;
        }
        boolean isInstalled = Permissons4App.isAppInstalled(MainActivity.this, "com.github.tikmatrix.test");
        if (!isInstalled) {
            tvRunningStatus.setText(R.string.agent_not_installed);
            tvRunningStatus.setTextColor(Color.RED);
            statusIcon.setImageResource(R.drawable.ic_status_inactive);
        } else {
            tvRunningStatus.setText(R.string.agent_not_running);
            tvRunningStatus.setTextColor(Color.RED);
            statusIcon.setImageResource(R.drawable.ic_status_inactive);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();
        switchNotification.setChecked(isNotificationPermissionGranted());
        switchFloatingWindow.setChecked(isFloatingWindowPermissionGranted());
        tvInStorage.setText(Formatter.formatFileSize(this, MemoryManager.getAvailableInternalMemorySize()) + "/"
                + Formatter.formatFileSize(this, MemoryManager.getTotalExternalMemorySize()));
        testUiautomator();
        startTimeUpdates(); // 重新开始时间更新
        
        // 更新新增的设备信息
        updateCpuInfo();
        updateMemoryInfo();
        updateScreenInfo();
        updateBatteryInfo();
        updateNetworkInfo();
        updateMacAddress();
    }

    public String getEthernetIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {

                // 排除回环接口和未活动的接口
                if (!networkInterface.isLoopback() && networkInterface.isUp()) {
                    List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                    for (InetAddress address : addresses) {
                        if (!address.isLoopbackAddress() && address instanceof java.net.Inet4Address) {
                            if (Objects.equals(address.getHostAddress(), "0.0.0.0")) {
                                continue;
                            }
                            return address.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return "0.0.0.0";
    }

    public void checkNetworkAddress(View v) {
        String ipAddress = getEthernetIpAddress();
        textViewIP.setText(ipAddress);
        textViewIP.setTextColor(Color.BLUE);

        Request request = new Request.Builder().url("https://pro.api.tikmatrix.com/front-api/ip")
                .get()
                .build();
        okhttpManager.newCall(request, new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, e.toString());
                runOnUiThread(() -> {
                    tvWanIp.setText("Network Error");
                    tvWanIp.setTextColor(Color.RED);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String content = response.body().string();
                    Log.i(TAG, content);
                    runOnUiThread(() -> {
                        tvWanIp.setText(content);
                        tvWanIp.setTextColor(Color.BLUE);
                    });
                }
            }
        });

    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "unbind service");
        if (mStubStatusReceiver != null) {
            unregisterReceiver(mStubStatusReceiver);
        }
        stopTimeUpdates(); // 确保停止时间更新
    }

    private void updateCurrentTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentTime = dateFormat.format(new Date());
        tvCurrentTime.setText(currentTime);
    }

    private void startTimeUpdates() {
        timeUpdateHandler.post(timeUpdateRunnable);
    }

    private void stopTimeUpdates() {
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimeUpdates(); // 暂停时间更新
    }

    // 获取CPU信息
    private void updateCpuInfo() {
        String cpuInfo = Build.HARDWARE + ", " + Runtime.getRuntime().availableProcessors() + " " + getString(R.string.cores);
        cpuInfoTextView.setText(cpuInfo);
    }

    // 获取内存信息
    private void updateMemoryInfo() {
        ActivityManager actManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        
        long totalMemory = memInfo.totalMem;
        long availMemory = memInfo.availMem;
        
        String memoryStr = Formatter.formatFileSize(this, availMemory) + " / " 
                         + Formatter.formatFileSize(this, totalMemory);
        memoryInfoTextView.setText(memoryStr);
    }

    // 获取屏幕信息
    private void updateScreenInfo() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = (int) metrics.density;
        
        String screenInfo = width + "x" + height + ", " + density + "x";
        screenInfoTextView.setText(screenInfo);
    }

    // 获取电池信息
    private void updateBatteryInfo() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level * 100 / (float) scale;
        
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                             status == BatteryManager.BATTERY_STATUS_FULL;
        
        String batteryInfo = (int) batteryPct + "%" + (isCharging ? " (" + getString(R.string.battery_charging) + ")" : "");
        batteryInfoTextView.setText(batteryInfo);
    }

    // 获取网络类型
    private void updateNetworkInfo() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        
        String networkType = getString(R.string.network_disconnected);
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                networkType = getString(R.string.network_wifi);
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null && wifiInfo.getSSID() != null) {
                    networkType += " (" + wifiInfo.getSSID().replace("\"", "") + ")";
                }
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                networkType = getString(R.string.network_mobile) + " (" + activeNetwork.getSubtypeName() + ")";
            } else {
                networkType = activeNetwork.getTypeName();
            }
        }
        
        networkTypeTextView.setText(networkType);
    }

    // 获取MAC地址
    private void updateMacAddress() {
        String macAddress = getString(R.string.not_available);
        
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wInfo = wifiManager.getConnectionInfo();
            macAddress = wInfo.getMacAddress();
            
            if (macAddress == null || macAddress.equals("02:00:00:00:00:00")) {
                // Android 6.0及以上获取MAC地址的方法
                try {
                    List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
                    for (NetworkInterface nif : all) {
                        if (!nif.getName().equalsIgnoreCase("wlan0")) continue;
                        
                        byte[] macBytes = nif.getHardwareAddress();
                        if (macBytes == null) {
                            macAddress = getString(R.string.unable_to_get);
                        } else {
                            StringBuilder sb = new StringBuilder();
                            for (byte b : macBytes) {
                                sb.append(String.format("%02X:", b));
                            }
                            if (sb.length() > 0) {
                                sb.deleteCharAt(sb.length() - 1);
                            }
                            macAddress = sb.toString();
                        }
                    }
                } catch (Exception ex) {
                    macAddress = getString(R.string.not_available);
                }
            }
        } catch (Exception e) {
            macAddress = getString(R.string.not_available);
        }
        
        macAddressTextView.setText(macAddress);
    }
}
