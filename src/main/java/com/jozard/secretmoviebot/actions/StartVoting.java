package com.jozard.secretmoviebot.actions;

import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.ArrayList;
import java.util.List;

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
        messageService.send(absSender, chatId, """
                Amazing! The movie list is ready now!
                Everyone can vote now. Click the button below or go to a private chat with me.""", keyboardMarkup);


        userService.getGroup(chatId).orElseThrow().getUsers().forEach(
                item -> userService.getPrivateChat(item.user()).ifPresentOrElse(privateChatId -> {
                    PitchStateMachine userState = userService.getPitching(item.user()).orElseThrow();
                    if (userState.isPendingVoteStart()) {
                        userState.pendingVote();
                    }
                    // TODO: print movie list with descriptions to the private chats
                    requestVote.execute(absSender, item.user(), privateChatId, null);

                }, () -> System.out.println(
                        "Private chat for " + item.user().getFirstName() + " is not registered! Should not happen for a user in pending vote state¬")));


    }
}
