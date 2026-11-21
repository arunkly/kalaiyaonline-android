package com.kalaiyaonline.news;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                FileOutputStream output = new FileOutputStream(new File(getFilesDir(), "crash.txt"));
                PrintWriter writer = new PrintWriter(output);
                error.printStackTrace(writer);
                writer.flush();
                output.close();
            } catch (Exception ignored) {
            }
            if (previous != null) previous.uncaughtException(thread, error);
        });
        AppCompatDelegate.setDefaultNightMode(Night.on(this)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
        Account.use(this);
    }
}