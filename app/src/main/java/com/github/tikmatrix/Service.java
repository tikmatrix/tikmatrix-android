package com.github.tikmatrix;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class Service extends android.app.Service {
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final String TAG = "UIAService";
    private static final int NOTIFICATION_ID = 0x1;

    @Override
    public IBinder onBind(Intent intent) {
        // We don't support binding to this service
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Stopping service");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "On StartCommand");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TikMatrix Service")
                .setContentText("TikMatrix service is running")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        // Do the work that should be done by this service
        return START_NOT_STICKY;
    }

    @Override
    public void onLowMemory() {
        Log.w(TAG, "Low memory");
    }

}
