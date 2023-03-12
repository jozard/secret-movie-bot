package com.jozard.secretmoviebot.actions;

import com.jozard.secretmoviebot.users.PitchStateMachine;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.function.Consumer;

abstract public class PrivateChatAction {
    public abstract void execute(AbsSender absSender, PitchStateMachine state, Message message, String[] arguments);

    protected void reply(AbsSender absSender, Long chatId, Consumer<SendMessage> respond) {
        SendMessage response = new SendMessage();
        response.setChatId(String.valueOf(chatId));
        respond.accept(response);
        try {
            absSender.execute(response);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
