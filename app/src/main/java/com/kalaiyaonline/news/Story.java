package com.kalaiyaonline.news;

public final class Story {
    public final String slug;
    public final String title;
    public final String image;
    public final String category;
    public final String date;
    public final String author;
    public final String body;

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
