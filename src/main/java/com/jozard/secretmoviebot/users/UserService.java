package com.jozard.secretmoviebot.users;


import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Service
public class UserService {
    // chat_id -> group
    private final Map<Long, Group> groups = new HashMap<>();

    private final List<PitchStateMachine> pitchStateMachines = new ArrayList<>();


    public final List<JoinedUser> getUsers(long chatId) {
        if (groups.containsKey(chatId)) {
            return new ArrayList<>(groups.get(chatId).users);
        }
        return new ArrayList<>();
    }

    public final Optional<PitchStateMachine> getPitching(User user) {
        return pitchStateMachines.stream().filter(item -> item.user().equals(user)).findFirst();
    }

    public final List<PitchStateMachine> getStates(Long chatId) {
        return pitchStateMachines.stream().filter(
                state -> state.getCurrentGroup().isPresent() && state.getCurrentGroup().get().equals(chatId)).collect(
                Collectors.toList());
    }

    public final Optional<PitchStateMachine> getPitching(User user, Long chatId) {
        return pitchStateMachines.stream().filter(item -> item.user().equals(
                user) && item.getCurrentGroup().isPresent() && item.getCurrentGroup().get().equals(chatId)).findFirst();
    }

    public final boolean pitchRegistered(long chatId) {
        return groups.containsKey(chatId);
    }

    public final Group start(long chatId, String chatName, User admin) {
        Optional<User> existingUser = findAdmin(admin.getId());
        if (existingUser.isPresent()) {
            throw new AdminExistsException(
                    MessageFormat.format("{1} is already leading a pitch in the chat {0}", chatId,
                            admin.getFirstName()));
        }
        if (groups.containsKey(chatId)) {
            throw new PitchExistsException(MessageFormat.format("Pitch is already started in {0}", chatName));
        }
        return add(chatId, chatName, admin);
    }

    public final void add(long chatId, User user) {
        if (!groups.containsKey(chatId)) {
            throw new IllegalArgumentException(MessageFormat.format("Pitch is not registered in chat {0}", chatId));
        }
        groups.get(chatId).add(user);
    }

    public final void remove(long chatId) {
        groups.remove(chatId);
        pitchStateMachines.forEach(state -> state.getPendingStartGroups().remove(chatId));
        while (pitchStateMachines.stream().anyMatch(
                stateMachine -> stateMachine.getCurrentGroup().isPresent() && stateMachine.getCurrentGroup().get().equals(
                        chatId))) {
            PitchStateMachine removeCandidate = pitchStateMachines.stream().filter(
                    state -> state.getCurrentGroup().isPresent() && state.getCurrentGroup().get().equals(
                            chatId)).findFirst().orElseThrow();
            pitchStateMachines.remove(removeCandidate);
        }
    }

    public boolean groupExist(long chatId) {
        return groups.containsKey(chatId);
    }

    private Group add(long chatId, String chatName, User admin) {
        Group group = new Group(chatId, chatName);
        group.add(admin);
        groups.put(chatId, group);
        return group;
    }

    public boolean joined(User user, long chatId) {
        if (!groups.containsKey(chatId)) {
            return false;
        }
        return groups.get(chatId).has(user);
    }

    public Optional<JoinedUser> getFromGroup(User user, long chatId) {
        if (!joined(user, chatId)) {
            return Optional.empty();
        }
        return groups.get(chatId).users.stream().filter(item -> item.user().getId().equals(user.getId())).findFirst();
    }

    public List<Group> getAllGroups(User user) {
        return groups.values().stream().filter(group -> group.has(user)).toList();
    }

    public List<Group> getGroupsAvailableToStartPitch(User user) {
        List<Long> userPendingStartGroups = getPitching(user).stream().flatMap(
                pitchStateMachine -> pitchStateMachine.getPendingStartGroups().stream()).toList();
        return groups.values().stream().filter(
                group -> group.pitchCreated() && userPendingStartGroups.contains(group.chatId)).toList();
    }

    public Optional<Group> getGroup(JoinedUser joinedUser) {
        return groups.values().stream().filter(group -> group.has(joinedUser)).findFirst();
    }

    public Optional<User> findAdmin(long id) {
        return groups.values().stream().map(group -> group.users).flatMap(Collection::stream).filter(
                JoinedUser::isAdmin).map(JoinedUser::user).filter(joined -> joined.getId().equals(id)).findFirst();
    }

    public Optional<Group> getGroup(Long id) {
        return Optional.ofNullable(groups.get(id));
    }

    public enum PitchType {
        RANDOM, SIMPLE_VOTE, BALANCED_VOTE
    }

    public class Group {
        private final long chatId;

        private final String chatName;
        private final List<JoinedUser> users = new ArrayList<>();
        private final Set<Movie> movies = new HashSet<>();

        private final List<VoteResult> votes = new ArrayList<>();

        private PitchType pitchType;

        private ScheduledFuture<?> cleanupTask;

        public Group(long chatId, String chatName) {
            this.chatId = chatId;
            this.chatName = chatName;
        }

        public ScheduledFuture<?> getCleanupTask() {
            return cleanupTask;
        }

        public void setCleanupTask(ScheduledFuture<?> cleanupTask) {
            this.cleanupTask = cleanupTask;
        }

        public void setRandom() {
            this.pitchType = PitchType.RANDOM;
        }

        public void setSimpleVote() {
            this.pitchType = PitchType.SIMPLE_VOTE;
        }

        public void setBalancedVote() {
            this.pitchType = PitchType.BALANCED_VOTE;
        }

        public PitchType getPitchType() {
            return pitchType;
        }

        public boolean pitchCreated() {
            return pitchType != null;
        }

        public void add(User user) {
            if (users.size() == 0) {
                this.users.add(new JoinedUser(user, true, chatId));
            } else {
                if (!this.has(user)) {
                    this.users.add(new JoinedUser(user, false, chatId));
                }
            }
            PitchStateMachine stateMachine;

            Optional<PitchStateMachine> userPitching = getPitching(user);
            if (userPitching.isEmpty()) {
                stateMachine = new PitchStateMachine(user);
                pitchStateMachines.add(stateMachine);
            } else {
                stateMachine = userPitching.get();
            }
            stateMachine.join(chatId);
        }

        private boolean has(User user) {
            return this.users.stream().anyMatch(item -> item.user().getId().equals(user.getId()));
        }

        private boolean has(JoinedUser user) {
            return this.users.stream().anyMatch(item -> item.equals(user));
        }

        public boolean isAllMoviesSelected() {
            return movies.size() == users.size();
        }

        public Optional<User> getAdmin() {
            return users.stream().filter(JoinedUser::isAdmin).map(JoinedUser::user).findFirst();
        }

        public long getChatId() {
            return chatId;
        }

        public String getChatName() {
            return chatName;
        }

        public void addMovie(Movie movie) {
            this.movies.add(movie);
        }

        public Set<Movie> getMovies() {
            return movies;
        }

        public List<JoinedUser> getUsers() {
            return users;
        }

        public void addVote(Movie movie, User user) {
            Optional<VoteResult> voteResult = votes.stream().filter(item -> item.movie.equals(movie)).findFirst();

            if (voteResult.isEmpty()) {
                VoteResult vote = new VoteResult(movie);
                vote.add(user);
                votes.add(vote);
            } else {
                voteResult.get().add(user);
            }
        }

        public List<VoteResult> getVotes() {
            return votes;
        }

        public class VoteResult {
            private final Movie movie;
            private final Set<User> voted = new HashSet<>();


            VoteResult(Movie movie) {
                this.movie = movie;
            }

            public void add(User user) {
                voted.add(user);
            }

            int getVoteCount() {
                return voted.size();
            }

            public Set<User> getVoted() {
                return voted;
            }

            public Movie getMovie() {
                return movie;
            }
        }
    }
}
