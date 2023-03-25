package com.jozard.secretmoviebot;

import com.jozard.secretmoviebot.users.UserService;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Utils {
    private final static String TO_ESCAPE = "!\"#$%&'()*+,-./:;<=>?@[]^_`{|}~";
    private static final String RESULT_PATTERN_ESCAPED = "{2} votes\\: *{0}* pitched by {1}";
    private static final String RESULT_PATTERN = "{2} votes: *{0}* pitched by {1}";

    public static boolean isGroup(Chat chat) {
        return chat.isGroupChat() || chat.isSuperGroupChat();
    }

    public static boolean isUser(Chat chat) {
        return chat.isUserChat();
    }

    public static String getVoteSummary(UserService.Group.VoteResult result, boolean escaped) {
        String title = escaped ? escapeMarkdownV2Content(result.getMovie().getTitle()) : result.getMovie().getTitle();
        String movieResult = MessageFormat.format(escaped ? RESULT_PATTERN_ESCAPED : RESULT_PATTERN, title,
                result.getMovie().getOwner().getFirstName(), result.getVoted().size());
        String votedList = result.getVoted().stream().map(User::getFirstName).collect(Collectors.joining(","));
        return String.join(System.getProperty("line.separator"), movieResult,
                (escaped ? "Votes\\: " : "Votes: ") + votedList);
    }

    public static String escapeMarkdownV2Content(String value) {
        final String[] result = new String[1];
        result[0] = value.replace("\\", "\\\\");
        Arrays.stream(TO_ESCAPE.split("")).forEach((item) -> result[0] = result[0].replace(item, "\\" + item));
        return result[0];
    }
}
