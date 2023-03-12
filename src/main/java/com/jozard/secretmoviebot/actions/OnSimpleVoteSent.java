package com.jozard.secretmoviebot.actions;

import com.jozard.secretmoviebot.users.Movie;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.capitalize;

@Component
public class OnSimpleVoteSent extends PrivateChatAction {

    private final UserService userService;

    public OnSimpleVoteSent(UserService userService) {
        this.userService = userService;
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
        if (targetGroup.get().getPitchType() == UserService.PitchType.SIMPLE_VOTE) {

            this.reply(absSender, message.getChatId(), response -> {
                response.enableMarkdown(true);
                Optional<Movie> matchingMovie = targetGroup.get().getMovies().stream().filter(
                        movie -> movie.getTitle().equals(message.getText())).findFirst();
                User user = state.getUser();
                if (matchingMovie.isPresent()) {
                    // voted proper movie

                    targetGroup.get().addVote(matchingMovie.get(), user);

                    SendMessage groupNotification = new SendMessage();
                    groupNotification.setChatId(String.valueOf(targetGroup.get().getChatId()));
                    groupNotification.enableMarkdown(true);

                    //check if all are voted
                    Set<User> votedUsers = targetGroup.get().getVotes().stream().flatMap(
                            item -> item.getVoted().stream()).collect(Collectors.toSet());

                    if (votedUsers.size() == targetGroup.get().getUsers().size()) {
                        // show result in the group

                        SendMessage extraGroupNotification = new SendMessage();
                        extraGroupNotification.setChatId(String.valueOf(targetGroup.get().getChatId()));
                        extraGroupNotification.enableMarkdown(true);
                        extraGroupNotification.setText(
                                MessageFormat.format("*{0}* has voted", capitalize(user.getFirstName())));
                        try {
                            absSender.execute(extraGroupNotification);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                        List<UserService.Group.VoteResult> sorted = targetGroup.get().getVotes().stream().sorted(
                                Comparator.comparing(voteResult -> voteResult.getVoted().size(),
                                        Comparator.reverseOrder())).toList();
                        String content = sorted.stream().map(
                                item -> MessageFormat.format("*{0}* \\- {1} votes", item.getMovie().getTitle(),
                                        item.getVoted().size())).collect(
                                Collectors.joining(System.getProperty("line.separator")));
                        groupNotification.setText("||" + content + "||");
                        groupNotification.setParseMode("MarkdownV2");
                        userService.remove(targetGroup.get().getChatId());

                    } else {
                        groupNotification.setText(
                                MessageFormat.format("*{0}* has voted", capitalize(user.getFirstName())));
                    }

                    try {
                        absSender.execute(groupNotification);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    response.setText(MessageFormat.format(
                            "Your vote for the *{0}* movie is registered for *{1}* group. If you want to select a movie for another group chat, send me the /start command",
                            message.getText(), targetGroup.get().getChatName()));
                    ReplyKeyboardRemove keyboardRemove = ReplyKeyboardRemove.builder().removeKeyboard(true).build();
                    response.setReplyMarkup(keyboardRemove);
                } else {
                    //response wrong vote
                    response.setText(MessageFormat.format(
                            "Movie *{0}* Not found. You can use the buttons below to vote for a movie",
                            message.getText(), targetGroup.get().getChatName()));
                }


            });
        }
    }
}
