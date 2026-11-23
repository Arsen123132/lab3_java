package com.lab3.activitytracker;

import android.util.Log;

public final class AppLogger {
    private static final String TAG = "ActivityTracker";

    private AppLogger() {
    }

    public static void d(String message) {
        Log.d(TAG, message);
    }

    public static void i(String message) {
        Log.i(TAG, message);
    }

    public static void w(String message, Throwable error) {
        Log.w(TAG, message, error);
    }

    public static void e(String message, Throwable error) {
        Log.e(TAG, message, error);
    }
}

