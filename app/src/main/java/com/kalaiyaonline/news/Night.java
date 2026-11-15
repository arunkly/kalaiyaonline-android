package com.kalaiyaonline.news;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

final class Night {
    private Night() {}

    static boolean on(Context context) {
        return context.getSharedPreferences("kalaiya", Context.MODE_PRIVATE).getBoolean("dark", false);
    }

    static void set(Context context, boolean dark) {
        context.getSharedPreferences("kalaiya", Context.MODE_PRIVATE).edit().putBoolean("dark", dark).apply();
    }

    static void sync(Activity activity) {
        Window window = activity.getWindow();
        int bar = on(activity) ? 0xFF101412 : 0xFFFFFFFF;
        window.setStatusBarColor(bar);
        window.setNavigationBarColor(bar);
        try {
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
        } catch (RuntimeException ignored) {
        }
    }

    static void belowStatus(Activity activity, View screen) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(screen, (view, insets) -> {
            Insets cut = insets.getInsets(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(view.getPaddingLeft(), cut.top, view.getPaddingRight(), view.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(screen);
    }

    static void allow(View view, boolean enabled) {
        if (Build.VERSION.SDK_INT >= 29) view.setForceDarkAllowed(enabled);
    }
}
