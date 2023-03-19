package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.users.UserService;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class Create extends AdminCommand {

    protected final StickerService stickerService;
    private final Consumer<Chat> postCreate;

    public Create(String name, String description, UserService userService, MessageService messageService, StickerService stickerService, Consumer<Chat> postCreate) {
        super(userService, messageService, name, description);
        this.stickerService = stickerService;
        this.postCreate = postCreate;
    }

    @Override
    public void onCommandAction(AbsSender absSender, Chat chat, User user) {
        long chatId = chat.getId();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();

        boolean allowed = true;
        System.out.println(MessageFormat.format("User {0} is trying to create a pitch in chat {1}",
                String.join(" ", user.getFirstName(), user.getLastName()), chat.getId()));

        if (userService.getGroup(chatId).orElseThrow().getUsers().size() > 1) {
            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            InlineKeyboardButton gotoPitchButton = InlineKeyboardButton.builder().text("Click me").url(
                    "https://t.me/secret_movie_bot").build();

            keyboardRow.add(gotoPitchButton);
            keyboardMarkup.setKeyboard(List.of(keyboardRow));
            messageService.send(absSender, chatId, """
                            Now we can start!
                            Click the button below or create a private chat with me and then execute the *start* command.""",
                    keyboardMarkup);
        } else {
            allowed = false;
            messageService.send(absSender, chatId, "Wait for someone else to join first");
        }

        if (!allowed) {
            throw new PitchFailedException("Not enough users joined");
        }
        postCreate.accept(chat);

    }

}