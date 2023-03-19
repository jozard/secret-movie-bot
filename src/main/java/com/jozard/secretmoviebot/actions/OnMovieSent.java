package com.jozard.secretmoviebot.actions;

import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.Utils;
import com.jozard.secretmoviebot.users.Movie;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.capitalize;

@Component
public class OnMovieSent extends PrivateChatAction {

    private static final String DELIMITER = System.getProperty("line.separator");
    private final StickerService stickerService;

    public OnMovieSent(MessageService messageService, UserService userService, StickerService stickerService) {
        super(messageService, userService);
        this.stickerService = stickerService;
    }

    @Override
    public void execute(AbsSender absSender, PitchStateMachine state, Message message, String[] arguments) {
        if (state.getCurrentGroup().isEmpty()) {
            return;
        }
        Optional<UserService.Group> targetGroup = userService.getGroup(state.getCurrentGroup().get());
        if (targetGroup.isEmpty()) {
            return; // should never happen
        }
        this.reply(absSender, message.getChatId(), response -> {

            // movie title sent
            System.out.println(MessageFormat.format(
                    "The user {0} is pending movie name in chat ID =  {1}. We assume it is a movie in the message",
                    state.getUser().getFirstName(), state.getCurrentGroup().orElseThrow()));

            String chosenMovie = message.getText();

            Movie movie = state.setMovie(chosenMovie);
            targetGroup.get().addMovie(movie);

            long groupChatId = targetGroup.get().getChatId();
            messageService.send(absSender, groupChatId,
                    MessageFormat.format("*{0}* has selected a movie", capitalize(state.getUser().getFirstName())));
            if (targetGroup.get().getPitchType() == UserService.PitchType.RANDOM) {
                state.done();
                if (targetGroup.get().isAllMoviesSelected()) {

                    List<String> movies = targetGroup.get().getMovies().stream().map(Movie::getTitle).toList();
                    int index = ThreadLocalRandom.current().nextInt(0, movies.size());

                    String visibleContent = targetGroup.get().getMovies().stream().map(
                            item -> MessageFormat.format("*{0}* by {1}", item.getTitle(),
                                    item.getOwner().getFirstName())).collect(Collectors.joining(DELIMITER));

                    String spoilerContent = String.join(DELIMITER,
                            MessageFormat.format("Hurray\\! The chosen one has arrived\\!{1}*||{0}||*",
                                    Utils.escapeMarkdownV2Content(movies.get(index)), DELIMITER));

                    messageService.send(absSender, groupChatId, visibleContent);
                    messageService.send(absSender, groupChatId, spoilerContent, MessageService.MARKDOWN_V2);
                    userService.remove(groupChatId);

                    response.setText(MessageFormat.format(
                            "Great! The *{0}* movie is registered and everyone in the group is done. Switch to the group chat to check the result.",
                            chosenMovie, targetGroup.get().getChatName()));
                } else {
                    response.setText(MessageFormat.format("""
                                    Great! The *{0}* movie is registered for *{1}* group. Wait for the others to finish.
                                    If you want to select a movie for another group chat, send me the /start command.""",
                            chosenMovie, targetGroup.get().getChatName()));
                }

            } else if (List.of(UserService.PitchType.SIMPLE_VOTE, UserService.PitchType.BALANCED_VOTE).contains(
                    targetGroup.get().getPitchType())) {
                state.pendingVoteStart();
                if (targetGroup.get().isAllMoviesSelected()) {
                    // everyone selected -> vote
                    response.setText(MessageFormat.format(
                            "Great! The *{0}* movie is registered and everyone in the group is done. Send me the /vote command here.",
                            chosenMovie, targetGroup.get().getChatName()));

                    InlineKeyboardButton voteButton = InlineKeyboardButton.builder().text("Go to vote").url(
                            "https://t.me/secret_movie_bot").build();
                    InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
                    List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
                    keyboardRow.add(voteButton);
                    keyboardMarkup.setKeyboard(List.of(keyboardRow));
                    messageService.send(absSender, groupChatId, """
                                    Amazing! The movie list is ready now!
                                    Click the button below or go to a private chat with me and then execute the *vote* command.""",
                            keyboardMarkup);
                } else {
                    response.setText(MessageFormat.format(
                            "Great! The *{0}* movie is registered. Wait for the others to finish and then send me the /vote command here.",
                            chosenMovie, targetGroup.get().getChatName()));
                }
            } else {
                throw new IllegalStateException(targetGroup.get().getPitchType() + " pitch type is not supported!");
            }
            ReplyKeyboardRemove keyboardRemove = ReplyKeyboardRemove.builder().removeKeyboard(true).build();
            response.setReplyMarkup(keyboardRemove);

        });

    }
}
