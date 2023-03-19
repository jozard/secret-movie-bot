package com.jozard.secretmoviebot.actions;

import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.function.Consumer;

abstract public class PrivateChatAction {

    protected final MessageService messageService;
    protected final UserService userService;

    protected PrivateChatAction(MessageService messageService, UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }

    public abstract void execute(AbsSender absSender, PitchStateMachine state, Message message, String[] arguments);

    protected void reply(AbsSender absSender, Long chatId, Consumer<SendMessage> message) {
        SendMessage response = new SendMessage();
        response.setChatId(String.valueOf(chatId));
        response.setParseMode(ParseMode.MARKDOWN);
        message.accept(response);
        try {
            absSender.execute(response);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
