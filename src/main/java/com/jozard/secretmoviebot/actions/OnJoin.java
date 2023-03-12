package com.jozard.secretmoviebot.actions;

import com.jozard.secretmoviebot.users.UserService;
import com.jozard.secretmoviebot.StickerService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.MessageFormat;

@Component
public class OnJoin extends Action {

    public static final String WAIT_FOR_THE_START_MESSAGE = "{0}, you have already joined the pitch. Wait for the start.";
    public static final String WAIT_FOR_THE_START_MESSAGE_ESCAPED = "{0}, you have already joined the pitch\\. Wait for the start\\.";
    private final UserService userService;
    private final StickerService stickerService;

    public OnJoin(UserService userService, StickerService stickerService) {
        this.userService = userService;
        this.stickerService = stickerService;
    }

    @Override
    public void execute(AbsSender absSender, User user, long chatId, String[] arguments) {
        SendMessage response = new SendMessage();
        boolean sendSticker = false;
        response.enableMarkdown(true);
        if (userService.joined(user, chatId)) {
            try {
                String userMention = MessageFormat.format("[{0}](tg://user?id={1})", user.getFirstName(), user.getId());
                if (arguments == null || arguments.length == 0) {
                    response.setParseMode("MarkdownV2");
                    response.setText(MessageFormat.format(WAIT_FOR_THE_START_MESSAGE_ESCAPED, userMention));
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
            sendSticker = true;
            try {
                userService.add(chatId, user);
                response.setText(MessageFormat.format("*{0}* joined current movie choosing",
                        user.getUserName() == null ? user.getFirstName() : user.getUserName()));
            } catch (IllegalArgumentException e) {
                sendSticker = false;
                response.setText("Movie choosing is not registered in this chat. Use the /start command.");
            }


        }

        response.setChatId(String.valueOf(chatId));

        try {
            absSender.execute(response);
            if (sendSticker) {
                stickerService.sendSticker(absSender, chatId, StickerService.SHALOM_STICKER_ID);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
