package com.jozard.secretmoviebot.listeners;

import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.Utils;
import com.jozard.secretmoviebot.actions.RequestDescription;
import com.jozard.secretmoviebot.actions.StartVoting;
import com.jozard.secretmoviebot.config.ServiceConfig;
import com.jozard.secretmoviebot.users.Movie;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.capitalize;

@Component
public class OnMovieSent extends PrivateChatListener {

    private static final String DELIMITER = System.getProperty("line.separator");
    private final StartVoting startVoting;
    private final RequestDescription requestDescription;

    public OnMovieSent(MessageService messageService, UserService userService, StartVoting startVoting, RequestDescription requestDescription) {
        super(messageService, userService);
        this.startVoting = startVoting;
        this.requestDescription = requestDescription;
    }

    @Override
    public void doExecute(AbsSender absSender, PitchStateMachine state, Message message, String[] arguments) {
        if (state.getCurrentGroup().isEmpty()) {
            return;
        }
        Optional<UserService.Group> targetGroup = state.getCurrentGroup();
        if (targetGroup.isEmpty()) {
            return; // should never happen
        }

        // movie title sent
        User user = state.getUser();
        System.out.println(MessageFormat.format(
                "The user {0} is pending movie name in chat ID =  {1}. We assume it is a movie in the message",
                user.getFirstName(), state.getCurrentGroup().orElseThrow()));

        String chosenMovie = message.getText();

        state.movieSet();
        targetGroup.get().addMovie(new Movie(chosenMovie, user));

        long groupChatId = targetGroup.get().getChatId();
        messageService.send(absSender, groupChatId,
                MessageFormat.format("*{0}* has selected a movie", capitalize(user.getFirstName())));
        String response;
        boolean requestVotes = false;
        if (targetGroup.get().getPitchType() == UserService.PitchType.RANDOM) {
            state.done();
            if (targetGroup.get().isAllMoviesSelected()) {

                List<String> movies = targetGroup.get().getMovies().stream().map(Movie::getTitle).toList();
                int index = ThreadLocalRandom.current().nextInt(0, movies.size());

                String visibleContent = targetGroup.get().getMovies().stream().map(
                        item -> MessageFormat.format("*{0}* by {1}", item.getTitle(),
                                item.getOwner().getFirstName())).collect(Collectors.joining(DELIMITER));

                String spoilerContent = String.join(DELIMITER,
                        MessageFormat.format("Hurray\\! The chosen one has arrived\\!{1}We are watching *||{0}||*",
                                Utils.escapeMarkdownV2Content(movies.get(index)), DELIMITER));

                messageService.send(absSender, groupChatId, visibleContent);
                messageService.send(absSender, groupChatId, spoilerContent, MessageService.MARKDOWN_V2);
                userService.remove(groupChatId);

                response = MessageFormat.format(
                        "Great! The *{0}* movie is registered and everyone in the group is done. Switch to the group chat to check the result.",
                        chosenMovie, targetGroup.get().getChatName());
            } else {
                response = MessageFormat.format("""
                                Great! The *{0}* movie is registered for *{1}* group. Wait for the others to finish.
                                If you want to select a movie for another group chat, send me the /start command.""",
                        chosenMovie, targetGroup.get().getChatName());
            }

        } else if (List.of(UserService.PitchType.SIMPLE_VOTE, UserService.PitchType.BALANCED_VOTE).contains(
                targetGroup.get().getPitchType())) {
            if (ServiceConfig.DESCRIPTION_ENABLED) {
                state.pendingDescription();
                // we need to send this before requesting the description; therefore having return in the end of the block
                messageService.send(absSender, message.getChatId(),
                        MessageFormat.format("Great! The *{0}* movie is registered.", chosenMovie,
                                targetGroup.get().getChatName()));
                this.requestDescription.execute(absSender, user, message.getChatId(), new String[]{chosenMovie});
                return;
            } else {
                state.pendingVoteStart();
                if (targetGroup.get().isAllMoviesSelected()) {
                    response = MessageFormat.format(
                            "Great! The *{0}* movie is registered and everyone in the group is done.", chosenMovie,
                            targetGroup.get().getChatName());
                    requestVotes = true;
                } else {
                    response = MessageFormat.format(
                            "Great! The *{0}* movie is registered. Wait for the others to finish and then send me the /vote command here.",
                            chosenMovie, targetGroup.get().getChatName());
                }
            }
        } else {
            throw new IllegalStateException(targetGroup.get().getPitchType() + " pitch type is not supported!");
        }
        ReplyKeyboardRemove keyboardRemove = ReplyKeyboardRemove.builder().removeKeyboard(true).build();
        messageService.send(absSender, message.getChatId(), response, keyboardRemove);

        if (requestVotes) {
            // Request everyone for vote
            startVoting.execute(absSender, null, groupChatId, null);
        }
    }
}
