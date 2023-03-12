package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.users.AdminExistsException;
import com.jozard.secretmoviebot.users.UserService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.jobs.GroupCleanup;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static com.jozard.secretmoviebot.StickerService.NO_KIPESH_STICKER_ID;
import static org.springframework.util.StringUtils.capitalize;

@Component
public class Start extends BotCommand {

    public static final String NAME = "start";
    public static final String DESCRIPTION = """
            With this command you can start the Bot.
            Starting the bot in a group chat registers a movie choosing for users joined from this group.""";
    private final UserService userService;
    private final ThreadPoolTaskScheduler scheduler;
    private final StickerService stickerService;

    public Start(UserService userService, ThreadPoolTaskScheduler scheduler, StickerService stickerService) {
        super(NAME, DESCRIPTION);
        this.userService = userService;
        this.scheduler = scheduler;
        this.stickerService = stickerService;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        long chatId = chat.getId();
        if (chat.isGroupChat()) {
            // we are in a group
            if (!userService.pitchRegistered(chatId)) {
                // first time start command called for this chat
                UserService.Group group;
                try {
                    group = userService.start(chatId, chat.getTitle(), user);
                } catch (AdminExistsException e) {
                    SendMessage response = new SendMessage();
                    response.setChatId(String.valueOf(chatId));
                    response.setText(MessageFormat.format(
                            "{0}, you are already an admin in another movie choosing",
                            capitalize(user.getFirstName())));
                    try {
                        absSender.execute(response);
                    } catch (TelegramApiException telegramApiException) {
                        e.printStackTrace();
                    }
                    return;
                }
                ScheduledFuture<?> cleanupTask = scheduler.schedule(new GroupCleanup(chatId, userService, absSender),
                        Instant.now().plus(20, ChronoUnit.MINUTES));
                group.setCleanupTask(cleanupTask);

            } else {
                SendMessage response = new SendMessage();
                response.setChatId(String.valueOf(chatId));
                response.setText(EmojiParser.parseToUnicode(MessageFormat.format(
                        "{0}, do not abuse, the movie choosing has already been started :stuck_out_tongue_winking_eye:",
                        capitalize(user.getFirstName()))));
                try {
                    stickerService.sendSticker(absSender, chatId, NO_KIPESH_STICKER_ID);
                    absSender.execute(response);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                return;
            }
        } else {
            // private chat. Do nothing here yet.
        }

        System.out.println(MessageFormat.format("User {0} starts bot in {1} chat {2}", user.getUserName(),
                chat.isGroupChat() ? "group" : "private", chat.getId()));
        SendMessage response = new SendMessage();
        response.setChatId(chat.getId().toString());

        if (chat.isGroupChat()) {
            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
            response.setText("Let the movie choosing begin! Click the `Join` button to participate.");
            // someone started the bot in the group
            InlineKeyboardButton joinButton = InlineKeyboardButton.builder().text("Join")
                    .callbackData("btn_join") // check if we get it
                    .build();

            keyboardRow.add(joinButton);
            keyboardMarkup.setKeyboard(List.of(keyboardRow));
            response.enableMarkdown(true);
            response.setReplyMarkup(keyboardMarkup);


        } else {

            List<KeyboardButton> groupButtons = userService.getGroupsAvailableToStartPitch(user).stream().map(
                    group -> KeyboardButton.builder().text(group.getChatName()).build()).toList();
            if (groupButtons.isEmpty()) {
                response.setText(
                        "Cannot start. Either you are done with all pitches, a bot is waiting for a movie title from you, or no pitch is created in your groups.");
            } else {
                userService.getPitching(user).orElseThrow().start();
                response.setText("Send me a group to choose movies. You can use the button(s) below.");
                List<KeyboardRow> keyboardRows = groupButtons.stream().map(button -> {
                    KeyboardRow row = new KeyboardRow();
                    row.add(button);
                    return row;
                }).toList();
                ReplyKeyboardMarkup replyKeyboardMarkup = ReplyKeyboardMarkup
                        .builder()
                        .keyboard(keyboardRows)
                        .resizeKeyboard(true)
                        .oneTimeKeyboard(true)
                        .build();
                response.setReplyMarkup(replyKeyboardMarkup);
            }

            response.enableMarkdown(true);

        }


        try {
            absSender.execute(response);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}