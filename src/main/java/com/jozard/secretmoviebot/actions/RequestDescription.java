package com.jozard.secretmoviebot.actions;

import com.jozard.secretmoviebot.MessageService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.MessageFormat;

@Component
public class RequestDescription extends Action {

    private final MessageService messageService;

    public RequestDescription(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void execute(AbsSender absSender, User user, long chatId, String[] arguments) {
        ReplyKeyboardRemove keyboardRemove = ReplyKeyboardRemove.builder().removeKeyboard(true).build();
        messageService.send(absSender, chatId, MessageFormat.format("""
                Send me a message with description for the *{0}* movie.
                You can add year, director, short summary, etc.""", arguments[0]), keyboardRemove);


    }

}
