package com.jozard.secretmoviebot.actions;

import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.users.PitchStateMachine;
import com.jozard.secretmoviebot.users.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import static com.jozard.secretmoviebot.StickerService.NA_SVYAZI_STICKER_ID;

@Component
public class OnGroupSent extends PrivateChatAction {

    private final UserService userService;
    private final StickerService stickerService;

    public OnGroupSent(UserService userService, StickerService stickerService) {
        this.userService = userService;
        this.stickerService = stickerService;
    }

    @Override
    public void execute(AbsSender absSender, PitchStateMachine state, Message message, String[] arguments) {
        this.reply(absSender, message.getChatId(), response -> {
            User user = state.user();
            System.out.println(MessageFormat.format(
                    "There are only groups the user {0} is in JOINED state. We assume the message contains the group name and will look for it",
                    user.getUserName()));
            // Bot is waiting for a group selection. Let's check if the answer matches any group
            List<UserService.Group> userGroups = userService.getAllGroups(user);
            Optional<UserService.Group> targetGroup = userGroups.stream().filter(
                    group -> group.getChatName().equals(message.getText())).findFirst();

            if (targetGroup.isPresent()) {
                state.selectGroup(targetGroup.get().getChatId());
                response.setText(MessageFormat.format("Send me a movie title for the *{0}* group.",
                        targetGroup.get().getChatName()));
                ReplyKeyboardRemove keyboardRemove = ReplyKeyboardRemove.builder().removeKeyboard(true).build();
                response.setReplyMarkup(keyboardRemove);
                // update state machine for this user in this group ???

                stickerService.sendSticker(absSender, message.getChatId(), NA_SVYAZI_STICKER_ID);
            } else {
                response.setText(MessageFormat.format(
                        "Group *{0}* not found or movie choosing is not registered in it. Try using reply buttons below.",
                        message.getText()));
            }
            response.enableMarkdown(true);
        });
    }
}
