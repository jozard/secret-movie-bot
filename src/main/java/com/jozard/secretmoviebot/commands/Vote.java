package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.Utils;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;
import java.util.Optional;

import static com.jozard.secretmoviebot.StickerService.NO_KIPESH_STICKER_ID;

@Component
public class Vote extends BotCommand {

    public static final String NAME = "vote";
    public static final String DESCRIPTION = """
            With this command you can vote for a movie.""";
    private final MessageService messageService;
    private final UserService userService;
    private final ThreadPoolTaskScheduler scheduler;
    private final StickerService stickerService;

    public Vote(MessageService messageService, UserService userService, ThreadPoolTaskScheduler scheduler, StickerService stickerService) {
        super(NAME, DESCRIPTION);
        this.messageService = messageService;
        this.userService = userService;
        this.scheduler = scheduler;
        this.stickerService = stickerService;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        long chatId = chat.getId();
        if (Utils.isUser(chat)) {

            Optional<PitchStateMachine> votingUser = userService.getPitching(user);
            if (votingUser.isPresent()) {
                if (votingUser.get().isPendingStart() || votingUser.get().isPendingCurrentGroup() || votingUser.get().isPendingMovie()) {
                    return;
                }

                if (votingUser.get().isPendingVoteStart()) {
                    Long userCurrentGroup = votingUser.get().getCurrentGroup().orElseThrow();
                    UserService.Group group = userService.getGroup(userCurrentGroup).orElseThrow();
                    if (group.isAllMoviesSelected()) {

                        if (List.of(UserService.PitchType.SIMPLE_VOTE, UserService.PitchType.BALANCED_VOTE).contains(
                                group.getPitchType())) {
                            votingUser.get().pendingVote();
                            List<KeyboardButton> groupButtons = group.getMovies().stream().filter(
                                    movie -> !movie.getOwner().equals(user)).map(
                                    movie -> KeyboardButton.builder().text(movie.getTitle()).build()).toList();

                            List<KeyboardRow> keyboardRows = groupButtons.stream().map(button -> {
                                KeyboardRow row = new KeyboardRow();
                                row.add(button);
                                return row;
                            }).toList();
                            ReplyKeyboardMarkup replyKeyboardMarkup = ReplyKeyboardMarkup.builder().keyboard(
                                    keyboardRows).resizeKeyboard(true).oneTimeKeyboard(true).build();
                            messageService.send(absSender, chatId,
                                    "Send me your vote. You can use the button(s) below.", replyKeyboardMarkup);
                        }
                        // we ignore random choosing or any other vote types yet
                    }
                } else {
                    stickerService.sendSticker(absSender, chatId, NO_KIPESH_STICKER_ID);
                    messageService.send(absSender, chatId, "Wait for the others to choose movies");
                }
            } else {
                System.out.println("!!! Pitching state is not present for " + user.getLastName());
                System.out.println("!!! Pitching states for " + chatId + ": " + userService.getStates(chatId));
            }
        }


    }
}