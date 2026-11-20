package com.kalaiyaonline.news;

import android.content.Context;
import android.content.SharedPreferences;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Account {
    static final String ORIGIN = "https://kalaiyaonline.com";
    private static final String PAGE = ORIGIN + "/account.php";
    private static final String RESET = ORIGIN + "/password-reset.php";
    private static final String UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36";
    private static Context app;

    private Account() {}

    static final class Result {
        final boolean ok;
        final boolean loggedIn;
        final String message;
        final String name;

        Result(boolean ok, boolean loggedIn, String message, String name) {
            this.ok = ok;
            this.loggedIn = loggedIn;
            this.message = message == null ? "" : message;
            this.name = name == null ? "" : name;
        }
    }

    static void use(Context context) {
        app = context.getApplicationContext();
    }

    static boolean loggedIn() {
        return prefs() != null && prefs().getBoolean("logged", false);
    }

    static String name() {
        return prefs() == null ? "" : prefs().getString("name", "");
    }

    static String contact() {
        return prefs() == null ? "" : prefs().getString("contact", "");
    }

    static void apply(HttpURLConnection connection) {
        String cookie = header();
        if (!cookie.isEmpty()) connection.setRequestProperty("Cookie", cookie);
    }

    static void capture(HttpURLConnection connection) {
        if (connection == null || prefs() == null) return;
        Map<String, List<String>> headers = connection.getHeaderFields();
        if (headers == null) return;
        List<String> lines = null;
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase("Set-Cookie")) lines = entry.getValue();
        }
        if (lines == null) return;
        LinkedHashMap<String, String> jar = load();
        boolean changed = false;
        for (String line : lines) {
            if (line == null || line.isEmpty()) continue;
            String pair = line.split(";", 2)[0];
            int cut = pair.indexOf('=');
            if (cut <= 0) continue;
            String key = pair.substring(0, cut).trim();
            String value = pair.substring(cut + 1).trim();
            String lower = line.toLowerCase();
            if (value.isEmpty() || lower.contains("max-age=0")) {
                changed |= jar.remove(key) != null;
                continue;
            }
            if (!value.equals(jar.get(key))) {
                jar.put(key, value);
                changed = true;
            }
        }
        if (changed) save(jar);
    }

    static Result session() {
        Page page = get(PAGE);
        if (page.html == null) return new Result(loggedIn(), loggedIn(), loggedIn() ? "" : "नेट ढिलो छ। फेरि प्रयास गर्नुहोस्।", name());
        if (loginForm(page.html)) {
            clearUser();
            return new Result(true, false, "", "");
        }
        String found = displayName(page.html);
        mark(found.isEmpty() ? name() : found, contact());
        return new Result(true, true, "", name());
    }

    static Result login(String id, String password, boolean remember) {
        Page page = get(PAGE);
        if (page.html == null) return fail("लगइन पृष्ठ खुलेन। नेट जाँच गर्नुहोस्।");
        String csrf = csrf(page.html);
        if (csrf.isEmpty()) return fail("लगइन पृष्ठ खुलेन।");
        String body = form("csrf", csrf, "mode", "login", "next", "", "ref", "", "email", id, "password", password);
        if (remember) body += "&remember=1";
        Page sent = post(PAGE, body);
        if (sent.html == null) return fail("जवाफ आएन। नेट ढिलो छ।");
        String error = error(sent.html);
        if (!error.isEmpty()) return fail(error);
        if (loginForm(sent.html)) return fail("लगइन हुन सकेन।");
        String found = displayName(sent.html);
        mark(found.isEmpty() ? id : found, id.trim());
        return new Result(true, true, "लगइन भयो।", name());
    }

    static Result register(String person, String phone, String email, String password) {
        Page page = get(PAGE + "?mode=register");
        if (page.html == null) return fail("दर्ता पृष्ठ खुलेन। नेट जाँच गर्नुहोस्।");
        String csrf = csrf(page.html);
        if (csrf.isEmpty()) return fail("दर्ता पृष्ठ खुलेन।");
        String body = form("csrf", csrf, "mode", "register", "next", "", "ref", "", "name", person, "phone", phone, "email", email, "password", password, "password_confirm", password);
        Page sent = post(PAGE + "?mode=register", body);
        if (sent.html == null) return fail("जवाफ आएन। नेट ढिलो छ।");
        String error = error(sent.html);
        if (!error.isEmpty()) return fail(error);
        if (registerForm(sent.html)) return fail("खाता बनेन। विवरण जाँच गर्नुहोस्।");
        mark(person.trim(), email.trim());
        return new Result(true, true, "खाता बन्यो।", person.trim());
    }

    static Result reset(String email) {
        Page page = get(RESET);
        if (page.html == null) return fail("रिसेट पृष्ठ खुलेन।");
        String csrf = csrf(page.html);
        if (csrf.isEmpty()) return fail("रिसेट पृष्ठ खुलेन।");
        Page sent = post(RESET, form("csrf", csrf, "action", "request", "email", email));
        if (sent.html == null) return fail("जवाफ आएन। नेट ढिलो छ।");
        String error = error(sent.html);
        if (!error.isEmpty()) return fail(error);
        return new Result(true, false, "यदि यो इमेल दर्ता छ भने रिसेट लिंक पठाइएको छ। Inbox र Spam जाँच गर्नुहोस्।", "");
    }

    static void logout() {
        get(PAGE + "?mode=logout");
        if (prefs() != null) prefs().edit().clear().apply();
    }

    private static Result fail(String message) {
        return new Result(false, false, message, "");
    }

    private static void mark(String person, String contact) {
        if (prefs() == null) return;
        prefs().edit().putBoolean("logged", true).putString("name", person == null ? "" : person).putString("contact", contact == null ? "" : contact).apply();
    }

    private static void clearUser() {
        if (prefs() == null) return;
        prefs().edit().putBoolean("logged", false).remove("name").remove("contact").apply();
    }

    private static boolean loginForm(String html) {
        Document doc = Jsoup.parse(html);
        String title = doc.title() == null ? "" : doc.title();
        if (!title.contains("लग इन") && !title.contains("लगइन")) return false;
        return doc.selectFirst("input[name=mode][value=login]") != null;
    }

    private static boolean registerForm(String html) {
        String title = Jsoup.parse(html).title();
        return title != null && title.contains("खाता बनाउनुहोस्");
    }

    private static String error(String html) {
        Document doc = Jsoup.parse(html);
        Element box = doc.selectFirst(".ko-error");
        return box == null ? "" : box.text().replaceAll("\\s+", " ").trim();
    }

    private static String csrf(String html) {
        Document doc = Jsoup.parse(html);
        Element input = doc.selectFirst("input[name=csrf]");
        return input == null ? "" : input.attr("value");
    }

    private static String displayName(String html) {
        Document doc = Jsoup.parse(html);
        for (String selector : new String[]{".ko-user-name", ".profile-name", ".account-name", ".member-name"}) {
            Element node = doc.selectFirst(selector);
            if (node != null) {
                String text = node.text().replaceAll("\\s+", " ").trim();
                if (usable(text)) return text;
            }
        }
        java.util.regex.Matcher hello = java.util.regex.Pattern.compile("(?:नमस्कार|स्वागतम|स्वागत)[,\\s]+([^।<]{2,40})").matcher(html);
        if (hello.find() && usable(hello.group(1))) return hello.group(1).trim();
        return "";
    }

    private static boolean usable(String text) {
        if (text == null || text.length() < 2 || text.length() > 60) return false;
        return !text.contains("लगइन") && !text.contains("लग इन") && !text.contains("खाता") && !text.contains("पासवर्ड");
    }

    private static Page get(String url) {
        Page page = open("GET", url, null, 30000);
        return page.html == null ? open("GET", url, null, 60000) : page;
    }

    private static Page post(String url, String body) {
        Page page = open("POST", url, body, 35000);
        return page.html == null ? open("POST", url, body, 70000) : page;
    }

    private static Page open(String method, String url, String body, int timeout) {
        String current = url;
        String verb = method;
        String send = body;
        for (int hop = 0; hop < 5; hop++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(current).openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(timeout);
                connection.setReadTimeout(timeout);
                connection.setRequestProperty("User-Agent", UA);
                connection.setRequestProperty("Accept-Language", "ne,en;q=0.8");
                connection.setRequestProperty("Referer", PAGE);
                apply(connection);
                if ("POST".equals(verb) && send != null) {
                    byte[] bytes = send.getBytes(StandardCharsets.UTF_8);
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    connection.setFixedLengthStreamingMode(bytes.length);
                    OutputStream output = connection.getOutputStream();
                    output.write(bytes);
                    output.close();
                }
                int code = connection.getResponseCode();
                capture(connection);
                if (code >= 300 && code < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location == null || location.isEmpty()) return new Page(null);
                    current = location.startsWith("http") ? location : ORIGIN + (location.startsWith("/") ? location : "/" + location);
                    verb = "GET";
                    send = null;
                    continue;
                }
                InputStream input = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
                return new Page(read(input));
            } catch (Exception ignored) {
                return new Page(null);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        return new Page(null);
    }

    private static String read(InputStream input) throws Exception {
        if (input == null) return "";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = input.read(buffer)) != -1) bytes.write(buffer, 0, n);
        input.close();
        return bytes.toString("UTF-8");
    }

    private static String form(String... pairs) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (body.length() > 0) body.append('&');
            body.append(encode(pairs[i])).append('=').append(encode(pairs[i + 1]));
        }
        return body.toString();
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String header() {
        LinkedHashMap<String, String> jar = load();
        StringBuilder cookie = new StringBuilder();
        for (Map.Entry<String, String> entry : jar.entrySet()) {
            if (cookie.length() > 0) cookie.append("; ");
            cookie.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return cookie.toString();
    }

    private static LinkedHashMap<String, String> load() {
        LinkedHashMap<String, String> jar = new LinkedHashMap<>();
        String raw = prefs() == null ? "" : prefs().getString("cookie", "");
        if (raw == null || raw.isEmpty()) return jar;
        for (String part : raw.split(";")) {
            int cut = part.indexOf('=');
            if (cut <= 0) continue;
            jar.put(part.substring(0, cut).trim(), part.substring(cut + 1).trim());
        }
        return jar;
    }

    private static void save(LinkedHashMap<String, String> jar) {
        if (prefs() == null) return;
        StringBuilder raw = new StringBuilder();
        for (Map.Entry<String, String> entry : jar.entrySet()) {
            if (raw.length() > 0) raw.append(';');
            raw.append(entry.getKey()).append('=').append(entry.getValue());
        }
        prefs().edit().putString("cookie", raw.toString()).apply();
    }

    private static SharedPreferences prefs() {
        return app == null ? null : app.getSharedPreferences("account", Context.MODE_PRIVATE);
    }

    private static final class Page {
        final String html;

        Page(String html) {
            this.html = html;
        }
    }
}
