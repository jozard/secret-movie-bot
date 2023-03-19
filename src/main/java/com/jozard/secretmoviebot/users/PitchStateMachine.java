package com.jozard.secretmoviebot.users;

import org.telegram.telegrambots.meta.api.objects.User;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class PitchStateMachine {
    private final User user;

    private final Set<Long> pendingStartGroups = new HashSet<>();

    private final Set<Movie> votes = new HashSet<>();
    private Movie movie;
    private UserState currentState = UserState.PENDING_START;

    private Long currentGroup;

    public PitchStateMachine(User user) {
        this.user = user;
    }

    public User user() {
        return user;
    }

    public boolean isPendingMovie() {
        return this.currentState == UserState.PENDING_MOVIE_NAME;
    }

    public boolean hasPendingStartGroups() {
        return this.pendingStartGroups.size() > 0;
    }

    public void selectGroup(long chatId) {
        this.currentState = UserState.PENDING_MOVIE_NAME;
        movie = null;
        votes.clear();
        currentGroup = chatId;
        pendingStartGroups.remove(currentGroup);
    }

    public void start() {
        this.currentState = UserState.PENDING_CURRENT_GROUP;
    }

    public void pendingVote() {
        this.currentState = UserState.PENDING_VOTE;
    }

    public void pendingVoteStart() {
        this.currentState = UserState.PENDING_VOTE_START;
    }

    public Movie setMovie(String title) {
        this.movie = new Movie(title, user);
        this.currentState = UserState.MOVIE_NAME_SET;
        return this.movie;
    }

    public void done() {
        this.currentState = UserState.PENDING_START;
    }

    public Set<Movie> getVotes() {
        return votes;
    }

    public void join(Long chatId) {
        if (pendingStartGroups.contains(chatId)) {
            throw new IllegalArgumentException("Pitched user state with chatId = " + chatId + " is already created");
        }
        pendingStartGroups.add(chatId);
    }

    public User getUser() {
        return user;
    }

    public Set<Long> getPendingStartGroups() {
        return pendingStartGroups;
    }

    public UserState getCurrentState() {
        return currentState;
    }

    public Optional<Long> getCurrentGroup() {
        return Optional.ofNullable(currentGroup);
    }

    public boolean isPendingStart() {
        return this.currentState == UserState.PENDING_START;
    }

    public boolean isPendingCurrentGroup() {
        return this.currentState == UserState.PENDING_CURRENT_GROUP;
    }

    public boolean isPendingSimpleVote() {
        return this.currentState == UserState.PENDING_VOTE;
    }

    public boolean isPendingVoteStart() {
        return this.currentState == UserState.PENDING_VOTE_START;
    }

    public enum UserState {
        PENDING_START,
        PENDING_CURRENT_GROUP,
        PENDING_MOVIE_NAME,
        MOVIE_NAME_SET,
        PENDING_VOTE,
        PENDING_VOTE_START,
    }
}
