package com.kalaiyaonline.news;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class Net {
    static volatile int lastKbps;
    private static File folder;

    private Net() {}

    static void use(File cache) {
        folder = new File(cache, "pages");
        if (!folder.exists()) folder.mkdirs();
        File[] old = folder.listFiles();
        if (old == null) return;
        for (File file : old) {
            String name = file.getName();
            if (name.endsWith(".html") && name.length() < 20) file.delete();
        }
    }

    static String text(String url) {
        return text(url, "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36");
    }

    static String text(String url, String agent) {
        if (url == null || url.isEmpty()) return null;
        File file = fileFor(url);
        boolean saved = file != null && file.exists() && file.length() > 40;
        int tries = saved ? 2 : 3;
        int[] connect = {20000, 30000, 40000};
        int[] read = {40000, 60000, 90000};
        for (int attempt = 0; attempt < tries; attempt++) {
            byte[] data = fetch(url, agent, connect[attempt], read[attempt]);
            if (data == null || data.length < 40) continue;
            if (file != null) write(file, data);
            return new String(data, StandardCharsets.UTF_8);
        }
        if (!saved) return null;
        byte[] cached = read(file);
        return cached == null ? null : new String(cached, StandardCharsets.UTF_8);
    }

    private static byte[] fetch(String url, String agent, int connect, int readTimeout) {
        HttpURLConnection connection = null;
        long start = System.nanoTime();
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(connect);
            connection.setReadTimeout(readTimeout);
            connection.setRequestProperty("User-Agent", agent);
            connection.setRequestProperty("Accept-Language", "ne,en;q=0.8");
            Account.apply(connection);
            int code = connection.getResponseCode();
            Account.capture(connection);
            if (code >= 400) return null;
            InputStream input = connection.getInputStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) bytes.write(buffer, 0, read);
            input.close();
            long ms = Math.max(1L, (System.nanoTime() - start) / 1_000_000L);
            lastKbps = (int) Math.min(Integer.MAX_VALUE, (bytes.size() * 8L) / ms);
            return bytes.toByteArray();
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static File fileFor(String url) {
        if (folder == null) return null;
        String key = url + "\n" + (Account.loggedIn() ? "1" : "0");
        return new File(folder, digest(key) + ".html");
    }

    private static String digest(String key) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(32);
            for (int i = 0; i < 16; i++) {
                int value = hash[i] & 0xff;
                if (value < 16) out.append('0');
                out.append(Integer.toHexString(value));
            }
            return out.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(key.hashCode());
        }
    }

    private static byte[] read(File file) {
        try (InputStream input = new FileInputStream(file)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = input.read(buffer)) != -1) bytes.write(buffer, 0, n);
            return bytes.toByteArray();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void write(File file, byte[] data) {
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temp)) {
            output.write(data);
            output.getFD().sync();
        } catch (Exception ignored) {
            temp.delete();
            return;
        }
        if (!temp.renameTo(file)) {
            temp.delete();
        }
    }
}
