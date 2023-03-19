package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.actions.JoinUser;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
public class Join extends GroupCommand {

    public static final String NAME = "join";
    public static final String DESCRIPTION = """
            With this command you can join the movie choosing in a group chat.
            After joining wait until the pitch admin starts the pitch.""";
    private final JoinUser joinUser;

    public Join(UserService userService, JoinUser joinUser) {
        super(userService, NAME, DESCRIPTION);
        this.joinUser = joinUser;
    }

    @Override
    public void doExecute(AbsSender absSender, User user, Chat chat, String[] strings) {
        this.joinUser.execute(absSender, user, chat.getId(), null);
    }
}