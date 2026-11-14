package com.kalaiyaonline.news;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFFFFFFFF);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.splash_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int size = Math.round(240 * getResources().getDisplayMetrics().density);
        root.addView(logo, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        setContentView(root);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing()) return;
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }, 1100);
    }
}
