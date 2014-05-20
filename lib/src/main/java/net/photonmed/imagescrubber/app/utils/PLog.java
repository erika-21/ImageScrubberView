package net.photonmed.imagescrubber.app.utils;

import android.util.Log;

/**
 * Created by erikbuttram on 5/20/14.
 */
public class PLog {

    public static boolean Debug = true;

    public enum LogLevel {
        VERBOSE,
        DEBUG,
        ERROR,
        WARNING
    }

    public static void setDebug(boolean debugValue) {
        Debug = debugValue;
    }

    public static void l(String TAG, LogLevel level, String message) {

        switch (level) {
            case VERBOSE:
                Log.v(TAG, message);
                break;
            case DEBUG:
                Log.d(TAG, message);
                break;
            case WARNING:
                Log.w(TAG, message);
                break;
            case ERROR:
                Log.e(TAG, message);
        }
    }
}
