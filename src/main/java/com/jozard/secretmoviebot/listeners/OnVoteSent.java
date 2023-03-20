package com.jozard.secretmoviebot.listeners;

import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.Utils;
import com.jozard.secretmoviebot.actions.RequestVote;
import com.jozard.secretmoviebot.users.Movie;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.springframework.util.StringUtils.capitalize;

@Component
public class OnVoteSent extends PrivateChatListener {

    private static final String DELIMITER = System.getProperty("line.separator");

    private final RequestVote requestVote;
    private final StickerService stickerService;

    public OnVoteSent(UserService userService, MessageService messageService, RequestVote requestVote, StickerService stickerService) {
        super(messageService, userService);
        this.requestVote = requestVote;
        this.stickerService = stickerService;
    }

    @Override
    public void doExecute(AbsSender absSender, PitchStateMachine state, Message message, String[] arguments) {
        Optional<UserService.Group> targetGroup = state.getCurrentGroup();
        if (targetGroup.isEmpty()) {
            return;
        }
        if (List.of(UserService.PitchType.SIMPLE_VOTE, UserService.PitchType.BALANCED_VOTE).contains(
                targetGroup.get().getPitchType())) {
            Optional<Movie> matchingMovie = targetGroup.get().getMovies().stream().filter(
                    movie -> movie.getTitle().equals(message.getText())).findFirst();
            User user = state.getUser();
            if (matchingMovie.isPresent()) {
                // voted proper movie
                if (matchingMovie.get().getOwner().equals(user)) {
                    stickerService.sendSticker(absSender, message.getChatId(), StickerService.ESCUZMI_STICKER_ID);
                    messageService.send(absSender, message.getChatId(),
                            MessageFormat.format("{0} is your movie. Please chose any other.",
                                    matchingMovie.get().getTitle()));
                } else {
                    targetGroup.get().addVote(matchingMovie.get(), user);
                    state.done();

                    //check if all are voted
                    List<UserService.Group.VoteResult> votes = targetGroup.get().getVotes();
                    Set<User> votedUsers = votes.stream().flatMap(item -> item.getVoted().stream()).collect(
                            Collectors.toSet());

                    long chatId = targetGroup.get().getChatId();
                    messageService.send(absSender, chatId,
                            MessageFormat.format("*{0}* has voted", capitalize(user.getFirstName())));
                    if (votedUsers.size() == targetGroup.get().getUsers().size()) {
                        // show result in the group

                        List<UserService.Group.VoteResult> sortedVoteResults = votes.stream().sorted(
                                Comparator.comparing(voteResult -> voteResult.getVoted().size(),
                                        Comparator.reverseOrder())).toList();
                        // TODO: add description to result summary
                        if (UserService.PitchType.BALANCED_VOTE.equals(targetGroup.get().getPitchType())) {

                            // send vote results, they can be visible
                            String visibleContent = sortedVoteResults.stream().map(
                                    item -> Utils.getVoteSummary(item, false)).collect(Collectors.joining(DELIMITER));
                            messageService.send(absSender, chatId, visibleContent);

                            // generate list of weights
                            List<Integer> weights = votes.stream().flatMapToInt(
                                    item -> IntStream.generate(() -> votes.indexOf(item)).limit(
                                            votes.size())).boxed().toList();
                            int index = ThreadLocalRandom.current().nextInt(0, weights.size());
                            UserService.Group.VoteResult winner = votes.get(weights.get(index));

                            messageService.send(absSender, chatId,
                                    MessageFormat.format(
                                            "Hurray\\! The chosen one has arrived\\!{1}We are watching *||{0}||*",
                                            Utils.escapeMarkdownV2Content(winner.getMovie().getTitle()), DELIMITER),
                                    MessageService.MARKDOWN_V2);
                        } else {
                            // simple vote
                            messageService.send(absSender, chatId,
                                    "||" + sortedVoteResults.stream().map(
                                            item -> Utils.getVoteSummary(item, true)).collect(
                                            Collectors.joining(DELIMITER)) + "||", MessageService.MARKDOWN_V2);
                        }

                        userService.remove(chatId);
                    }

                    ReplyKeyboardRemove keyboardRemove = ReplyKeyboardRemove.builder().removeKeyboard(true).build();
                    messageService.send(absSender, message.getChatId(), MessageFormat.format(
                            "Your vote for the *{0}* movie is registered for *{1}* group. If you want to select a movie for another group chat, send me the /start command",
                            message.getText(), targetGroup.get().getChatName()), keyboardRemove);
                }
            } else {
                //privateChatResponse wrong vote
                messageService.send(absSender, message.getChatId(),
                        MessageFormat.format("Movie *{0}* was not proposed in {1}.", message.getText(),
                                targetGroup.get().getChatName()));
                requestVote.execute(absSender, user, message.getChatId(), null);
            }
        }
    }
}
