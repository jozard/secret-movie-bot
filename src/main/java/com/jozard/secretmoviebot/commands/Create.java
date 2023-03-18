package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.Utils;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.users.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class Create extends AdminCommand {

    protected final StickerService stickerService;
    private final Consumer<Chat> postCreate;

    public Create(String name, String description, UserService userService, StickerService stickerService, Consumer<Chat> postCreate) {
        super(userService, name, description);
        this.stickerService = stickerService;
        this.postCreate = postCreate;
    }


    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        super.execute(absSender, user, chat, strings);
        long chatId = chat.getId();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();

        if (Utils.isGroup(chat)) {
            boolean allowed = true;
            System.out.println(MessageFormat.format("User {0} is trying to create a pitch in chat {1}",
                    String.join(" ", user.getFirstName(), user.getLastName()), chat.getId()));
            SendMessage response = new SendMessage();
            response.setChatId(String.valueOf(chatId));

            if (userService.getGroup(chatId).orElseThrow().getUsers().size() > 1) {
                InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
                response.setText("""
                        Now we can start!
                        Click the button below or create a private chat with me and then execute the *start* command.""");

                InlineKeyboardButton gotoPitchButton = InlineKeyboardButton.builder().text("Click me").url(
                        "https://t.me/secret_movie_bot").build();

                keyboardRow.add(gotoPitchButton);
                keyboardMarkup.setKeyboard(List.of(keyboardRow));
                response.setReplyMarkup(keyboardMarkup);
            } else {
                allowed = false;
                response.setText("Wait for someone else to join first");

            }
            response.enableMarkdown(false);

            try {
                absSender.execute(response);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            if (!allowed) {
                throw new PitchFailedException("Not enough users joined");
            }
            postCreate.accept(chat);
        }
        // no reason to do this in private chats

    }
}