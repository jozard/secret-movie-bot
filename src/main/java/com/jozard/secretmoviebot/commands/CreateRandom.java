package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.users.UserService;
import com.jozard.secretmoviebot.StickerService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
public class CreateRandom extends Create {

    public static final String NAME = "create_random";
    public static final String DESCRIPTION = "Open random movie choosing for the users joined from this group";

    public CreateRandom(UserService userService, StickerService stickerService) {
        super(NAME, DESCRIPTION, userService, stickerService);
    }


    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {


        if (chat.isGroupChat()) {
            super.execute(absSender, user, chat, strings);
            userService.getGroup(chat.getId()).ifPresent(UserService.Group::setRandom);
        }


    }
}