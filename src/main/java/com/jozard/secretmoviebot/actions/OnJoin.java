package com.jozard.secretmoviebot.actions;

import com.jozard.secretmoviebot.MessageService;
import com.jozard.secretmoviebot.StickerService;
import com.jozard.secretmoviebot.users.UserService;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.MessageFormat;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.util.StringUtils.capitalize;

@Component
public class OnJoin extends Action {

    public static final String WAIT_FOR_THE_START_MESSAGE = "{0}, you have already joined the pitch. Wait for the start.";
    public static final String WAIT_FOR_THE_START_MESSAGE_ESCAPED = "{0}, you have already joined the pitch\\. Wait for the start\\.";
    private static final String[] HELLO_EMOJIS = {":thumbsup:", ":wave:", ":clap:", ":splayed_hand:", ":rock_on:", ":popcorn:"};
    private final UserService userService;
    private final StickerService stickerService;

    private final MessageService messageService;

    public OnJoin(UserService userService, StickerService stickerService, MessageService messageService) {
        this.userService = userService;
        this.stickerService = stickerService;
        this.messageService = messageService;
    }

    @Override
    public void execute(AbsSender absSender, User user, long chatId, String[] arguments) {
        if (userService.joined(user, chatId)) {
            try {
                String userMention = MessageFormat.format("[{0}](tg://user?id={1})", user.getFirstName(), user.getId());
                if (arguments == null || arguments.length == 0) {
                    messageService.send(absSender, chatId,
                            MessageFormat.format(WAIT_FOR_THE_START_MESSAGE_ESCAPED, userMention),
                            MessageService.MARKDOWN_V2);
                } else {
                    sendAnswerCallbackQuery(absSender, user.getFirstName(), arguments[0]);
                    return;
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            String message = MessageFormat.format("User {0} joined from chat {1}",
                    user.getUserName() == null ? user.getFirstName() : user.getUserName(), chatId);
            System.out.println(message);
            try {
                userService.add(chatId, user);
                messageService.send(absSender, chatId, EmojiParser.parseToUnicode(
                        MessageFormat.format("*{0}* joined current movie choosing {1}", capitalize(user.getFirstName()),
                                HELLO_EMOJIS[ThreadLocalRandom.current().nextInt(0, HELLO_EMOJIS.length)])));
            } catch (IllegalArgumentException e) {
                messageService.send(absSender, chatId, EmojiParser.parseToUnicode(
                        "Movie choosing is not registered in this chat. Use the /start command."));
            }


        }

    }

    private void sendAnswerCallbackQuery(AbsSender absSender, String userName, String callbackQueryId) throws TelegramApiException {
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
        answerCallbackQuery.setCallbackQueryId(callbackQueryId);
        answerCallbackQuery.setShowAlert(false);
        answerCallbackQuery.setText(MessageFormat.format(WAIT_FOR_THE_START_MESSAGE, userName));
        absSender.execute(answerCallbackQuery);
    }
}
