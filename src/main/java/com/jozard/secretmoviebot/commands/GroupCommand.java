package com.jozard.secretmoviebot.commands;

import com.jozard.secretmoviebot.Utils;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

public abstract class GroupCommand extends BotCommand {

    public GroupCommand(String name, String description) {
        super(name, description);
    }

    public abstract void doExecute(AbsSender absSender, User user, Chat chat, String[] strings);

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        if (Utils.isGroup(chat)) {
            doExecute(absSender, user, chat, strings);
        }
    }
}
