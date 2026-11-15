package com.kalaiyaonline.news;

import java.util.ArrayList;
import java.util.List;

public final class Story {
    public final String slug;
    public final String title;
    public final String image;
    public final String category;
    public final String date;
    public final String author;
    public final String body;
    public String views = "";
    public String authorImage = "";
    public final List<Story> related = new ArrayList<>();
    public final List<String> tags = new ArrayList<>();
    public final List<String> tagUrls = new ArrayList<>();
    public final int[] feelings = new int[6];
    public final String[] feelingUrls = new String[]{"", "", "", "", "", ""};
    public String feelingNote = "";

    public Story(String slug, String title, String image, String category, String date, String author, String body) {
        this.slug = slug;
        this.title = title;
        this.image = image == null ? "" : image;
        this.category = category == null ? "" : category;
        this.date = date == null ? "" : date;
        this.author = author == null ? "" : author;
        this.body = body == null ? "" : body;
    }
}
