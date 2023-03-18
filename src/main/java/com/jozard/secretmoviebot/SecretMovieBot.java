package com.jozard.secretmoviebot;

import com.jozard.secretmoviebot.actions.OnGroupSent;
import com.jozard.secretmoviebot.actions.OnJoin;
import com.jozard.secretmoviebot.actions.OnMovieSent;
import com.jozard.secretmoviebot.actions.OnVoteSent;
import com.jozard.secretmoviebot.commands.*;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScope;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllGroupChats;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllPrivateChats;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class SecretMovieBot extends TelegramLongPollingCommandBot {

    private final UserService userService;
    private final StickerService stickerService;

    private final OnJoin onJoin;
    private final OnGroupSent onGroupSent;
    private final OnMovieSent onMovieSent;

    private final OnVoteSent onVoteSent;

    public SecretMovieBot(UserService userService, StickerService stickerService, Start start, Stop stop, Join join, Vote vote, CreateRandom createRandom, CreateSimpleVote createSimpleVote, CreateBalancedVote createBalancedVote, OnJoin onJoin, OnGroupSent onGroupSent, OnMovieSent onMovieSent, OnVoteSent onVoteSent) throws TelegramApiException {
        super(new DefaultBotOptions(), true);
        this.userService = userService;
        this.stickerService = stickerService;
        this.onJoin = onJoin;
        this.onGroupSent = onGroupSent;
        this.onMovieSent = onMovieSent;
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
    public void processNonCommandUpdate(Update update) {

        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (Utils.isUser(message.getChat())) {
                // private message from the user. Let's process it.
                var user = message.getFrom();
                System.out.println(
                        MessageFormat.format("Message from {0}: {1}", user.getUserName(), message.getText()));
                List<UserService.Group> userGroups = userService.getAllGroups(user);
                if (userGroups.size() == 0) {
                    System.out.println(
                            MessageFormat.format("User {0} has not joined any group pitch. Ignore messages from them",
                                    user.getUserName()));
                    return;
                }
                Optional<PitchStateMachine> userState = userService.getPitching(user);
                if (userState.isEmpty()) {
                    System.out.println(
                            MessageFormat.format("User {0} has not started any pitching yet. Ignore this message",
                                    user.getUserName()));
                    return;
                }
                if (userState.get().isPendingStart()) {
                    System.out.println(MessageFormat.format(
                            "User {0} has not run start command in private chat. Ignore this message",
                            user.getUserName()));
                } else if (userState.get().isPendingCurrentGroup()) {
                    this.onGroupSent.execute(this, userState.get(), message, null);

                } else if (userState.get().isPendingMovie()) {
                    this.onMovieSent.execute(this, userState.get(), message, null);
                } else if (userState.get().isPendingVoteStart()) {
                    System.out.println(MessageFormat.format("User {0} is in pending vote state. Ignore this message",
                            user.getUserName()));
                } else if (userState.get().isPendingSimpleVote()) {
                    this.onVoteSent.execute(this, userState.get(), message, null);
                } else {
                    //the user is done
                    System.out.println(
                            MessageFormat.format("User {0} is in DONE state. Ignore this message", user.getUserName()));
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
                this.onJoin.execute(this, user, chatId, new String[]{callbackQuery.getId()});
            }

        }
    }

    @Override
    public String getBotToken() {
            return ""; // use your bot token here
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    @Override
    public String getBaseUrl() {
        return super.getBaseUrl();
    }

    @Override
    public void clearWebhook() throws TelegramApiRequestException {

    }


}
