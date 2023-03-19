package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.Utils;
import com.jozard.secretmoviebot.jobs.GroupCleanup;
import com.jozard.secretmoviebot.users.AdminExistsException;
import com.jozard.secretmoviebot.users.UserService;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;

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
    private final MessageService messageService;
    private final UserService userService;
    private final ThreadPoolTaskScheduler scheduler;
    private final StickerService stickerService;

    public Start(MessageService messageService, UserService userService, ThreadPoolTaskScheduler scheduler, StickerService stickerService) {
        super(NAME, DESCRIPTION);
        this.messageService = messageService;
        this.userService = userService;
        this.scheduler = scheduler;
        this.stickerService = stickerService;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        long chatId = chat.getId();
        boolean isGroup = Utils.isGroup(chat);
        if (isGroup) {
            // we are in a group
            if (!userService.pitchRegistered(chatId)) {
                // first time start command called for this chat
                UserService.Group group;
                try {
                    group = userService.start(chatId, chat.getTitle(), user);
                } catch (AdminExistsException e) {
                    messageService.send(absSender, chatId,
                            MessageFormat.format("{0}, you are already an admin in another movie choosing",
                                    capitalize(user.getFirstName())));
                    return;
                }
                ScheduledFuture<?> cleanupTask = scheduler.schedule(new GroupCleanup(chatId, userService, absSender),
                        Instant.now().plus(20, ChronoUnit.MINUTES));
                group.setCleanupTask(cleanupTask);

            } else {
                stickerService.sendSticker(absSender, chatId, NO_KIPESH_STICKER_ID);
                messageService.send(absSender, chatId, EmojiParser.parseToUnicode(MessageFormat.format(
                        "{0}, do not abuse, the movie choosing has already been started :stuck_out_tongue_winking_eye:",
                        capitalize(user.getFirstName()))));
                return;
            }
        }

        System.out.println(MessageFormat.format("User {0} starts bot in {1} chat {2}", user.getFirstName(),
                chat.isGroupChat() ? "group" : "private", chat.getId()));

        if (isGroup) {
            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
            // someone started the bot in the group
            InlineKeyboardButton joinButton = InlineKeyboardButton.builder().text("Join").callbackData(
                            "btn_join") // check if we get it
                    .build();

            keyboardRow.add(joinButton);
            keyboardMarkup.setKeyboard(List.of(keyboardRow));
            messageService.send(absSender, chatId,
                    "Let the movie choosing begin! Click the `Join` button to participate.", keyboardMarkup);

        } else if (Utils.isUser(chat)) {

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
                        keyboardRows).resizeKeyboard(true).oneTimeKeyboard(true).build();
                messageService.send(absSender, chatId,
                        "Send me a group to choose movies. You can use the button(s) below.", replyKeyboardMarkup);
            }

        }

    }
}