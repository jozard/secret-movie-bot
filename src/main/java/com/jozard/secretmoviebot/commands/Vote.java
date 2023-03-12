package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.users.UserService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Optional;

import static com.jozard.secretmoviebot.StickerService.NO_KIPESH_STICKER_ID;

@Component
public class Vote extends BotCommand {

    public static final String NAME = "vote";
    public static final String DESCRIPTION = """
            With this command you can vote for a movie.""";
    private final UserService userService;
    private final ThreadPoolTaskScheduler scheduler;
    private final StickerService stickerService;

    public Vote(UserService userService, ThreadPoolTaskScheduler scheduler, StickerService stickerService) {
        super(NAME, DESCRIPTION);
        this.userService = userService;
        this.scheduler = scheduler;
        this.stickerService = stickerService;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        long chatId = chat.getId();




        if (chat.isUserChat()) {

            Optional<PitchStateMachine> votingUser = userService.getPitching(user);
            if (votingUser.isPresent()) {
                if (votingUser.get().isPendingStart() || votingUser.get().isPendingCurrentGroup() || votingUser.get().isPendingMovie()) {
                    return;
                }
                SendMessage response = new SendMessage();
                response.setChatId(chat.getId().toString());

                if (votingUser.get().isPendingVoteStart()) {
                    Long userCurrentGroup = votingUser.get().getCurrentGroup().orElseThrow();
                    UserService.Group group = userService.getGroup(userCurrentGroup).orElseThrow();
                    if (group.isAllMoviesSelected()) {

                        if (group.getPitchType().equals(UserService.PitchType.SIMPLE_VOTE)) {
                            votingUser.get().pendingSimpleVote();
                            List<KeyboardButton> groupButtons = group.getMovies().stream().filter(
                                    movie -> !movie.getOwner().equals(user)).map(
                                    movie -> KeyboardButton.builder().text(movie.getTitle()).build()).toList();

                            response.setText("Send me your vote. You can use the button(s) below.");
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
                            response.enableMarkdown(true);
                        } else {
                            // we ignore random choosing or any other vote types yet
                            return;
                        }
                    }
                } else {
                    response.setText("Wait for the others to choose movies");
                    stickerService.sendSticker(absSender, chatId, NO_KIPESH_STICKER_ID);
                }
                try {
                    absSender.execute(response);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("!!! Pitching state is not present for " + user.getLastName());
                System.out.println("!!! Pitching states for " + chatId + ": " + userService.getStates(chatId));
            }
        }



    }
}