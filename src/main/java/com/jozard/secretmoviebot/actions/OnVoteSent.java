package com.jozard.secretmoviebot.actions;

import com.jozard.secretmoviebot.Utils;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.springframework.util.StringUtils.capitalize;

@Component
public class OnVoteSent extends PrivateChatAction {

    private static final String DELIMITER = System.getProperty("line.separator");
    private final UserService userService;

    public OnVoteSent(UserService userService) {
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
        if (List.of(UserService.PitchType.SIMPLE_VOTE, UserService.PitchType.BALANCED_VOTE).contains(
                targetGroup.get().getPitchType())) {

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
                    List<UserService.Group.VoteResult> votes = targetGroup.get().getVotes();
                    Set<User> votedUsers = votes.stream().flatMap(item -> item.getVoted().stream()).collect(
                            Collectors.toSet());

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
                        List<UserService.Group.VoteResult> sortedVoteResults = votes.stream().sorted(
                                Comparator.comparing(voteResult -> voteResult.getVoted().size(),
                                        Comparator.reverseOrder())).toList();
                        String hiddenContent;
                        String visibleContent = "";
                        if (UserService.PitchType.BALANCED_VOTE.equals(targetGroup.get().getPitchType())) {

                            // send vote results, they can be visible
                            visibleContent = sortedVoteResults.stream().map(Utils::getVoteSummary).collect(
                                    Collectors.joining(DELIMITER));

                            // generate list of weights
                            List<Integer> weights = votes.stream().flatMapToInt(
                                    item -> IntStream.generate(() -> votes.indexOf(item)).limit(
                                            votes.size())).boxed().toList();
                            int index = ThreadLocalRandom.current().nextInt(0, weights.size());
                            UserService.Group.VoteResult winner = votes.get(weights.get(index));

                            hiddenContent = MessageFormat.format("Hurray\\! The chosen one has arrived\\!{1}*||{0}||*",
                                    winner.getMovie().getTitle(), DELIMITER);
                        } else {
                            // simple vote
                            hiddenContent = sortedVoteResults.stream().map(Utils::getVoteSummary).collect(
                                    Collectors.joining(DELIMITER));
                        }

                        groupNotification.setText(
                                Stream.of(visibleContent, DELIMITER, "||" + hiddenContent + "||").collect(
                                        Collectors.joining(DELIMITER)));
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
