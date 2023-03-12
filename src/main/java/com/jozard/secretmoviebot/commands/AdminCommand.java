package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.users.UserService;
import com.jozard.secretmoviebot.users.JoinedUser;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.ws.rs.ForbiddenException;
import java.text.MessageFormat;
import java.util.Optional;

public abstract class AdminCommand extends BotCommand {

    protected final UserService userService;

    public AdminCommand(UserService userService, String name, String description) {
        super(name, description);
        this.userService = userService;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {

        long chatId = chat.getId();
        if (chat.isGroupChat()) {
            // we are in a group
            if (!userService.pitchRegistered(chatId)) {
                throw new IllegalArgumentException(MessageFormat.format("Pitch is not registered in chat {0}", chatId));
            } else {
                Optional<JoinedUser> joinedUser = userService.getFromGroup(user, chatId);
                if (joinedUser.map(JoinedUser::isAdmin).isEmpty()) {
                    // only admin can call the command
                    SendMessage response = new SendMessage();
                    response.setChatId(String.valueOf(chatId));
                    response.setText("Pitch admin only can call this command");
                    try {
                        absSender.execute(response);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    throw new ForbiddenException(
                            MessageFormat.format("Pitch admin only can call {0}", this.getClass()));
                }
            }

        }
    }
}