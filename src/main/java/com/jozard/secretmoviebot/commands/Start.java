package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.Utils;
import com.jozard.secretmoviebot.actions.RequestDescription;
import com.jozard.secretmoviebot.actions.RequestMovie;
import com.jozard.secretmoviebot.actions.RequestTargetGroup;
import com.jozard.secretmoviebot.actions.RequestVote;
import com.jozard.secretmoviebot.jobs.GroupCleanup;
import com.jozard.secretmoviebot.users.AdminExistsException;
import com.jozard.secretmoviebot.users.Movie;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    private final RequestTargetGroup requestTargetGroup;
    private final RequestMovie requestMovie;
    private final RequestVote requestVote;
    private final RequestDescription requestDescription;

    public Start(MessageService messageService, UserService userService, ThreadPoolTaskScheduler scheduler, StickerService stickerService, RequestTargetGroup requestTargetGroup, RequestMovie requestMovie, RequestDescription requestDescription, RequestVote requestVote) {
        super(NAME, DESCRIPTION);
        this.messageService = messageService;
        this.userService = userService;
        this.scheduler = scheduler;
        this.stickerService = stickerService;
        this.requestTargetGroup = requestTargetGroup;
        this.requestMovie = requestMovie;
        this.requestDescription = requestDescription;
        this.requestVote = requestVote;
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
            Optional<PitchStateMachine> stateWrapper = userService.getPitching(user);
            if (stateWrapper.isPresent()) {
                PitchStateMachine state = stateWrapper.get();
                if (state.isPendingStart() || state.isPendingCurrentGroup()) {
                    requestTargetGroup.execute(absSender, user, chatId, strings);
                } else if (state.isPendingMovie()) {
                    UserService.Group currentGroup = state.getCurrentGroup().orElseThrow();
                    requestMovie.execute(absSender, user, chatId, new String[]{currentGroup.getChatName()});
                } else if (state.isPendingDescription()) {
                    Movie userMovie = userService.getGroup(
                            state.getCurrentGroup().orElseThrow().getChatId()).orElseThrow().getMovie(
                            user).orElseThrow();
                    requestDescription.execute(absSender, user, chatId, new String[]{userMovie.getTitle()});
                } else if (state.isPendingVoteStart()) {
                    // waiting; do nothing
                } else if (state.isPendingVote()) {
                    requestVote.execute(absSender, user, chatId, null);
                } else {
                    throw new IllegalStateException(
                            "State " + state.getCurrentState() + " is not supported in Start command");
                }
            }

        }

    }
}