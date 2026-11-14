package com.kalaiyaonline.news;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

final class PreetiBoard {
    private final AppCompatActivity activity;
    private final LinearLayout root;
    private final EditText input;
    private final TextView output;
    private final TextView inputCount;
    private final TextView outputCount;
    private final TextView toUnicode;
    private final TextView toPreeti;
    private boolean preetiToUnicode = true;

    PreetiBoard(AppCompatActivity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        TextView intro = text("पुरानो Preeti र नेपाली Unicode यही फोनमा बदल्नुहोस्। पाठ सर्भरमा जाँदैन।", 14, 0xFF57534E, false);
        intro.setPadding(0, 0, 0, dp(8));
        root.addView(intro);

        LinearLayout modes = new LinearLayout(activity);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        toUnicode = chip("Preeti › Unicode", true);
        toPreeti = chip("Unicode › Preeti", false);
        toUnicode.setOnClickListener(v -> setMode(true, false));
        toPreeti.setOnClickListener(v -> setMode(false, false));
        modes.addView(toUnicode, weight());
        modes.addView(toPreeti, weight());
        root.addView(modes);

        TextView swap = text("दिशा बदल्नुहोस्", 14, 0xFF0B5C28, true);
        swap.setGravity(Gravity.CENTER);
        swap.setPadding(0, dp(8), 0, dp(4));
        swap.setOnClickListener(v -> setMode(!preetiToUnicode, true));
        root.addView(swap);

        inputCount = text("0 अक्षर", 12, 0xFF78716C, false);
        root.addView(inputCount);
        input = new EditText(activity);
        input.setMinLines(5);
        input.setTextColor(0xFF1C1917);
        input.setTextSize(16);
        input.setHint("यहाँ Preeti text paste गर्नुहोस्…");
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        input.setBackground(field());
        input.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) { render(); }
        });
        root.addView(input);

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(action("नमूना", false, this::sample), weight());
        actions.addView(action("खाली", false, () -> input.setText("")), weight());
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsLp.topMargin = dp(8);
        root.addView(actions, actionsLp);

        outputCount = text("0 अक्षर", 12, 0xFF78716C, false);
        outputCount.setPadding(0, dp(10), 0, 0);
        root.addView(outputCount);
        output = text("", 17, 0xFF1C1917, false);
        output.setMinHeight(dp(120));
        output.setTextIsSelectable(true);
        output.setPadding(dp(12), dp(10), dp(12), dp(10));
        output.setBackground(field());
        root.addView(output);
        root.addView(action("आउटपुट कपी", true, this::copy));
    }

    View view() {
        return root;
    }

    private void setMode(boolean next, boolean carry) {
        if (carry) input.setText(output.getText());
        preetiToUnicode = next;
        toUnicode.setBackground(chipBg(next));
        toUnicode.setTextColor(next ? 0xFFFFFFFF : 0xFF0B5C28);
        toPreeti.setBackground(chipBg(!next));
        toPreeti.setTextColor(!next ? 0xFFFFFFFF : 0xFF0B5C28);
        input.setHint(next ? "यहाँ Preeti text paste गर्नुहोस्…" : "यहाँ Unicode नेपाली paste गर्नुहोस्…");
        render();
    }

    private void render() {
        String value = input.getText().toString();
        String converted = preetiToUnicode ? PreetiConverter.preetiToUnicode(value) : PreetiConverter.unicodeToPreeti(value);
        output.setText(converted);
        inputCount.setText(value.codePointCount(0, value.length()) + " अक्षर");
        outputCount.setText(converted.codePointCount(0, converted.length()) + " अक्षर");
    }

    private void sample() {
        input.setText(preetiToUnicode ? "g]kfn /fd k|sfz" : "नेपाल सरकार प्रकाशित समाचार");
    }

    private void copy() {
        String value = output.getText().toString();
        if (value.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("preeti", value));
        Toast.makeText(activity, "कपी भयो", Toast.LENGTH_SHORT).show();
    }

    private TextView chip(String label, boolean active) {
        TextView chip = text(label, 13, active ? 0xFFFFFFFF : 0xFF0B5C28, true);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(8), dp(10), dp(8), dp(10));
        chip.setBackground(chipBg(active));
        return chip;
    }

    private TextView action(String label, boolean solid, Runnable task) {
        TextView button = text(label, 14, solid ? 0xFFFFFFFF : 0xFF0B5C28, true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setBackground(solid ? chipBg(true) : chipBg(false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        button.setLayoutParams(lp);
        button.setOnClickListener(v -> task.run());
        return button;
    }

    private GradientDrawable field() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), 0xFFE7E5E4);
        return bg;
    }

    private GradientDrawable chipBg(boolean active) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(active ? 0xFF0F8A3C : 0xFFE5F6EC);
        return bg;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = dp(6);
        return lp;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(ResourcesCompat.getFont(activity, bold ? R.font.mukta_bold : R.font.mukta_regular));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
