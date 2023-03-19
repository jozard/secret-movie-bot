package com.jozard.secretmoviebot.jobs;

import com.jozard.secretmoviebot.users.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class GroupCleanup implements Runnable {

    private final Long groupId;
    private final UserService userService;
    private final AbsSender absSender;

    public GroupCleanup(Long groupId, UserService userService, AbsSender absSender) {

        this.groupId = groupId;
        this.userService = userService;
        this.absSender = absSender;
    }

    @Override
    public void run() {
        boolean groupExist = userService.groupExist(groupId);
        userService.remove(groupId);
        if (groupExist) {
            SendMessage response = new SendMessage();
            response.setChatId(String.valueOf(groupId));
            response.setText("Registered movie choosing has timed out. Create a new one");
            try {
                absSender.execute(response);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}
