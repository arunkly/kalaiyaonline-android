package com.kalaiyaonline.news;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

final class Ads {
    static final String APP_ID = "ca-app-pub-6927625499631220~1443076509";
    static final String BANNER = "ca-app-pub-6927625499631220/5952992145";
    private static boolean started;

    private Ads() {}

    static void start(Activity activity) {
        if (started) return;
        started = true;
        MobileAds.initialize(activity.getApplicationContext());
    }

    static AdView attach(Activity activity, LinearLayout parent) {
        start(activity);
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setBackgroundColor(0xFFFFFFFF);
        int pad = Math.round(6 * activity.getResources().getDisplayMetrics().density);
        box.setPadding(0, pad, 0, pad);
        TextView label = new TextView(activity);
        label.setText("विज्ञापन");
        label.setTextColor(0xFF78716C);
        label.setTextSize(11);
        label.setGravity(Gravity.CENTER);
        box.addView(label);
        AdView ad = new AdView(activity);
        ad.setAdUnitId(BANNER);
        LinearLayout.LayoutParams adLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        adLp.topMargin = pad / 2;
        box.addView(ad, adLp);
        parent.addView(box, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        ad.post(() -> load(activity, ad));
        return ad;
    }

    private static void load(Activity activity, AdView ad) {
        if (activity.isFinishing() || ad.getAdSize() != null) return;
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int width = Math.round(ad.getWidth() / metrics.density);
        if (width < 1) width = Math.round(metrics.widthPixels / metrics.density);
        ad.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, width));
        ad.loadAd(new AdRequest.Builder().build());
    }
}
