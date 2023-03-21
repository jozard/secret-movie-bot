package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.MessageFormat;
import java.util.Optional;

@Component
public class ToggleDescription extends AdminCommand {

    public static final String NAME = "toggle_description";
    public static final String DESCRIPTION = """
            Switches on/off movie description (by default is on) and should be invoked  before calling any create command.
            If it is switched on, I will request to send me selected movie summary.
            This command has no effect after any create_... command.""";

    public ToggleDescription(MessageService messageService, UserService userService) {
        super(userService, messageService, NAME, DESCRIPTION);
    }

    @Override
    public void onCommandAction(AbsSender absSender, Chat chat, User user) {
        var chatId = chat.getId();
        Optional<UserService.Group> group = userService.getGroup(chatId);
        if (group.isPresent()) {
            if (group.get().getPitchType() == null) {
                messageService.send(absSender, chatId,
                        MessageFormat.format("Description is switched {0} for registered movie choosing.",
                                group.get().toggleDescription() ? "on" : "off"));
            } else {
                messageService.send(absSender, chatId,
                        "This command should have been called before the pitch created.");
            }
        }
    }

}