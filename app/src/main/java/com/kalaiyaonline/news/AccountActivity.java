package com.kalaiyaonline.news;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AccountActivity extends AppCompatActivity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private LinearLayout body;
    private TextView note;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Night.sync(this);
        boolean dark = Night.on(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(dark ? 0xFF101412 : 0xFFF6F8F7);
        root.addView(top());
        note = text("", 14, 0xFFB42318, false);
        note.setVisibility(View.GONE);
        note.setPadding(dp(20), dp(12), dp(20), 0);
        root.addView(note);
        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(8), dp(16), dp(24));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        showWait("खाता जाँचिँदैछ…");
        io.execute(() -> {
            Account.Result result = Account.session();
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                if (result.loggedIn) showIn();
                else showLogin();
                if (!result.ok && !result.message.isEmpty()) flash(result.message, false);
            });
        });
    }

    private View top() {
        boolean dark = Night.on(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(dark ? 0xFF101412 : 0xFFFFFFFF);
        box.setElevation(dp(1));
        FrameLayout bar = new FrameLayout(this);
        bar.setPadding(dp(12), dp(8), dp(12), dp(8));
        ImageView logo = new ImageView(this);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setImageResource(R.drawable.wordmark);
        Night.allow(logo, false);
        bar.addView(logo, new FrameLayout.LayoutParams(dp(156), dp(34), Gravity.CENTER));
        TextView back = text("‹", 22, 0xFF0F8A3C, true);
        back.setGravity(Gravity.CENTER);
        GradientDrawable chip = new GradientDrawable();
        chip.setCornerRadius(dp(20));
        chip.setColor(dark ? 0xFF1C2822 : 0xFFE7F6EE);
        back.setBackground(chip);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new FrameLayout.LayoutParams(dp(40), dp(40), Gravity.START | Gravity.CENTER_VERTICAL));
        box.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        View accent = new View(this);
        accent.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFF0B5C28, 0xFF18A34A, 0xFFF15A22}));
        box.addView(accent, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));
        return box;
    }

    private void showWait(String message) {
        body.removeAllViews();
        body.addView(text(message, 15, Night.on(this) ? 0xFFD7E3DC : 0xFF57534E, false));
    }

    private void showLogin() {
        body.removeAllViews();
        body.addView(switcher(true));
        EditText id = field("इमेल वा मोबाइल नम्बर", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText password = field("पासवर्ड", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        CheckBox remember = new CheckBox(this);
        remember.setText("म सम्झन चाहन्छु");
        remember.setChecked(true);
        remember.setTextColor(Night.on(this) ? 0xFFE7EEEA : 0xFF292524);
        style(remember, 14, false);
        body.addView(id);
        body.addView(secret(password));
        body.addView(remember);
        body.addView(button("लग इन गर्नुहोस्", () -> submitLogin(id, password, remember.isChecked())));
        TextView forgot = text("पासवर्ड बिर्सनुभयो?", 14, 0xFF0F8A3C, true);
        forgot.setPadding(0, dp(14), 0, 0);
        forgot.setGravity(Gravity.CENTER);
        forgot.setOnClickListener(v -> showReset());
        body.addView(forgot);
    }

    private void showRegister() {
        body.removeAllViews();
        body.addView(switcher(false));
        EditText person = field("पूरा नाम", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        EditText phone = field("मोबाइल नम्बर", InputType.TYPE_CLASS_PHONE);
        EditText email = field("इमेल", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText password = field("पासवर्ड", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText again = field("पासवर्ड पुनः लेख्नुहोस्", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        body.addView(person);
        body.addView(phone);
        body.addView(email);
        body.addView(secret(password));
        body.addView(secret(again));
        body.addView(text("कम्तीमा १० अक्षर, ठूलो अक्षर, सानो अक्षर, अंक र विशेष चिन्ह।", 12, 0xFF78716C, false));
        body.addView(button("खाता बनाउनुहोस्", () -> submitRegister(person, phone, email, password, again)));
    }

    private void showReset() {
        body.removeAllViews();
        body.addView(text("पासवर्ड रिसेट", 22, Night.on(this) ? 0xFFF4F7F5 : 0xFF142033, true));
        EditText email = field("दर्ता भएको इमेल", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        body.addView(email);
        body.addView(button("रिसेट लिंक पठाउनुहोस्", () -> submitReset(email)));
        TextView back = text("लगइनमा फर्कनुहोस्", 14, 0xFF0F8A3C, true);
        back.setPadding(0, dp(14), 0, 0);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> showLogin());
        body.addView(back);
    }

    private void showIn() {
        body.removeAllViews();
        boolean dark = Night.on(this);
        String person = Account.name().trim();
        String contact = Account.contact().trim();
        boolean named = !person.isEmpty() && !person.contains("@");
        String mail = !contact.isEmpty() ? contact : (person.contains("@") ? person : "");
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(dark ? 0xFF1A2420 : 0xFFFFFFFF);
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);
        LinearLayout who = new LinearLayout(this);
        who.setOrientation(LinearLayout.HORIZONTAL);
        who.setGravity(Gravity.CENTER_VERTICAL);
        who.addView(avatar(named ? person : mail));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(14), 0, 0, 0);
        words.addView(text("नमस्कार", 14, 0xFF0F8A3C, true));
        TextView title = text(named ? person : "तपाईंको खाता", 22, dark ? 0xFFF4F7F5 : 0xFF142033, true);
        title.setPadding(0, dp(2), 0, 0);
        words.addView(title);
        if (!mail.isEmpty()) {
            TextView line = text(mail, 14, 0xFF78716C, false);
            line.setPadding(0, dp(2), 0, 0);
            words.addView(line);
        }
        who.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(who);
        TextView badge = text("लगइन सक्रिय", 13, 0xFF0B5C28, true);
        badge.setPadding(dp(10), dp(5), dp(10), dp(5));
        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(dp(12));
        pill.setColor(dark ? 0xFF143024 : 0xFFE7F6EE);
        badge.setBackground(pill);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.topMargin = dp(16);
        badge.setLayoutParams(badgeLp);
        card.addView(badge);
        TextView about = text("यो kalaiyaonline.com को खाता हो। समुदाय, टिप्पणी र रक्तदाता यही खाताबाट चल्छ।", 14, 0xFF57534E, false);
        about.setPadding(0, dp(12), 0, 0);
        about.setLineSpacing(dp(2), 1f);
        card.addView(about);
        body.addView(card);
        body.addView(button("लगआउट", () -> {
            if (busy) return;
            busy = true;
            io.execute(() -> {
                Account.logout();
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    busy = false;
                    flash("लगआउट भयो।", true);
                    showLogin();
                });
            });
        }));
    }

    private View avatar(String source) {
        FrameLayout frame = new FrameLayout(this);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0xFF0F8A3C);
        frame.setBackground(circle);
        String raw = source == null ? "" : source.trim();
        int at = raw.indexOf('@');
        if (at > 0) raw = raw.substring(0, at);
        String letter = "क";
        if (!raw.isEmpty()) letter = new String(Character.toChars(raw.codePointAt(0))).toUpperCase(java.util.Locale.ROOT);
        TextView mark = text(letter, 26, 0xFFFFFFFF, true);
        mark.setGravity(Gravity.CENTER);
        frame.addView(mark, new FrameLayout.LayoutParams(dp(64), dp(64), Gravity.CENTER));
        frame.setLayoutParams(new LinearLayout.LayoutParams(dp(64), dp(64)));
        return frame;
    }

    private View switcher(boolean login) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(4), dp(4), dp(4), dp(4));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Night.on(this) ? 0xFF1C2822 : 0xFFE8EEF2);
        bg.setCornerRadius(dp(14));
        row.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        row.setLayoutParams(lp);
        row.addView(tab("लगइन", login, this::showLogin), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tab("नयाँ खाता", !login, this::showRegister), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private View tab(String label, boolean on, Runnable action) {
        TextView view = text(label, 15, on ? 0xFFFFFFFF : 0xFF0F8A3C, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(8), dp(10), dp(8), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(11));
        bg.setColor(on ? 0xFF0F8A3C : 0x00000000);
        view.setBackground(bg);
        view.setOnClickListener(v -> action.run());
        return view;
    }

    private void submitLogin(EditText id, EditText password, boolean remember) {
        String who = id.getText().toString().trim();
        String pass = password.getText().toString();
        if (who.isEmpty() || pass.isEmpty()) {
            flash("इमेल वा मोबाइल र पासवर्ड लेख्नुहोस्।", false);
            return;
        }
        work(() -> Account.login(who, pass, remember), this::showIn);
    }

    private void submitRegister(EditText person, EditText phone, EditText email, EditText password, EditText again) {
        String name = person.getText().toString().trim();
        String mobile = phone.getText().toString().trim();
        String mail = email.getText().toString().trim();
        String pass = password.getText().toString();
        String repeat = again.getText().toString();
        if (name.length() < 2 || mobile.length() < 7 || !mail.contains("@")) {
            flash("नाम, मोबाइल र इमेल पूरा लेख्नुहोस्।", false);
            return;
        }
        if (!pass.equals(repeat)) {
            flash("पासवर्ड मिलेन।", false);
            return;
        }
        if (!strong(pass)) {
            flash("पासवर्ड कम्तीमा १० अक्षरको हुनुपर्छ। ठूलो अक्षर, सानो अक्षर, अंक र विशेष चिन्ह राख्नुहोस्।", false);
            return;
        }
        work(() -> Account.register(name, mobile, mail, pass), this::showIn);
    }

    private void submitReset(EditText email) {
        String mail = email.getText().toString().trim();
        if (!mail.contains("@")) {
            flash("इमेल लेख्नुहोस्।", false);
            return;
        }
        work(() -> Account.reset(mail), this::showLogin);
    }

    private void work(java.util.concurrent.Callable<Account.Result> task, Runnable done) {
        if (busy) return;
        busy = true;
        flash("कृपया पर्खनुहोस्…", true);
        io.execute(() -> {
            Account.Result result;
            try {
                result = task.call();
            } catch (Exception error) {
                result = new Account.Result(false, false, "काम पूरा भएन।", "");
            }
            Account.Result ready = result;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                busy = false;
                flash(ready.message, ready.ok);
                if (ready.ok && ready.loggedIn) done.run();
                else if (ready.ok) done.run();
            });
        });
    }

    private boolean strong(String password) {
        if (password.length() < 10) return false;
        boolean upper = false, lower = false, digit = false, special = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isLowerCase(c)) lower = true;
            else if (Character.isDigit(c)) digit = true;
            else special = true;
        }
        return upper && lower && digit && special;
    }

    private EditText field(String hint, int type) {
        boolean dark = Night.on(this);
        EditText input = new EditText(this);
        input.setHint(hint);
        if (passwordType(type)) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
            input.setMaxLines(1);
            input.setHorizontallyScrolling(true);
        } else {
            input.setSingleLine(true);
            input.setInputType(type);
        }
        input.setTextColor(dark ? 0xFFF4F7F5 : 0xFF1C1917);
        input.setHintTextColor(0xFF9AA89F);
        style(input, 16, false);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(dark ? 0xFF1A2420 : 0xFFFFFFFF);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), dark ? 0xFF2C3A34 : 0xFFE3EBE6);
        input.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        input.setLayoutParams(lp);
        return input;
    }

    private boolean passwordType(int type) {
        int variation = type & InputType.TYPE_MASK_VARIATION;
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;
    }

    private View secret(EditText input) {
        input.setPadding(dp(14), dp(12), dp(46), dp(12));
        ViewGroup.LayoutParams old = input.getLayoutParams();
        FrameLayout box = new FrameLayout(this);
        box.setLayoutParams(old);
        input.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ImageView eye = new ImageView(this);
        eye.setImageResource(R.drawable.ic_eye);
        eye.setPadding(dp(10), dp(10), dp(10), dp(10));
        eye.setContentDescription("पासवर्ड हेर्नुहोस्");
        FrameLayout.LayoutParams eyeLp = new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.END | Gravity.CENTER_VERTICAL);
        eyeLp.rightMargin = dp(2);
        boolean[] open = {false};
        eye.setOnClickListener(v -> {
            open[0] = !open[0];
            int start = Math.max(0, input.getSelectionStart());
            int end = Math.max(start, input.getSelectionEnd());
            input.setTransformationMethod(open[0] ? null : PasswordTransformationMethod.getInstance());
            eye.setImageResource(open[0] ? R.drawable.ic_eye_off : R.drawable.ic_eye);
            eye.setContentDescription(open[0] ? "पासवर्ड लुकाउनुहोस्" : "पासवर्ड हेर्नुहोस्");
            int len = input.getText() == null ? 0 : input.length();
            try {
                input.setSelection(Math.min(start, len), Math.min(end, len));
            } catch (RuntimeException ignored) {
            }
        });
        box.addView(input);
        box.addView(eye, eyeLp);
        return box;
    }

    private View button(String label, Runnable action) {
        TextView view = text(label, 15, 0xFFFFFFFF, true);
        view.setGravity(Gravity.CENTER);
        view.setIncludeFontPadding(false);
        view.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0F8A3C);
        bg.setCornerRadius(dp(12));
        view.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        view.setLayoutParams(lp);
        view.setOnClickListener(v -> action.run());
        return view;
    }

    private void flash(String message, boolean ok) {
        if (message == null || message.isEmpty()) {
            note.setVisibility(View.GONE);
            return;
        }
        note.setText(message);
        note.setTextColor(ok ? 0xFF0F8A3C : 0xFFB42318);
        note.setVisibility(View.VISIBLE);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        style(view, size, bold);
        return view;
    }

    private void style(TextView view, int size, boolean bold) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        Typeface face = ResourcesCompat.getFont(this, bold ? R.font.mukta_bold : R.font.mukta_regular);
        if (face != null) view.setTypeface(face);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
