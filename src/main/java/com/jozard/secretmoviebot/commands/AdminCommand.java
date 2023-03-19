package com.jozard.secretmoviebot.commands;


import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.users.JoinedUser;
import com.jozard.secretmoviebot.users.UserService;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import javax.ws.rs.ForbiddenException;
import java.text.MessageFormat;
import java.util.Optional;

public abstract class AdminCommand extends GroupCommand {

    protected final MessageService messageService;

    public AdminCommand(UserService userService, MessageService messageService, String name, String description) {
        super(userService, name, description);
        this.messageService = messageService;
    }

    public abstract void onCommandAction(AbsSender absSender, Chat chat, User user);

    @Override
    public final void doExecute(AbsSender absSender, User user, Chat chat, String[] strings) {
        long chatId = chat.getId();

        Optional<Boolean> isAdmin = userService.getFromGroup(user, chatId).map(JoinedUser::isAdmin);
        if (isAdmin.isEmpty() || !isAdmin.get()) {
            // only admin can call the command
            messageService.send(absSender, chatId, "Pitch admin only can call this command");
            throw new ForbiddenException(MessageFormat.format("Pitch admin only can call {0}", this.getClass()));
        } else {
            onCommandAction(absSender, chat, user);
        }

    }

}