package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.actions.RequestTargetGroup;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;

@Component
public class CreateRandom extends Create {

    public static final String NAME = "create_random";
    public static final String DESCRIPTION = "Open random movie choosing for the users joined from this group";

    public CreateRandom(UserService userService, MessageService messageService, StickerService stickerService, RequestTargetGroup requestTargetGroup) {

        super(NAME, DESCRIPTION, UserService.PitchType.RANDOM, userService, messageService, stickerService,
                requestTargetGroup);
    }

}