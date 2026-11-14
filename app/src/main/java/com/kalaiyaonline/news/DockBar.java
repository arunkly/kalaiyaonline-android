package com.kalaiyaonline.news;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

final class DockBar extends FrameLayout {
    interface Listener {
        void onSelect(int id);
        void onReselect(int id);
    }

    private static final int[] IDS = {
            R.id.nav_gallery, R.id.nav_directory, R.id.nav_home, R.id.nav_saved, R.id.nav_more
    };
    private static final int[] ICONS = {
            R.drawable.ic_photo, R.drawable.ic_pin, R.drawable.ic_home, R.drawable.ic_bookmark, R.drawable.ic_more
    };
    private static final String[] LABELS = {"फोटो", "डाइरेक्टरी", "गृह", "सेभ", "थप"};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape = new Path();
    private final LinearLayout[] slots = new LinearLayout[4];
    private final ImageView[] icons = new ImageView[5];
    private final TextView[] labels = new TextView[4];
    private final ImageView home;
    private Listener listener;
    private int selected = R.id.nav_home;

    DockBar(Context context) {
        super(context);
        setWillNotDraw(false);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setShadowLayer(dp(10), 0, -dp(2), 0x22000000);
        home = circle();
        home.setOnClickListener(v -> choose(R.id.nav_home));
        addView(home, new LayoutParams(dp(56), dp(56), Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        int side = 0;
        for (int i = 0; i < IDS.length; i++) {
            int id = IDS[i];
            if (id == R.id.nav_home) {
                icons[i] = home;
                continue;
            }
            LinearLayout slot = new LinearLayout(context);
            slot.setOrientation(LinearLayout.VERTICAL);
            slot.setGravity(Gravity.CENTER);
            ImageView icon = new ImageView(context);
            icon.setImageDrawable(tinted(ICONS[i], 0xFF44403C));
            slot.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
            TextView label = new TextView(context);
            label.setText(LABELS[i]);
            label.setTextSize(11);
            label.setTextColor(0xFF44403C);
            label.setGravity(Gravity.CENTER);
            label.setTypeface(ResourcesCompat.getFont(context, R.font.mukta_semibold));
            label.setIncludeFontPadding(false);
            slot.addView(label);
            slot.setOnClickListener(v -> choose(id));
            icons[i] = icon;
            labels[side] = label;
            slots[side] = slot;
            side++;
            addView(slot);
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
        return Math.round(72 * context.getResources().getDisplayMetrics().density);
    }

    private void choose(int id) {
        setSelectedItemId(id);
    }

    private ImageView circle() {
        ImageView view = new ImageView(getContext());
        view.setImageDrawable(tinted(R.drawable.ic_home, Color.WHITE));
        view.setScaleType(ImageView.ScaleType.CENTER);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(0xFF0F8A3C);
        view.setBackground(bg);
        view.setElevation(dp(6));
        view.setPadding(dp(14), dp(14), dp(14), dp(14));
        return view;
    }

    private void paintSelection() {
        int side = 0;
        for (int i = 0; i < IDS.length; i++) {
            if (IDS[i] == R.id.nav_home) continue;
            boolean on = IDS[i] == selected;
            icons[i].setImageDrawable(tinted(ICONS[i], on ? 0xFF0F8A3C : 0xFF44403C));
            labels[side].setTextColor(on ? 0xFF0F8A3C : 0xFF44403C);
            side++;
        }
    }

    private Drawable tinted(int icon, int color) {
        Drawable drawable = DrawableCompat.wrap(getContext().getDrawable(icon).mutate());
        DrawableCompat.setTint(drawable, color);
        return drawable;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = dp(96);
        int slotWidth = Math.max(dp(56), width / 5);
        for (LinearLayout slot : slots) {
            slot.measure(MeasureSpec.makeMeasureSpec(slotWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(dp(58), MeasureSpec.EXACTLY));
        }
        home.measure(MeasureSpec.makeMeasureSpec(dp(56), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(56), MeasureSpec.EXACTLY));
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int col = width / 5;
        int slotWidth = slots[0].getMeasuredWidth();
        int y = dp(40);
        int[] columns = {0, 1, 3, 4};
        for (int i = 0; i < slots.length; i++) {
            int x = columns[i] * col + (col - slotWidth) / 2;
            slots[i].layout(x, y, x + slotWidth, y + slots[i].getMeasuredHeight());
        }
        int hx = (width - home.getMeasuredWidth()) / 2;
        home.layout(hx, dp(6), hx + home.getMeasuredWidth(), dp(6) + home.getMeasuredHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float barTop = dp(34);
        float cx = width / 2f;
        float radius = dp(36);
        shape.reset();
        shape.moveTo(0, getHeight());
        shape.lineTo(0, barTop + dp(16));
        shape.quadTo(0, barTop, dp(16), barTop);
        shape.lineTo(cx - radius - dp(12), barTop);
        shape.cubicTo(cx - radius, barTop, cx - radius, barTop + dp(8), cx - radius + dp(8), barTop + dp(16));
        shape.cubicTo(cx - dp(16), barTop + radius + dp(2), cx + dp(16), barTop + radius + dp(2), cx + radius - dp(8), barTop + dp(16));
        shape.cubicTo(cx + radius, barTop + dp(8), cx + radius, barTop, cx + radius + dp(12), barTop);
        shape.lineTo(width - dp(16), barTop);
        shape.quadTo(width, barTop, width, barTop + dp(16));
        shape.lineTo(width, getHeight());
        shape.close();
        canvas.drawPath(shape, paint);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
