package com.jozard.secretmoviebot.listeners;

import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.Utils;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

abstract public class PrivateChatListener {

    protected final MessageService messageService;
    protected final UserService userService;

    protected PrivateChatListener(MessageService messageService, UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }

    public abstract void doExecute(AbsSender absSender, PitchStateMachine state, Message message, String[] arguments);

    public void execute(AbsSender absSender, PitchStateMachine state, Message message, String[] strings) {
        if (Utils.isUser(message.getChat())) {
            doExecute(absSender, state, message, strings);
        }
    }
}
