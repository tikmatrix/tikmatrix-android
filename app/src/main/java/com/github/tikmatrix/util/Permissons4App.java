package com.github.tikmatrix.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

import com.github.tikmatrix.MainActivity;

import java.util.List;

public class Permissons4App {
    private static final String TAG = Permissons4App.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 1000;

    /**
     * init permissions
     */
    public static void initPermissions(Activity activity, String[] permissions) {
        initPermissions(activity, permissions, PERMISSION_REQUEST_CODE);
    }

    /**
     * init permissions with request code
     */
    public static void initPermissions(Activity activity, String[] permissions, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isAllGranted(activity, permissions)) {
                Log.i(TAG, "Permissions all granted");
            } else {
                Log.i(TAG, "Request permissions");
                ActivityCompat.requestPermissions(activity, permissions, requestCode);
            }
        }
    }

    /**
     * Determine whether all specified permissions have been authorized
     */
    private static boolean isAllGranted(Context context, String[] permissions) {
        return checkPermissionAllGranted(context, permissions);
    }

    /**
     * Check whether app have all the specified permissions
     */
    private static boolean checkPermissionAllGranted(Context context, String[] permissions) {
        // if permissions all granted, it will return true, otherwise it will return
        // false.
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(context, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * handle request permissions result
     */
    public static void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        handleRequestPermissionsResult(requestCode, permissions, grantResults, PERMISSION_REQUEST_CODE);
    }

    /**
     * handle request permissions result with request code
     */
    public static void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults,
            int customRequestCode) {
        if (requestCode == customRequestCode) {
            boolean isAllGranted = true;
            // Determine whether all specified permissions have been authorized
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }
            if (!isAllGranted) {
                // TODO: Pop-up dialog box tells the user why he needs permission and guides the
                // user to open permission manually in application permission management.
            }
        }
    }

    public static boolean isAppInstalled(MainActivity mainActivity, String packageName) {
        PackageManager pm = mainActivity.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isAppRunning(MainActivity mainActivity, String packageName) {
        ActivityManager am = (ActivityManager) mainActivity.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        Log.i("runningAppProcess: ", String.valueOf(runningAppProcesses.size()));
        for (ActivityManager.RunningAppProcessInfo runningAppProcess : runningAppProcesses) {
            Log.i("runningAppProcess: ", runningAppProcess.processName);
            if (runningAppProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
