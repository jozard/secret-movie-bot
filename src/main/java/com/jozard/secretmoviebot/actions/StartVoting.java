package com.jozard.secretmoviebot.actions;

import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.users.Movie;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class StartVoting extends Action {

    private final UserService userService;
    private final RequestVote requestVote;

    private final MessageService messageService;

    public StartVoting(UserService userService, RequestVote requestVote, MessageService messageService) {
        this.userService = userService;
        this.requestVote = requestVote;
        this.messageService = messageService;
    }

    @Override
    public void execute(AbsSender absSender, User user, long chatId, String[] arguments) {

        // everyone selected -> vote
        InlineKeyboardButton voteButton = InlineKeyboardButton.builder().text("Go to vote").url(
                "https://t.me/secret_movie_bot").build();
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
        keyboardRow.add(voteButton);
        keyboardMarkup.setKeyboard(List.of(keyboardRow));

        Optional<UserService.Group> group = userService.getGroup(chatId);
        Function<Movie, String> mapper = group.orElseThrow().isDescriptionEnabled() ? movie -> MessageFormat.format(
                " - *{0}*: _{1}_", movie.getTitle(),
                movie.getDescription()) : movie -> MessageFormat.format(" - *{0}*", movie.getTitle());
        String allOptions =
                group.get().getMovies().stream().map(mapper).collect(
                        Collectors.joining(System.getProperty("line.separator")));

        messageService.send(absSender, chatId, MessageFormat.format("""
                        Amazing! The movie list is ready!
                        {0}
                        Everyone can vote now. Click the button below or go to a private chat with me.""", allOptions),
                keyboardMarkup);

        group.get().getUsers().forEach(
                item -> userService.getPrivateChat(item.user()).ifPresentOrElse(privateChatId -> {
                    PitchStateMachine userState = userService.getPitching(item.user()).orElseThrow();
                    if (userState.isPendingVoteStart()) {
                        userState.pendingVote();
                    }

                    String userVoteOptions = MessageFormat.format("""
                                    The others proposed the movies below:
                                    {0}
                                    """,
                            group.get().getMovies().stream().filter(movie -> !movie.getOwner().equals(item.user())).map(
                                    mapper).collect(Collectors.joining(System.getProperty("line.separator"))));

                    messageService.send(absSender, privateChatId, userVoteOptions);
                    requestVote.execute(absSender, item.user(), privateChatId, null);

                }, () -> System.out.println(
                        "Private chat for " + item.user().getFirstName() + " is not registered! Should not happen for a user in pending vote state¬")));


    }
}
