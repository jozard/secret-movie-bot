package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

@Component
public class Stop extends AdminCommand {

    public static final String NAME = "stop";
    public static final String DESCRIPTION = """
            With this command you can reset movie choosing for this group.
            After this command you will be able to start again movie choosing in this group""";
    
    public Stop(MessageService messageService, UserService userService) {
        super(userService, messageService, NAME, DESCRIPTION);
    }

    @Override
    public void onCommandAction(AbsSender absSender, Chat chat, User user) {
        var chatId = chat.getId();
        if (userService.pitchRegistered(chatId)) {
            // first time start command called for this chat
            Optional<UserService.Group> group = userService.getGroup(chatId);
            group.ifPresent(value -> value.getCleanupTask().cancel(true));
            userService.remove(chatId);

            SendMessage response = new SendMessage();
            response.setChatId(String.valueOf(chatId));
            response.setText("Current pitch removed. You can start one again.");
            try {
                absSender.execute(response);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

}