package com.jozard.secretmoviebot;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class StickerService {

    public static final String SHALOM_STICKER_ID = "CAACAgIAAxkBAAEHlGBj3XodZzDxL0FiJNqvmqren-Rm1gACMwADLS3qC0H7YLyagH9ILgQ";
    public static final String ESCUZMI_STICKER_ID = "CAACAgUAAxkBAAEINxtkGM13CzaThwhv22DRKzJfTuXxUgACYgIAAnCZOFecbpQSD3AavS8E";
    public static final String NOOOO_STICKER_ID = "CAACAgUAAxkBAAEINx1kGM19RIT9DZRc4k7XXuelsE-RbwACdwEAAm6CQFd7GjMVdQOPuS8E";
    public static final String NA_SVYAZI_STICKER_ID = "CAACAgIAAxkBAAEHlHFj3YQxott-yPhbXwF6v1KbcKASbwACyAADMyQAAQxWdipbUAM9wi4E";
    public static final String NO_KIPESH_STICKER_ID = "CAACAgIAAxkBAAEHnlNj4YAcKk3hFdmSvyMMfLyPzK-_twACBwADC_VBCcoT-sqU7g7mLgQ";

    // stickerFileId is received from @idstickerbot bot
    public void sendSticker(final AbsSender sender, Long chatId, String stickerFileId) {
        // Create an InputFile containing Sticker's file_id or URL
        InputFile stickerFile = new InputFile(stickerFileId);
        // Create a SendSticker object using the chatId and stickerFile
        SendSticker sticker = new SendSticker(chatId.toString(), stickerFile);
        try {  // Execute the method
            sender.execute(sticker);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
