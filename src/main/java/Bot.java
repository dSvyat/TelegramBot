
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;

/**
 * Variables like currentChatID, userChatMap and everything else related to user's chat have to be implemented for 2 reasons: <br>
 * 1. Simplifies code reading, imagine reading update.getMessage().getChatId() instead of simple currentChatId.<br>
 * 2. Make possible conversation with chatGPT and solve the problem when chatGPT perceive all user chats as one chat,
 * which can cause security problems and misunderstanding from chatGPT related to user questions
 */
public class Bot extends TelegramLongPollingBot {
    private final long feedbackChatID = 384164240;
    Long currentChatId;
    Map<Long, Service> userChatMap = new HashMap<>(); //Long - User chat ID, Service - Instance of Service class related to user.
    SendChatAction ChatAction = new SendChatAction();
    SendMessage message = new SendMessage();
    SendMessage tmpMessage = new SendMessage();

    /**
     *
     * @param update message from user <br>
     * Method is being called each time Bot receives new message from user, that's why Map of users chats is necessary. Each user has own instance of Service class, as a result own chat history.
     */
    @Override
    public void onUpdateReceived(Update update){
        currentChatId = update.getMessage().getChatId();
        ChatAction.setChatId(currentChatId);
        message.setChatId(currentChatId.toString());
        tmpMessage.setChatId(currentChatId.toString());
        String receivedText = update.getMessage().getText();
        String answer;

        //if user's chat ID is not in the map, we add it
        if (!userChatMap.containsKey(currentChatId)) {
            userChatMap.put(currentChatId, new Service());
        }

        //start of message processing
        //if message is text
        if (update.hasMessage() && update.getMessage().hasText()) {
            //sending message to user that bot now works as a translator and set variable Translating to true.
            if (receivedText.equals("/translator")){
                if (currentUser().isTranslating()){
                    tmpMessage.setText("I am already translator!");
                }
                else {
                    tmpMessage.setText("I am now translator.\nCurrently, only translation to English is supported.");
                }
                currentUser().setTranslating(true);
                try {
                    execute(tmpMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
            //calling back chatGPT and set boolean variable Translating to false
            else if (receivedText.equals("/chatgpt")) {
                if (!currentUser().isTranslating()){
                    tmpMessage.setText("I am already chatGPT!");
                }
                else {
                    tmpMessage.setText("I am again chatGPT, ask me anything.");
                }
                currentUser().setTranslating(false);
                try {
                    execute(tmpMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
            //deleting user's chat history
            else if (receivedText.equals("/new")){
                currentUser().setTranslating(false);
                currentUser().clearChat();
                tmpMessage.setText("Old memories are disappearing like Master Oogway... ");
                try {
                    execute(tmpMessage);
                } catch (TelegramApiException e){
                    e.printStackTrace();
                }
            }
            else if (receivedText.equals("/feedback")){
                currentUser().setGivingFeedback(true);
                tmpMessage.setText("Write your feedback:");
                try {
                    execute(tmpMessage);
                } catch (TelegramApiException e){
                    e.printStackTrace();
                }
            }
            else if (receivedText.equals("/start") || receivedText.equals("/info")){
                tmpMessage.setText("""
                        Who am I?
                        I am your personal assistant, ChatGPT. You've probably heard about me. I can understand regular text, or voice messages.

                        What can i do for you?
                        I can assist you with anything related to text or translation.

                        Why do I have to use the "Translator" instead of just asking you to translate something?
                        Technically, you can use me as a translator. However, when you use the
                        "/translator" command, the text is processed with the help of DeepL Translate, an AI model specifically designed for translation. This ensures a higher accuracy in the translations.

                        Is my data being processed or shared in any way that compromises my privacy?
                        No, your data is completely private. I do not store our chats, and they are not transferred to any third parties.

                        Can I use you for free?
                        Yes, in the current version, the ChatGPT bot is absolutely free. Subscription or any other form of monetization will only be introduced if the bills for using the OpenAI API (ChatGPT and Whisper for voice-to-text translation) and DeepL Translate API become too high for the author to bear. Both of these APIs are paid, so please use "/new" each time you are done with a specific conversation topic. This will reduce the amount of information processed and, as a result, lower the bills.

                        Also, you can  provide your ideas, bugs you have encouraged etc.
                        using /feedback command.""");
                try{
                    execute(tmpMessage);
                } catch (TelegramApiException e){
                    e.printStackTrace();
                }
            }
            else{
                ChatAction.setAction(ActionType.TYPING);
                try {
                    execute(ChatAction);
                    if (currentUser().isGivingFeedback()) {
                        message.setChatId(feedbackChatID);
                        answer = receivedText + "\n@" + update.getMessage().getFrom().getUserName() + "\n#feedback\n" + update.getMessage().getChatId();
                        currentUser().setGivingFeedback(false);
                        tmpMessage.setText("Feedback received.\nNow you can use me again as a ChatGPT.");
                        execute(tmpMessage);
                    } else if (currentUser().isTranslating()) {
                        answer = Translator.translate(receivedText, "en-US");
                    } else {
                        answer = currentUser().chatGPT(receivedText);
                    }
                    message.setText(answer);
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }

        // if message is audio
        else if (update.hasMessage() && update.getMessage().hasVoice()) {
            Voice voice = update.getMessage().getVoice();
            ChatAction.setAction(ActionType.TYPING);
            try {
                java.io.File file = currentUser().convertToMP3(downloadAudio(getFilePath(voice)));
                execute(ChatAction);
                if (currentUser().isTranslating()){
                    answer = Translator.translate(currentUser().whisperAI(file), "en-US");
                } else{
                    answer = currentUser().chatGPTWithWhisper(file);
                }
                message.setText(answer);
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    //end of message processing

    public String getFilePath(Voice voice) {
        GetFile getFile = new GetFile();
        getFile.setFileId(voice.getFileId());

        try {
            File file = execute(getFile);
            return file.getFilePath();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    public java.io.File downloadAudio(String filePath) {
        try {
            return downloadFile(filePath);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getBotUsername() {
        return "ChatGPT";
    }

    @Override
    public String getBotToken() {
        return ""; //put your api here
    }

    public Service currentUser() {
        return userChatMap.get(currentChatId);
    }
}
