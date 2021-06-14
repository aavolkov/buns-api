package com.example.buns.service;

import com.example.buns.dal.entity.TypeSubscribe;
import com.example.buns.rest.model.Subscriber;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.ExportChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TelegramBotHandler.
 *
 * @author avolkov
 */

@Component
@Data
@Slf4j
public class TelegramBotHandler extends TelegramLongPollingBot {

    private final String INFO_LABEL = "О чем канал?";
    private final String ACCESS_LABEL = "Как получить доступ?";
    private final String SUCCESS_LABEL = "Дайте полный доступ!";
    private final String DEMO_LABEL = "Хочу демо-доступ на 3 дня";

    private final SubscribersService subscribersService;


    private enum COMMANDS {
        INFO("/info"),
        START("/start"),
        DEMO("/demo"),
        ACCESS("/access"),
        SUCCESS("/success");

        private String command;

        COMMANDS(String command) {
            this.command = command;
        }

        public String getCommand() {
            return command;
        }
    }

    @Value("${telegram.support.chat-id}")
    private String supportChatId;

    @Value("${telegram.name}")
    private String name;

    @Value("${telegram.token}")
    private String token;

    @Value("${telegram.chanel-id}")
    private String privateChannelId;

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chat_id = update.getMessage().getChatId();

            try {
                Pattern patternTimeCommand = Pattern.compile("^message\\splease$");
                Pattern patternClear = Pattern.compile("^clear-expired$");
                Pattern patternGiveRights = Pattern.compile("^give-rights\\s(\\d*)$");

                Matcher matcherTimeCommand = patternTimeCommand.matcher(text);
                Matcher matcherClear = patternClear.matcher(text);
                Matcher matcherGiveRights = patternGiveRights.matcher(text);


                if (matcherTimeCommand.find()) {
                    sendInfoToSupport("Пользователь запросил полный доступ:\n" +
                            "\nLogin: @" + update.getMessage().getFrom().getUserName() +
                            "\nName: " + update.getMessage().getFrom().getFirstName() + " " + update.getMessage().getFrom().getLastName() +
                            "\nChat ID: [" + chat_id + "](" + chat_id + ")");

                    SendMessage messageSuccess = new SendMessage();
                    messageSuccess.setText("Ваши данные получены. Идет проверка.");
                    messageSuccess.setChatId(String.valueOf(chat_id));
                    execute(messageSuccess);
                } else if (matcherClear.find() && isAdmin(String.valueOf(chat_id))) {
                    clearExpired();
                } else if (matcherGiveRights.find() && isAdmin(String.valueOf(chat_id))) {
                    giveRights(matcherGiveRights.group(1));
                } else {
                    SendMessage message = getCommandResponse(text, update.getMessage().getFrom(), String.valueOf(chat_id));
                    message.enableHtml(true);
                    message.setParseMode(ParseMode.HTML);
                    message.setChatId(String.valueOf(chat_id));
                    execute(message);
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
                SendMessage message = handleNotFoundCommand();
                message.setChatId(String.valueOf(chat_id));
                try {
                    sendInfoToSupport("Error " + e.getMessage());

                    execute(message);
                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                }
            }
        } else if (update.hasCallbackQuery()) {
            try {
                SendMessage message = getCommandResponse(update.getCallbackQuery().getData(), update.getCallbackQuery().getFrom(), String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
                message.enableHtml(true);
                message.setParseMode(ParseMode.HTML);
                message.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
                try {
                    sendInfoToSupport("Error " + e.getMessage());
                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private boolean isAdmin(String chatId) {
        return chatId.equals(supportChatId);
    }

    private void clearExpired() throws TelegramApiException {
        List<Subscriber> subscribers = subscribersService.getExpired();
        List<Subscriber> successDeleted = new ArrayList<>();

        for (Subscriber subscriber : subscribers) {
            try {
                KickChatMember kickChatMember = new KickChatMember();
                kickChatMember.setChatId(privateChannelId);
                kickChatMember.setUserId(Integer.valueOf(subscriber.getTelegramId()));
                execute(kickChatMember);

                subscribersService.disable(subscriber.getId());

                SendMessage message = new SendMessage();
                message.setText("Ваш доступ к каналу окончен");
                message.setChatId(subscriber.getTelegramId());
                execute(message);

                successDeleted.add(subscriber);

                Thread.sleep(100);
            } catch (Exception ex) {
                ex.printStackTrace();
                sendInfoToSupport("Ошибка при удалении \nChat ID = " + subscriber.getTelegramId() + "\nID = " + subscriber.getId() + "\n" + ex.getMessage());
            }
        }
    }

    private void giveRights(String chatId) throws TelegramApiException {

        try {
            UnbanChatMember unbanChatMember = new UnbanChatMember();
            unbanChatMember.setChatId(privateChannelId);
            unbanChatMember.setOnlyIfBanned(false);
            unbanChatMember.setUserId(Integer.valueOf(chatId));

            execute(unbanChatMember);
        } catch (TelegramApiException e) {
            try {
                sendInfoToSupport("Ощибка при удалении пользователя из бана: " + e.getMessage());
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }
        }

        addInfoSubscriberToDb(null, chatId, null, TypeSubscribe.FULL);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Вам выдан полный доступ: " + getChatInviteLink());
        execute(message);

        sendInfoToSupport("Выдан полный доступ для пользователя " + chatId);
    }

    private void sendInfoToSupport(String message) throws TelegramApiException {
        SendMessage messageSupport = new SendMessage();
        messageSupport.setText(message);
        messageSupport.setChatId(supportChatId);

        execute(messageSupport);
    }

    private SendMessage getCommandResponse(String text, User user, String chatId) throws TelegramApiException {
        if (text.equals(COMMANDS.INFO.getCommand())) {
            return handleInfoCommand();
        }

        if (text.equals(COMMANDS.ACCESS.getCommand())) {
            return handleAccessCommand();
        }

        if (text.equals(COMMANDS.SUCCESS.getCommand())) {
            return handleSuccessCommand();
        }

        if (text.equals(COMMANDS.START.getCommand())) {
            return handleStartCommand();
        }

        if (text.equals(COMMANDS.DEMO.getCommand())) {
            return handleDemoCommand(user.getUserName(), String.valueOf(user.getId()), user.getFirstName(), chatId);
        }

        return handleNotFoundCommand();
    }

    private SendMessage handleNotFoundCommand() {
        SendMessage message = new SendMessage();
        message.setText("Вы что-то сделали не так. Выберите команду:");
        message.setReplyMarkup(getKeyboard());
        return message;
    }

    private String getChatInviteLink() throws TelegramApiException {
        ExportChatInviteLink exportChatInviteLink = new ExportChatInviteLink();
        exportChatInviteLink.setChatId(privateChannelId);
        return execute(exportChatInviteLink);
    }

    private SendMessage handleDemoCommand(String username, String id, String name, String chatId) throws TelegramApiException {
        SendMessage message = new SendMessage();

        if (subscribersService.isDemoAccess(chatId)) {
            message.setText("Ссылка для доступа к закрытому каналу: " + getChatInviteLink() + " \nЧерез 3 дня вы будете исключены из канала");

            addInfoSubscriberToDb(username, chatId, name, TypeSubscribe.DEMO);
        } else {
            message.setText("Вы уже получали демо-доступ");
        }

        message.setReplyMarkup(getKeyboard());

        return message;
    }

    private Subscriber addInfoSubscriberToDb(String username, String chatId, String name,
                                             TypeSubscribe typeSubscribe) {
        Subscriber subscriber = new Subscriber();
        subscriber.setTypeSubscribe(typeSubscribe);
        subscriber.setTelegramId(chatId);
        subscriber.setName(name);
        subscriber.setLogin(username);
        subscriber.setStartDate(LocalDateTime.now());

        return subscribersService.add(subscriber);
    }

    private SendMessage handleStartCommand() {
        SendMessage message = new SendMessage();
        message.setText("Доступные команды:");
        message.setReplyMarkup(getKeyboard());
        return message;
    }

    private SendMessage handleInfoCommand() {
        SendMessage message = new SendMessage();
        message.setText("Это канал о самых вкусных пирожочках");
        message.setReplyMarkup(getKeyboard());
        return message;
    }

    private SendMessage handleAccessCommand() {
        SendMessage message = new SendMessage();
        message.setText("Чтобы получить полный доступ, вам надо сказать волшебное слово. Отправьте следующий текст: message please");
        message.setReplyMarkup(getKeyboard());
        return message;
    }

    private SendMessage handleSuccessCommand() {
        SendMessage message = new SendMessage();
        message.setText("После проверки вам выдадут полный доступ");
        message.setReplyMarkup(getKeyboard());
        return message;
    }

    private InlineKeyboardMarkup getKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setText(INFO_LABEL);
        inlineKeyboardButton.setCallbackData(COMMANDS.INFO.getCommand());

        InlineKeyboardButton inlineKeyboardButtonAccess = new InlineKeyboardButton();
        inlineKeyboardButtonAccess.setText(ACCESS_LABEL);
        inlineKeyboardButtonAccess.setCallbackData(COMMANDS.ACCESS.getCommand());

        InlineKeyboardButton inlineKeyboardButtonDemo = new InlineKeyboardButton();
        inlineKeyboardButtonDemo.setText(DEMO_LABEL);
        inlineKeyboardButtonDemo.setCallbackData(COMMANDS.DEMO.getCommand());

        InlineKeyboardButton inlineKeyboardButtonSuccess = new InlineKeyboardButton();
        inlineKeyboardButtonSuccess.setText(SUCCESS_LABEL);
        inlineKeyboardButtonSuccess.setCallbackData(COMMANDS.SUCCESS.getCommand());

        List<List<InlineKeyboardButton>> keyboardButtons = new ArrayList<>();

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(inlineKeyboardButton);
        keyboardButtonsRow1.add(inlineKeyboardButtonAccess);

        List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();
        keyboardButtonsRow2.add(inlineKeyboardButtonSuccess);

        List<InlineKeyboardButton> keyboardButtonsRow3 = new ArrayList<>();
        keyboardButtonsRow3.add(inlineKeyboardButtonDemo);

        keyboardButtons.add(keyboardButtonsRow1);
        keyboardButtons.add(keyboardButtonsRow3);
        keyboardButtons.add(keyboardButtonsRow2);

        inlineKeyboardMarkup.setKeyboard(keyboardButtons);

        return inlineKeyboardMarkup;
    }
}
