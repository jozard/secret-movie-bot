package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.Utils;
import com.jozard.secretmoviebot.actions.RequestVote;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.Optional;

@Component
public class Vote extends BotCommand {

    public static final String NAME = "vote";
    public static final String DESCRIPTION = """
            With this command you can vote for a movie.""";
    private final MessageService messageService;
    private final UserService userService;
    private final ThreadPoolTaskScheduler scheduler;
    private final StickerService stickerService;
    private final RequestVote requestVote;

    public Vote(MessageService messageService, UserService userService, ThreadPoolTaskScheduler scheduler, StickerService stickerService, RequestVote requestVote) {
        super(NAME, DESCRIPTION);
        this.messageService = messageService;
        this.userService = userService;
        this.scheduler = scheduler;
        this.stickerService = stickerService;
        this.requestVote = requestVote;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        long chatId = chat.getId();
        if (Utils.isUser(chat)) {

            Optional<PitchStateMachine> votingUser = userService.getPitching(user);
            if (votingUser.isPresent()) {
                requestVote.execute(absSender, user, chatId, null);
            } else {
                System.out.println("!!! Pitching state is not present for " + user.getLastName());
                System.out.println("!!! Pitching states for " + chatId + ": " + userService.getStates(chatId));
            }
        }


    }
}