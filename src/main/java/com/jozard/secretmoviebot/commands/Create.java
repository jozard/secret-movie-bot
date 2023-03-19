package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.actions.RequestTargetGroup;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class Create extends AdminCommand {

    protected final StickerService stickerService;
    private final RequestTargetGroup requestTargetGroup;
    private final UserService.PitchType pitchType;

    public Create(String name, String description, UserService.PitchType pitchType, UserService userService, MessageService messageService, StickerService stickerService, RequestTargetGroup requestTargetGroup) {
        super(userService, messageService, name, description);
        this.pitchType = pitchType;
        this.stickerService = stickerService;
        this.requestTargetGroup = requestTargetGroup;
    }

    @Override
    public void onCommandAction(AbsSender absSender, Chat chat, User user) {
        long chatId = chat.getId();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();

        System.out.println(MessageFormat.format("User {0} is trying to create a pitch in chat {1}",
                String.join(" ", user.getFirstName(), user.getLastName()), chat.getId()));

        UserService.Group group = userService.getGroup(chatId).orElseThrow();
        if (group.getUsers().size() > 1) {
            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            InlineKeyboardButton gotoPitchButton = InlineKeyboardButton.builder().text("Click me").url(
                    "https://t.me/secret_movie_bot").build();

            keyboardRow.add(gotoPitchButton);
            keyboardMarkup.setKeyboard(List.of(keyboardRow));
            messageService.send(absSender, chatId, """
                            Now we can start!
                            Click the button below or create a private chat with me.
                            In the private chat follow the latest instruction or execute the *start* command.""",
                    keyboardMarkup);
        } else {
            messageService.send(absSender, chatId, "Wait for someone else to join first");
            return;
        }
        group.setPitchType(this.pitchType);
        group.getUsers().forEach(item -> userService.getPrivateChat(item.user()).ifPresentOrElse(privateChatId -> {
            Optional<PitchStateMachine> userState = userService.getPitching(item.user());
            if (userState.isPresent()) {
                requestTargetGroup.execute(absSender, item.user(), privateChatId, null);
            }
        }, () -> System.out.println(
                "Private chat for " + item.user().getFirstName() + " is not registered. They would have call the start command to get the greeting message.")));

    }

}