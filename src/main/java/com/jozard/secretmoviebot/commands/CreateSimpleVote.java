package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;

@Component
public class CreateSimpleVote extends Create {

    public static final String NAME = "create_simple_vote";
    public static final String DESCRIPTION = "Open simple movie vote for the users joined from this group";

    public CreateSimpleVote(UserService userService, MessageService messageService, StickerService stickerService) {
        super(NAME, DESCRIPTION, userService, messageService, stickerService,
                (chat) -> userService.getGroup(chat.getId()).ifPresent(UserService.Group::setSimpleVote));
    }

}