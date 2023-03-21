package com.jozard.secretmoviebot;

import com.jozard.secretmoviebot.actions.JoinUser;
import com.jozard.secretmoviebot.commands.*;
import com.jozard.secretmoviebot.listeners.OnDescriptionSent;
import com.jozard.secretmoviebot.listeners.OnGroupSent;
import com.jozard.secretmoviebot.listeners.OnMovieSent;
import com.jozard.secretmoviebot.listeners.OnVoteSent;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberBanned;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberMember;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScope;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllGroupChats;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllPrivateChats;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class SecretMovieBot extends TelegramLongPollingCommandBot {

    private final UserService userService;
    private final StickerService stickerService;

    private final JoinUser joinUser;
    private final OnGroupSent onGroupSent;
    private final OnMovieSent onMovieSent;

    private final OnVoteSent onVoteSent;
    private final OnDescriptionSent onDescriptionSent;

    public SecretMovieBot(UserService userService, StickerService stickerService, Start start, Stop stop, Join join, Vote vote, CreateRandom createRandom, CreateSimpleVote createSimpleVote, CreateBalancedVote createBalancedVote, ToggleDescription toggleDescription, JoinUser joinUser, OnGroupSent onGroupSent, OnMovieSent onMovieSent, OnDescriptionSent onDescriptionSent, OnVoteSent onVoteSent) throws TelegramApiException {
        super(new DefaultBotOptions(), true);
        this.userService = userService;
        this.stickerService = stickerService;
        this.joinUser = joinUser;
        this.onGroupSent = onGroupSent;
        this.onMovieSent = onMovieSent;
        this.onDescriptionSent = onDescriptionSent;
        this.onVoteSent = onVoteSent;
        List<BotCommand> privateChatCommands = new ArrayList<>();
        List<BotCommand> groupChatCommands = new ArrayList<>();
        privateChatCommands.add(new BotCommand(Start.NAME, Start.DESCRIPTION));
        privateChatCommands.add(new BotCommand(Vote.NAME, Vote.DESCRIPTION));
        groupChatCommands.add(new BotCommand(Start.NAME, Start.DESCRIPTION));
        groupChatCommands.add(new BotCommand(Stop.NAME, Stop.DESCRIPTION));
        groupChatCommands.add(new BotCommand(Join.NAME, Join.DESCRIPTION));
        groupChatCommands.add(new BotCommand(CreateRandom.NAME, CreateRandom.DESCRIPTION));
        groupChatCommands.add(new BotCommand(CreateSimpleVote.NAME, CreateSimpleVote.DESCRIPTION));
        groupChatCommands.add(new BotCommand(CreateBalancedVote.NAME, CreateBalancedVote.DESCRIPTION));
        groupChatCommands.add(new BotCommand(ToggleDescription.NAME, ToggleDescription.DESCRIPTION));
        BotCommandScope privateChatsScope = new BotCommandScopeAllPrivateChats();
        BotCommandScope groupsScope = new BotCommandScopeAllGroupChats();
        this.execute(new SetMyCommands(privateChatCommands, privateChatsScope, "en"));
        this.execute(new SetMyCommands(groupChatCommands, groupsScope, "en"));
        this.register(start);
        this.register(stop);
        this.register(join);
        this.register(createRandom);
        this.register(createSimpleVote);
        this.register(createBalancedVote);
        this.register(toggleDescription);
        this.register(vote);
    }

    @Override
    public String getBotUsername() {
        return "secret_movie_bot";
    }

    @Override
    protected void processInvalidCommandUpdate(Update update) {
        super.processInvalidCommandUpdate(update);
        System.out.println(MessageFormat.format("Invalid command {0} from {0}", update.getMessage().getText(),
                update.getMessage().getFrom()));
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        updates.forEach(update -> {
            try {
                preprocessUpdate(update);
            } catch (Exception e) {
                System.out.println("Updated preprocess failed: " + e);
            }
        });
        super.onUpdatesReceived(updates);
    }

    private void preprocessUpdate(Update update) {
        if (update.hasChatJoinRequest()) {
            ChatJoinRequest chatJoinRequest = update.getChatJoinRequest();
            User user = chatJoinRequest.getUser();
            Chat chat = chatJoinRequest.getChat();
            System.out.println(
                    MessageFormat.format("User {0} wants to join chat {1}", user.getFirstName(), chat.getTitle()));
            return;
        }
        if (update.hasChatMember()) {
            ChatMemberUpdated memberUpdated = update.getChatMember();
            User oldUser = memberUpdated.getOldChatMember().getUser();
            String oldStatus = memberUpdated.getOldChatMember().getStatus();
            User newUser = memberUpdated.getNewChatMember().getUser();
            String newStatus = memberUpdated.getNewChatMember().getStatus();
            Chat chat = memberUpdated.getChat();
            System.out.println(MessageFormat.format("User {0} ({1}) changed. New user {2} ({3}) in chat {4}",
                    oldUser.getFirstName(), oldStatus, newUser.getFirstName(), newStatus,
                    chat.getFirstName() == null ? chat.getTitle() : chat.getFirstName()));
            return;
        }
        if (update.hasMyChatMember()) {
            // bot is blocked/unblocked by the user
            ChatMemberUpdated memberUpdated = update.getMyChatMember();
            User newUser = memberUpdated.getNewChatMember().getUser();
            String newStatus = memberUpdated.getNewChatMember().getStatus();
            // verify it is the movie bot
            if (newUser.getUserName().equals(getBotUsername())) {
                if (newStatus.equals(ChatMemberBanned.STATUS)) {
                    userService.deletePrivateChat(memberUpdated.getChat().getId());
                } else if (newStatus.equals(ChatMemberMember.STATUS)) {
                    // unfortunately we have no info about the user here
                }
            }
            System.out.println(
                    MessageFormat.format("MyChatMember. New user {0} ({1}) in chat {2}", newUser.getUserName(),
                            newStatus, memberUpdated.getChat().getId()));
            return;
        }
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (Utils.isUser(message.getChat())) {
                // private message from the user. Let's process it.
                var user = message.getFrom();
                userService.updatePrivateChat(user, message.getChat());
            }
        }
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (Utils.isUser(message.getChat())) {
                // private message from the user. Let's process it.
                var user = message.getFrom();
                System.out.println(
                        MessageFormat.format("Message from {0}: {1}", user.getFirstName(), message.getText()));
                List<UserService.Group> userGroups = userService.getAllGroups(user);
                if (userGroups.size() == 0) {
                    System.out.println(MessageFormat.format(
                            "User {0}({1}) has not joined any group pitch. Ignore messages from them", user.getId(),
                            user.getFirstName()));
                    return;
                }
                Optional<PitchStateMachine> userState = userService.getPitching(user);
                if (userState.isEmpty()) {
                    System.out.println(
                            MessageFormat.format("User {0}({1})  has not started any pitching yet. Ignore this message",
                                    user.getId(), user.getFirstName()));
                    return;
                }
                if (userState.get().isPendingStart()) {
                    System.out.println(MessageFormat.format(
                            "User {0}({1})  has not run start command in private chat. Ignore this message",
                            user.getId(), user.getFirstName()));
                } else if (userState.get().isPendingCurrentGroup()) {
                    this.onGroupSent.execute(this, userState.get(), message, null);

                } else if (userState.get().isPendingMovie()) {
                    this.onMovieSent.execute(this, userState.get(), message, null);
                } else if (userState.get().isPendingDescription()) {
                    this.onDescriptionSent.execute(this, userState.get(), message, null);
                } else if (userState.get().isPendingVoteStart()) {
                    System.out.println(MessageFormat.format("User {0} is in pending vote state. Ignore this message",
                            user.getFirstName()));
                } else if (userState.get().isPendingVote()) {
                    this.onVoteSent.execute(this, userState.get(), message, null);
                } else {
                    //the user is done
                    System.out.println(MessageFormat.format("User {0} is in DONE state. Ignore this message",
                            user.getFirstName()));
                }
            } else {
                // ignore messages in group chats
            }
        } else if (update.hasCallbackQuery()) {
            // User clicked a button
            CallbackQuery callbackQuery = update.getCallbackQuery();
            if (callbackQuery.getData().equals("btn_join")) {
                User user = callbackQuery.getFrom();
                Long chatId = callbackQuery.getMessage().getChatId();
                this.joinUser.execute(this, user, chatId, new String[]{callbackQuery.getId()});
            }

        }
    }

    @Override
    public String getBotToken() {
        return System.getenv("SECRET_MOVIE_BOT_TOKEN");
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    @Override
    public String getBaseUrl() {
        return super.getBaseUrl();
    }

}
