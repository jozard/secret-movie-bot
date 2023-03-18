package com.jozard.secretmoviebot;

import com.jozard.secretmoviebot.users.UserService;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {
    public static boolean isGroup(Chat chat) {
        return chat.isGroupChat() || chat.isSuperGroupChat();
    }

    public static boolean isUser(Chat chat) {
        return chat.isUserChat();
    }

    public static String getVoteSummary(UserService.Group.VoteResult result) {
        String movieResult = MessageFormat.format("{2} votes\\: *{0}* by {1}", result.getMovie().getTitle(),
                result.getMovie().getOwner().getFirstName(), result.getVoted().size());
        List<String> summaryLines = new ArrayList<>();
        summaryLines.add(movieResult);
        summaryLines.add("Voted:");
        summaryLines.addAll(result.getVoted().stream().map(User::getFirstName).toList());
        return summaryLines.stream().collect(Collectors.joining(System.getProperty("line.separator")));
    }
}
