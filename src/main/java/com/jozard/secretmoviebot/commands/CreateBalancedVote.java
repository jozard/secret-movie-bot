package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;

@Component
public class CreateBalancedVote extends Create {

    public static final String NAME = "create_balanced_vote";
    public static final String DESCRIPTION = """
            Open balanced movie vote for the users joined from this group.
            The movie will be randomly selected from proposed by the users.
            Movies with more votes get more chances to be chosen.
            """;

    public CreateBalancedVote(UserService userService, MessageService messageService, StickerService stickerService) {
        super(NAME, DESCRIPTION, userService, messageService, stickerService,
                chat -> userService.getGroup(chat.getId()).ifPresent(UserService.Group::setBalancedVote));
    }

}