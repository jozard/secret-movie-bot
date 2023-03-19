package com.jozard.secretmoviebot.commands;

import com.jozard.secretmoviebot.Utils;
import com.jozard.secretmoviebot.users.UserService;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.MessageFormat;

public abstract class GroupCommand extends BotCommand {

    protected final UserService userService;

    public GroupCommand(UserService userService, String name, String description) {
        super(name, description);
        this.userService = userService;
    }

    protected abstract void doExecute(AbsSender absSender, User user, Chat chat, String[] strings);

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        if (Utils.isGroup(chat)) {
            if (!userService.pitchRegistered(chat.getId())) {
                throw new IllegalArgumentException(
                        MessageFormat.format("Pitch is not registered in chat {0}", chat.getId()));
            } else {
                doExecute(absSender, user, chat, strings);
            }
        }
    }
}
