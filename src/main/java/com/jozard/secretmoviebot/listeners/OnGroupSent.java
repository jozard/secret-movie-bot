package com.jozard.secretmoviebot.listeners;

import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.actions.RequestMovie;
import com.jozard.secretmoviebot.actions.RequestTargetGroup;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import static com.jozard.secretmoviebot.StickerService.NA_SVYAZI_STICKER_ID;

@Component
public class OnGroupSent extends PrivateChatListener {

    private final StickerService stickerService;
    private final RequestMovie requestMovie;
    private final RequestTargetGroup requestTargetGroup;

    public OnGroupSent(MessageService messageService, UserService userService, StickerService stickerService, RequestMovie requestMovie, RequestTargetGroup requestTargetGroup) {
        super(messageService, userService);
        this.stickerService = stickerService;
        this.requestMovie = requestMovie;
        this.requestTargetGroup = requestTargetGroup;
    }

    @Override
    public void doExecute(AbsSender absSender, PitchStateMachine state, Message message, String[] arguments) {
        User user = state.user();
        System.out.println(MessageFormat.format(
                "There are only groups the user {0} is in JOINED state. We assume the message contains the group name and will look for it",
                user.getId()));
        // Bot is waiting for a group selection. Let's check if the answer matches any group
        List<UserService.Group> userGroups = userService.getAllGroups(user);
        Optional<UserService.Group> targetGroup = userGroups.stream().filter(
                group -> group.getChatName().equals(message.getText())).findFirst();

        if (targetGroup.isPresent()) {
            state.selectGroup(targetGroup.get());
            targetGroup.get().cleanMovie(user);
            stickerService.sendSticker(absSender, message.getChatId(), NA_SVYAZI_STICKER_ID);
            requestMovie.execute(absSender, user, message.getChatId(), new String[]{targetGroup.get().getChatName()});
            // update state machine for this user in this group ???

        } else {
            messageService.send(absSender, message.getChatId(),
                    MessageFormat.format("Group *{0}* not found or movie choosing is not registered in it.",
                            message.getText()));
            requestTargetGroup.execute(absSender, user, message.getChatId(), null);
        }
    }

}
