package com.github.tikmatrix;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;

public class AdbBroadcastReceiver extends BroadcastReceiver {

    private MockLocationProvider mockGPS;
    private MockLocationProvider mockWifi;
    private static final String TAG = "AdbBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "received intent: " + intent.getAction());
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        switch (action) {
            case "send.mock":
                mockGPS = new MockLocationProvider(LocationManager.GPS_PROVIDER, context);
                mockWifi = new MockLocationProvider(LocationManager.NETWORK_PROVIDER, context);

                double lat = Double
                        .parseDouble(intent.getStringExtra("lat") != null ? intent.getStringExtra("lat") : "0");
                double lon = Double
                        .parseDouble(intent.getStringExtra("lon") != null ? intent.getStringExtra("lon") : "0");
                double alt = Double
                        .parseDouble(intent.getStringExtra("alt") != null ? intent.getStringExtra("alt") : "0");
                float accurate = Float.parseFloat(
                        intent.getStringExtra("accurate") != null ? intent.getStringExtra("accurate") : "0");
                Log.i(TAG, String.format("setting mock to Latitude=%f, Longitude=%f Altitude=%f Accuracy=%f", lat, lon,
                        alt, accurate));
                mockGPS.pushLocation(lat, lon, alt, accurate);
                mockWifi.pushLocation(lat, lon, alt, accurate);
                break;
            case "stop.mock":
                if (mockGPS != null) {
                    mockGPS.shutdown();
                }
                if (mockWifi != null) {
                    mockWifi.shutdown();
                }
                break;
            case "com.github.tikmatrix.ACTION.SHOW_TOAST":
                String message = intent.getStringExtra("toast_text");
                int duration = intent.getIntExtra("duration", Toast.LENGTH_SHORT);
                Toast.makeText(context, message, duration).show();
                break;
            default:
                break;
        }

    }
}
