package com.kalaiyaonline.news;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

final class DockBar extends LinearLayout {
    interface Listener {
        void onSelect(int id);
        void onReselect(int id);
    }

    private static final int[] IDS = {
            R.id.nav_gallery, R.id.nav_directory, R.id.nav_home, R.id.nav_social, R.id.nav_saved, R.id.nav_more
    };
    private static final int[] ICONS = {
            R.drawable.ic_nav_photo, R.drawable.ic_nav_pin, R.drawable.ic_nav_home, R.drawable.ic_nav_social, R.drawable.ic_nav_save, R.drawable.ic_nav_more
    };
    private static final String[] LABELS = {"फोटो", "डाइरेक्टरी", "गृह", "समुदाय", "सेभ", "थप"};
    private static final int ACTIVE = 0xFFF15A22;
    private static final int IDLE = 0xFF1A1A1A;

    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ImageView[] icons = new ImageView[IDS.length];
    private final TextView[] labels = new TextView[IDS.length];
    private Listener listener;
    private int selected = R.id.nav_home;

    DockBar(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setWillNotDraw(false);
        setBackgroundColor(0xFFFFFFFF);
        for (int i = 0; i < IDS.length; i++) {
            int id = IDS[i];
            LinearLayout slot = new LinearLayout(context);
            slot.setOrientation(VERTICAL);
            slot.setGravity(Gravity.CENTER);
            ImageView icon = new ImageView(context);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            slot.addView(icon, new LayoutParams(dp(22), dp(22)));
            TextView label = new TextView(context);
            label.setText(LABELS[i]);
            label.setTextSize(11);
            label.setGravity(Gravity.CENTER);
            label.setIncludeFontPadding(false);
            label.setPadding(0, dp(2), 0, 0);
            slot.addView(label);
            slot.setOnClickListener(v -> choose(id));
            icons[i] = icon;
            labels[i] = label;
            LayoutParams lp = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            addView(slot, lp);
        }
        paintSelection();
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setSelectedItemId(int id) {
        if (id == selected) {
            if (listener != null) listener.onReselect(id);
            return;
        }
        selected = id;
        paintSelection();
        if (listener != null) listener.onSelect(id);
    }

    int getSelectedItemId() {
        return selected;
    }

    static int inset(Context context) {
        return Math.round(64 * context.getResources().getDisplayMetrics().density);
    }

    private void choose(int id) {
        setSelectedItemId(id);
    }

    private void paintSelection() {
        boolean dark = Night.on(getContext());
        setBackgroundColor(dark ? 0xFF242526 : 0xFFFFFFFF);
        for (int i = 0; i < IDS.length; i++) {
            boolean on = IDS[i] == selected;
            int color = on ? ACTIVE : (dark ? 0xFFE7EEEA : IDLE);
            icons[i].setImageDrawable(tinted(ICONS[i], color));
            labels[i].setTextColor(color);
            labels[i].setTypeface(ResourcesCompat.getFont(getContext(), on ? R.font.mukta_semibold : R.font.mukta_regular));
        }
    }

    private Drawable tinted(int icon, int color) {
        Drawable drawable = DrawableCompat.wrap(getContext().getDrawable(icon).mutate());
        DrawableCompat.setTint(drawable, color);
        return drawable;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        line.setColor(Night.on(getContext()) ? 0xFF3E4042 : 0xFFCED0D4);
        line.setStrokeWidth(Math.max(1, dp(1)));
        canvas.drawLine(0, 0, getWidth(), 0, line);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
