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
            With this command you can switch on/off movie description (by default is switched on) for current movie choosing.
            It should be invoked  before calling any create command.
            If description is switched on, in addition to the movie title, I will ask all participants to send me selected movie summary before voting starts.
            This command has no effect after a create_... command was called in case of create_random option is selected.""";

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