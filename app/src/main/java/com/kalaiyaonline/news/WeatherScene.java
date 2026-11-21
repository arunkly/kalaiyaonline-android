package com.kalaiyaonline.news;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;

public final class WeatherScene extends View {
    public static final int SUN = 0;
    public static final int PARTLY = 1;
    public static final int CLOUD = 2;
    public static final int RAIN = 3;
    public static final int STORM = 4;
    public static final int SNOW = 5;
    public static final int FOG = 6;
    public static final int NIGHT = 7;

    private int kind = SUN;
    private float tick;
    private boolean plate = true;
    private ValueAnimator animator;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final float[] seeds = new float[40];

    public WeatherScene(Context context) {
        super(context);
        java.util.Random random = new java.util.Random(4);
        for (int i = 0; i < seeds.length; i++) seeds[i] = random.nextFloat();
    }

    public static int drawable(int kind) {
        if (kind == NIGHT) return R.drawable.ic_wx_moon;
        if (kind == PARTLY) return R.drawable.ic_wx_partly;
        if (kind == CLOUD) return R.drawable.ic_wx_cloud;
        if (kind == RAIN) return R.drawable.ic_wx_rain;
        if (kind == STORM) return R.drawable.ic_wx_storm;
        if (kind == SNOW) return R.drawable.ic_wx_snow;
        if (kind == FOG) return R.drawable.ic_wx_fog;
        return R.drawable.ic_wx_sun;
    }

    public static int kindFor(int code, boolean day) {
        if (code == 0) return day ? SUN : NIGHT;
        if (code <= 2) return day ? PARTLY : NIGHT;
        if (code == 3) return CLOUD;
        if (code == 45 || code == 48) return FOG;
        if (code >= 95) return STORM;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return SNOW;
        if (code >= 51) return RAIN;
        return CLOUD;
    }

    public static int kindForMet(String code) {
        String value = code == null ? "" : code;
        boolean night = value.contains("night");
        if (value.contains("thunder")) return STORM;
        if (value.contains("snow") || value.contains("sleet")) return SNOW;
        if (value.contains("rain") || value.contains("drizzle")) return RAIN;
        if (value.contains("fog")) return FOG;
        if (value.contains("cloud")) return value.contains("partly") || value.contains("fair") ? (night ? NIGHT : PARTLY) : CLOUD;
        return night ? NIGHT : SUN;
    }

    public void setKind(int next) {
        kind = next;
        invalidate();
    }

    private boolean live = true;

    public void setPlate(boolean plate) {
        this.plate = plate;
        invalidate();
    }

    public void setLive(boolean live) {
        this.live = live;
        if (!live) {
            if (animator != null) animator.cancel();
            tick = 0.35f;
            invalidate();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!live) {
            tick = 0.35f;
            invalidate();
            return;
        }
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(3200);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            tick = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w < 2 || h < 2) return;
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        canvas.save();
        if (!plate) {
            drawBadge(canvas, w, h);
            canvas.restore();
            return;
        }
        if (plate) {
            float radius = h * 0.22f;
            clip.reset();
            clip.addRoundRect(0, 0, w, h, radius, radius, Path.Direction.CW);
            canvas.clipPath(clip);
            int[] sky = sky();
            paint.setShader(new LinearGradient(0, 0, 0, h, sky[0], sky[1], Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
        }
        float radius = Math.min(w, h) * (plate ? 0.16f : 0.30f);
        if (kind == SUN || kind == PARTLY) drawSun(canvas, w * (plate ? 0.30f : 0.42f), h * 0.46f, radius);
        if (kind == NIGHT) drawMoon(canvas, w, h);
        if (kind == CLOUD || kind == PARTLY || kind == RAIN || kind == STORM || kind == SNOW) drawClouds(canvas, w, h);
        if (kind == RAIN || kind == STORM) drawRain(canvas, w, h);
        if (plate && kind == STORM && tick > 0.84f && tick < 0.93f) {
            paint.setColor(0x55FFFFFF);
            canvas.drawRect(0, 0, w, h, paint);
        }
        if (kind == SNOW) drawSnow(canvas, w, h);
        if (kind == FOG) drawFog(canvas, w, h);
        canvas.restore();
    }

    private int[] sky() {
        if (kind == SUN) return new int[]{0xFF7EC8FF, 0xFFFFE29A};
        if (kind == PARTLY) return new int[]{0xFF79B7F2, 0xFFD5E7F6};
        if (kind == RAIN) return new int[]{0xFF516579, 0xFF9AABBA};
        if (kind == STORM) return new int[]{0xFF243140, 0xFF5C6D7E};
        if (kind == SNOW) return new int[]{0xFFD5E4F0, 0xFFF8FBFE};
        if (kind == FOG) return new int[]{0xFFC5CED6, 0xFFECEFF2};
        if (kind == NIGHT) return new int[]{0xFF101A33, 0xFF2A4068};
        return new int[]{0xFF8AA0B4, 0xFFD7E0E8};
    }

    private void drawBadge(Canvas canvas, float w, float h) {
        float cx = w * 0.5f;
        float cy = h * 0.5f;
        float radius = Math.min(w, h) * 0.22f;
        if (kind == NIGHT) {
            drawMoon(canvas, w, h);
            return;
        }
        if (kind == SUN || kind == PARTLY) drawSun(canvas, kind == PARTLY ? cx - radius * 0.45f : cx, cy - radius * 0.2f, radius);
        if (kind == CLOUD || kind == PARTLY || kind == RAIN || kind == STORM || kind == SNOW || kind == FOG) drawClouds(canvas, w, h);
        if (kind == RAIN || kind == STORM) drawRain(canvas, w, h);
        if (kind == SNOW) drawSnow(canvas, w, h);
        if (kind == FOG) drawFog(canvas, w, h);
        if (kind == STORM) drawBolt(canvas, cx, cy + radius * 0.2f, radius);
    }

    private void drawBolt(Canvas canvas, float cx, float cy, float radius) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFFC857);
        Path bolt = new Path();
        bolt.moveTo(cx + radius * 0.15f, cy - radius);
        bolt.lineTo(cx - radius * 0.45f, cy + radius * 0.15f);
        bolt.lineTo(cx - radius * 0.05f, cy + radius * 0.15f);
        bolt.lineTo(cx - radius * 0.25f, cy + radius);
        bolt.lineTo(cx + radius * 0.5f, cy - radius * 0.1f);
        bolt.lineTo(cx + radius * 0.05f, cy - radius * 0.1f);
        bolt.close();
        canvas.drawPath(bolt, paint);
    }

    private void drawSun(Canvas canvas, float cx, float cy, float radius) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(0xFFFFC857);
        canvas.save();
        canvas.rotate(tick * 360f, cx, cy);
        for (int i = 0; i < 8; i++) {
            canvas.rotate(45f, cx, cy);
            float length = radius * (1.7f + 0.16f * (i % 2));
            paint.setStrokeWidth(Math.max(2f, radius * 0.18f));
            canvas.drawLine(cx, cy - radius * 1.2f, cx, cy - length, paint);
        }
        canvas.restore();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFFF3C4);
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setColor(0xFFFFB703);
        canvas.drawCircle(cx, cy, radius * 0.72f, paint);
    }

    private void drawMoon(Canvas canvas, float w, float h) {
        float cx = plate ? w * 0.28f : w * 0.5f;
        float cy = (plate ? h * 0.38f : h * 0.5f) + (plate ? 0f : (float) Math.sin(tick * 6.28f) * Math.min(w, h) * 0.04f);
        float radius = Math.min(w, h) * (plate ? 0.16f : 0.34f);
        if (!plate) {
            Path moon = new Path();
            moon.addCircle(cx, cy, radius, Path.Direction.CW);
            Path cut = new Path();
            cut.addCircle(cx + radius * 0.46f, cy - radius * 0.08f, radius * 0.72f, Path.Direction.CW);
            moon.op(cut, Path.Op.DIFFERENCE);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFFFC857);
            canvas.drawPath(moon, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(Math.max(2.2f, radius * 0.14f));
            paint.setColor(0xFFF15A22);
            canvas.drawPath(moon, paint);
            paint.setStyle(Paint.Style.FILL);
            return;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFFF6D8);
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setColor(0xFF1A2C52);
        canvas.drawCircle(cx + radius * 0.45f, cy - radius * 0.15f, radius * 0.82f, paint);
        paint.setColor(0xFFFFFFFF);
        for (int i = 0; i < 8; i++) {
            float alpha = 0.35f + 0.65f * Math.abs((float) Math.sin((tick + seeds[i]) * 6.28f));
            paint.setAlpha((int) (alpha * 255));
            canvas.drawCircle(seeds[i + 8] * w, seeds[i + 16] * h * 0.7f, 1.6f + seeds[i] * 1.8f, paint);
        }
        paint.setAlpha(255);
    }

    private void drawClouds(Canvas canvas, float w, float h) {
        paint.setStyle(Paint.Style.FILL);
        if (!plate) {
            float size = Math.min(w, h);
            float drift = (tick - 0.5f) * size * 0.05f;
            float cx = w * 0.46f + drift;
            float cy = h * 0.54f;
            float r = size * 0.16f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFFFFFFF);
            blob(canvas, cx, cy, r);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2.2f, size * 0.045f));
            paint.setColor(0xFF0F8A3C);
            blob(canvas, cx, cy, r);
            paint.setStyle(Paint.Style.FILL);
            return;
        }
        paint.setColor(kind == STORM ? 0xFFE7EEF4 : 0xFFFFFFFF);
        float drift = (tick * 1.3f) % 1f;
        float cx = plate ? w * ((0.55f + drift) % 1.15f - 0.08f) : w * 0.5f;
        blob(canvas, cx, h * (plate ? 0.62f : 0.60f), h * (plate ? 0.16f : 0.20f));
        if (plate) blob(canvas, w * ((0.15f + drift * 0.7f) % 1.2f - 0.1f), h * 0.34f, h * 0.12f);
    }

    private void blob(Canvas canvas, float cx, float cy, float r) {
        canvas.drawCircle(cx, cy, r, paint);
        canvas.drawCircle(cx + r * 0.9f, cy + r * 0.15f, r * 0.78f, paint);
        canvas.drawCircle(cx - r * 0.85f, cy + r * 0.2f, r * 0.7f, paint);
    }

    private void drawRain(Canvas canvas, float w, float h) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(!plate ? 0xFF1D6FBF : kind == STORM ? 0xFFD6E4FF : 0xFFEFF6FF);
        paint.setStrokeWidth(Math.max(1.5f, w * 0.012f));
        for (int i = 0; i < 18; i++) {
            float x = seeds[i] * w;
            float speed = 0.85f + seeds[i + 10] * 0.7f;
            float y = ((seeds[i + 20] + tick * speed) % 1.15f - 0.08f) * h;
            canvas.drawLine(x, y, x - w * 0.03f, y + h * 0.12f, paint);
        }
    }

    private void drawSnow(Canvas canvas, float w, float h) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFFFFFF);
        for (int i = 0; i < 16; i++) {
            float x = (seeds[i] * w + tick * w * 0.08f * (i % 2 == 0 ? 1 : -1) + w) % w;
            float y = ((seeds[i + 16] + tick * (0.35f + seeds[i] * 0.4f)) % 1.1f) * h;
            canvas.drawCircle(x, y, 1.5f + seeds[i] * 2.2f, paint);
        }
    }

    private void drawFog(Canvas canvas, float w, float h) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x99FFFFFF);
        if (!plate) paint.setColor(0x6694A3AD);
        for (int i = 0; i < 3; i++) {
            float shift = (tick * w * 0.25f + i * w * 0.2f) % (w * 0.5f);
            float y = h * (0.28f + i * 0.22f);
            canvas.drawRoundRect(shift - w * 0.2f, y, shift + w * 0.85f, y + h * 0.12f, h, h, paint);
        }
    }
}
