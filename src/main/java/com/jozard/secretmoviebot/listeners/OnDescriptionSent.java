package com.jozard.secretmoviebot.listeners;

import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.actions.StartVoting;
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

import static org.springframework.util.StringUtils.capitalize;

@Component
public class OnDescriptionSent extends PrivateChatListener {
    private static final String DELIMITER = System.getProperty("line.separator");
    private final StartVoting startVoting;

    public OnDescriptionSent(MessageService messageService, UserService userService, StartVoting startVoting) {
        super(messageService, userService);
        this.startVoting = startVoting;
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

        // movie description sent
        User user = state.getUser();
        System.out.println(MessageFormat.format(
                "The user {0} is pending movie description in chat ID =  {1}. We assume it is a desctription in the message",
                user.getFirstName(), state.getCurrentGroup().orElseThrow()));

        String description = message.getText();

        state.descriptionSet();
        Movie movie = targetGroup.get().getMovie(user).orElseThrow();
        movie.setDescription(description);

        long groupChatId = targetGroup.get().getChatId();
        messageService.send(absSender, groupChatId,
                MessageFormat.format("*{0}* has sent a movie description", capitalize(user.getFirstName())));
        String response;
        boolean requestVotes = false;
        if (targetGroup.get().getPitchType() == UserService.PitchType.RANDOM) {
            return; // should never happen
        } else if (List.of(UserService.PitchType.SIMPLE_VOTE, UserService.PitchType.BALANCED_VOTE).contains(
                targetGroup.get().getPitchType())) {

            state.pendingVoteStart();
            if (targetGroup.get().isAllMoviesSelected()) {
                response = MessageFormat.format(
                        "Great! The description for *{0}* movie is registered and everyone in the group is done.",
                        movie.getTitle(),
                        targetGroup.get().getChatName());
                requestVotes = true;
            } else {
                response = MessageFormat.format(
                        "Great! The description for *{0}* movie is registered. Wait for the others to finish and then send me the /vote command here.",
                        movie.getTitle(), targetGroup.get().getChatName());
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
