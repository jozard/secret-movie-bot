package com.jozard.secretmoviebot.users;

import org.telegram.telegrambots.meta.api.objects.User;

import java.time.Instant;
import java.util.Objects;

public class JoinedUser {
    private final User user;
    private final Instant joined;
    private final boolean isAdmin;

    private final long chatId;

    public JoinedUser(User user, boolean isAdmin, long chatId) {
        this.user = user;
        this.isAdmin = isAdmin;
        this.chatId = chatId;
        this.joined = Instant.now();
    }

    public User user() {
        return user;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public long chatId() {
        return chatId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JoinedUser that = (JoinedUser) o;
        return isAdmin == that.isAdmin && chatId == that.chatId && user.equals(that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, isAdmin, chatId);
    }
}
