package com.jozard.secretmoviebot.actions;

import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.MessageFormat;

@Component
public class RequestMovie extends Action {

    private final UserService userService;
    private final StickerService stickerService;

    private final MessageService messageService;

    public RequestMovie(UserService userService, StickerService stickerService, MessageService messageService) {
        this.userService = userService;
        this.stickerService = stickerService;
        this.messageService = messageService;
    }

    @Override
    public void execute(AbsSender absSender, User user, long chatId, String[] arguments) {
        ReplyKeyboardRemove keyboardRemove = ReplyKeyboardRemove.builder().removeKeyboard(true).build();
        messageService.send(absSender, chatId,
                MessageFormat.format("Send me a movie title for the *{0}* group.", arguments[0]), keyboardRemove);


    }

}
