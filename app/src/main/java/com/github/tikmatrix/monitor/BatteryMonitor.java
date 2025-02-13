package com.github.tikmatrix.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

/**
 * Created by hzsunshx on 2017/11/15.
 */

public class BatteryMonitor extends AbstractMonitor {
    private static final String TAG = "UIABatteryMonitor";

    private static final String USB_STATE_CHANGE = "android.hardware.usb.action.USB_STATE";
    public BroadcastReceiver receiver = null;

    public BatteryMonitor(Context context, HttpPostNotifier notifier) {
        super(context, notifier);
    }

    @Override
    public void init() {
        Log.i(TAG, "Battery monitor init");
        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                report(notifier, intent);
            }
        };
    }

    @Override
    public void register() {
        Log.i(TAG, "Register BatteryMonitor");

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(USB_STATE_CHANGE);
        context.registerReceiver(receiver, filter);
    }

    @Override
    public void unregister() {
        if (receiver != null) {
            Log.i(TAG, "battery unregistered");
            context.unregisterReceiver(receiver);
        }
    }

    private void report(HttpPostNotifier notifier, Intent intent) {
        Integer level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        Log.d(TAG, "notify battery changed. current level " + level);
        notifier.Notify("/info/battery", String.valueOf(level));
    }
}
