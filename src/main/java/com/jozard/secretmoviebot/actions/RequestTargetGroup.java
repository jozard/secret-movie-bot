package com.jozard.secretmoviebot.actions;

import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;

@Component
public class RequestTargetGroup extends Action {

    private final UserService userService;
    private final StickerService stickerService;

    private final MessageService messageService;

    public RequestTargetGroup(UserService userService, StickerService stickerService, MessageService messageService) {
        this.userService = userService;
        this.stickerService = stickerService;
        this.messageService = messageService;
    }

    @Override
    public void execute(AbsSender absSender, User user, long chatId, String[] arguments) {
        List<KeyboardButton> groupButtons = userService.getGroupsAvailableToStartPitch(user).stream().map(
                group -> KeyboardButton.builder().text(group.getChatName()).build()).toList();
        if (groupButtons.isEmpty()) {
            messageService.send(absSender, chatId,
                    "Cannot start. Either you are done with all pitches, a bot is waiting for a movie title from you, or no pitch is created in your groups.");
        } else {
            userService.getPitching(user).orElseThrow().start();
            List<KeyboardRow> keyboardRows = groupButtons.stream().map(button -> {
                KeyboardRow row = new KeyboardRow();
                row.add(button);
                return row;
            }).toList();
            ReplyKeyboardMarkup replyKeyboardMarkup = ReplyKeyboardMarkup.builder().keyboard(
                    keyboardRows).resizeKeyboard(true).oneTimeKeyboard(false).build();
            messageService.send(absSender, chatId, "Send me a group to choose movies. You can use the button(s) below.",
                    replyKeyboardMarkup);
        }

    }

}
