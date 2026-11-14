package com.kalaiyaonline.news;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

import androidx.appcompat.app.AppCompatDelegate;

final class Night {
    private Night() {}

    static boolean on(Context context) {
        return context.getSharedPreferences("kalaiya", Context.MODE_PRIVATE).getBoolean("dark", false);
    }

    static void set(Context context, boolean dark) {
        context.getSharedPreferences("kalaiya", Context.MODE_PRIVATE).edit().putBoolean("dark", dark).apply();
    }

    static void sync(Activity activity) {
        int want = on(activity) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        if (AppCompatDelegate.getDefaultNightMode() != want) AppCompatDelegate.setDefaultNightMode(want);
        Window window = activity.getWindow();
        int bar = on(activity) ? 0xFF101412 : 0xFFFFFFFF;
        window.setStatusBarColor(bar);
        window.setNavigationBarColor(bar);
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                int flags = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(on(activity) ? 0 : flags, flags);
            }
        } else {
            View decor = window.getDecorView();
            int flags = decor.getSystemUiVisibility();
            if (on(activity)) flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            else flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decor.setSystemUiVisibility(flags);
        }
    }

    static void allow(View view, boolean enabled) {
        if (Build.VERSION.SDK_INT >= 29) view.setForceDarkAllowed(enabled);
    }
}
