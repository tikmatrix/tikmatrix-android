package com.github.tikmatrix;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("TikMatrix");

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
                    tvRunningStatus.setText("Success");
                    tvRunningStatus.setTextColor(Color.GREEN);
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
            tvRunningStatus.setText("Agent is running!");
            tvRunningStatus.setTextColor(Color.GREEN);
            return;
        }
        boolean isInstalled = Permissons4App.isAppInstalled(MainActivity.this, "com.github.tikmatrix.test");
        if (!isInstalled) {
            tvRunningStatus.setText("Agent not installed!");
            tvRunningStatus.setTextColor(Color.RED);
        } else {
            tvRunningStatus.setText("Agent not started!");
            tvRunningStatus.setTextColor(Color.RED);
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
        checkNetworkAddress(null);
        testUiautomator();
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
    }
}
