package com.jozard.secretmoviebot.users;

import org.telegram.telegrambots.meta.api.objects.User;

public class Movie {
    private final String title;
    private final User owner;
    private String description = "";


    public Movie(String title, User owner) {
        this.title = title;
        this.owner = owner;
    }

    public String getTitle() {
        return title;
    }

    public User getOwner() {
        return owner;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
