package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.users.UserService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

@Component
public class Stop extends BotCommand {

    public static final String NAME = "stop";
    public static final String DESCRIPTION = """
            With this command you can reset movie choosing for this group.
            After this command you will be able to start again movie choosing in this group""";
    private final UserService userService;
    private final ThreadPoolTaskScheduler scheduler;

    public Stop(UserService userService, ThreadPoolTaskScheduler scheduler) {
        super(NAME, DESCRIPTION);
        this.userService = userService;
        this.scheduler = scheduler;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        long chatId = chat.getId();
        if (chat.isGroupChat()) {
            // we are in a group
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
        } else {
            // private chat. Do nothing here yet.
        }


    }
}