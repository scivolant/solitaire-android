package com.droidcluster.solitaire.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;

public class Util {

    public static void updateFullScreen(Window window, boolean fullScreen) {
        WindowManager.LayoutParams attrs = window.getAttributes();
        if (fullScreen) {
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        } else {
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        window.setAttributes(attrs);
    }

    public static boolean getPrefBoolean(int strId, Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getBoolean(c.getString(strId), false);
    }

    public static String getPrefString(int strId, Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getString(c.getString(strId), null);
    }

    public static void close(Closeable toClose) {
        if (toClose != null) {
            try {
                toClose.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static boolean copyAndClose(InputStream is, OutputStream os) {
        byte[] buffer = new byte[4096];

        try {
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            close(is);
            close(os);
        }
    }
}
