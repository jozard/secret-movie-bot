package com.jozard.secretmoviebot.actions;

import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

abstract public class Action {
    public abstract void execute(AbsSender absSender, User user, long chatId, String[] arguments);
}
