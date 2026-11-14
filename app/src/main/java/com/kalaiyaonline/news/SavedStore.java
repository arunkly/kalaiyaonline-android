package com.kalaiyaonline.news;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class SavedStore {
    private static final String FILE = "article";
    private static final String LIST = "saved-list";

    private SavedStore() {}

    static boolean isSaved(Context context, String slug) {
        return prefs(context).getBoolean("saved:" + slug, false);
    }

    static void set(Context context, Story story, boolean saved) {
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putBoolean("saved:" + story.slug, saved);
        JSONArray next = new JSONArray();
        if (saved) next.put(json(story));
        for (Story item : list(context)) {
            if (!item.slug.equals(story.slug)) next.put(json(item));
        }
        editor.putString(LIST, next.toString());
        editor.apply();
    }

    static List<Story> list(Context context) {
        List<Story> stories = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs(context).getString(LIST, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                String slug = item.optString("slug");
                if (slug.isEmpty() || !isSaved(context, slug)) continue;
                stories.add(new Story(slug, item.optString("title"), item.optString("image"), item.optString("category"), item.optString("date"), item.optString("author"), ""));
            }
        } catch (Exception ignored) {
        }
        return stories;
    }

    private static JSONObject json(Story story) {
        JSONObject item = new JSONObject();
        try {
            item.put("slug", story.slug);
            item.put("title", story.title);
            item.put("image", story.image);
            item.put("category", story.category);
            item.put("date", story.date);
            item.put("author", story.author);
        } catch (Exception ignored) {
        }
        return item;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
