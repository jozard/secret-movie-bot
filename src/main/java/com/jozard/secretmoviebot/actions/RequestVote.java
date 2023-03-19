package com.jozard.secretmoviebot.actions;

import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;
import java.util.Optional;

import static com.jozard.secretmoviebot.StickerService.NO_KIPESH_STICKER_ID;

@Component
public class RequestVote extends Action {

    private final UserService userService;
    private final StickerService stickerService;

    private final MessageService messageService;

    public RequestVote(UserService userService, StickerService stickerService, MessageService messageService) {
        this.userService = userService;
        this.stickerService = stickerService;
        this.messageService = messageService;
    }

    @Override
    public void execute(AbsSender absSender, User user, long chatId, String[] arguments) {

        Optional<PitchStateMachine> state = userService.getPitching(user);
        if (state.isPresent()) {
            if (state.get().isPendingVote() || state.get().isPendingVoteStart()) {
                if (state.get().isPendingVote()) {
                    UserService.Group group = state.get().getCurrentGroup().orElseThrow();
                    if (List.of(UserService.PitchType.SIMPLE_VOTE, UserService.PitchType.BALANCED_VOTE).contains(
                            group.getPitchType())) {
                        if (group.isAllMoviesSelected()) {
                            state.get().pendingVote();
                            List<KeyboardButton> groupButtons = group.getMovies().stream().filter(
                                    movie -> !movie.getOwner().equals(user)).map(
                                    movie -> KeyboardButton.builder().text(movie.getTitle()).build()).toList();

                            List<KeyboardRow> keyboardRows = groupButtons.stream().map(button -> {
                                KeyboardRow row = new KeyboardRow();
                                row.add(button);
                                return row;
                            }).toList();
                            ReplyKeyboardMarkup replyKeyboardMarkup = ReplyKeyboardMarkup.builder().keyboard(
                                    keyboardRows).resizeKeyboard(
                                    true).oneTimeKeyboard(true).build();
                            messageService.send(absSender, chatId,
                                    "Send me your vote. You can use the button(s) below.",
                                    replyKeyboardMarkup);
                        }
                    }
                    // we ignore random choosing or any other vote types yet
                } else {
                    stickerService.sendSticker(absSender, chatId, NO_KIPESH_STICKER_ID);
                    messageService.send(absSender, chatId, "Wait for the others to finish choose movies");
                }
            }
        }
    }
}
