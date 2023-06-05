import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Service {
    private boolean givingFeedback;
    private boolean Translating;
    private JSONObject jsonObject = new JSONObject();
    private JSONArray jsonArray = new JSONArray();
    String apiKey = ""; // put your API key here
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new Bot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param prompt a String putted in Json object and later proceeds by chatGPT
     * @return chatGPT answer<br><br>
     * First of all, we have to create proper request to OpenAI servers. If we want to have conversation with ChatGPT, we have to
     * put into outgoing Json all of our previous messages. I created Json array, which holds each prompt-answer and is being putted into outgoing json.
     */
    public String chatGPT(String prompt) {
        String answer;
        String model = "gpt-3.5-turbo";
        OkHttpClient client = new OkHttpClient.Builder().readTimeout(200, TimeUnit.SECONDS).build();

        MediaType mediaType = MediaType.parse("application/json");

        jsonArray.put(new JSONObject()
                .put("role", "user")
                .put("content", prompt));

        jsonObject.put("messages", jsonArray)
                .put("max_tokens", 1000)
                .put("model", model);

        RequestBody body = RequestBody.create(jsonObject.toString(), mediaType);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(body)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try {

            Response response = client.newCall(request).execute();
            String responseData = response.body().string();
            JSONObject jsonObject1 = new JSONObject(responseData);
            JSONArray choicesArray = jsonObject1.getJSONArray("choices");
            answer = choicesArray.getJSONObject(0).getJSONObject("message").getString("content");
            jsonArray.put(new JSONObject()
                    .put("role", "assistant")
                    .put("content", answer));
            return answer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *
     * @param file mp3, mp4, mpeg, mpga, m4a, wav, or webm audio file. I used mp3
     * @return WhisperAI answer <br><br>
     *General method body and construction is quite similar to chatGPT method, the only difference is how RequestBody being created
     */
    public String whisperAI(File file) {
        String apiKey = ""//replace with your APi;
        String answer;
        String model = "whisper-1";

        OkHttpClient client = new OkHttpClient.Builder().readTimeout(200, TimeUnit.SECONDS).build();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(file, MediaType.parse("audio/mpeg")))
                .addFormDataPart("model", model)
                .build();
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/audio/transcriptions")
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();
        try {
            Response response = client.newCall(request).execute();
            String responseData = response.body().string();
            JSONObject jsonObject = new JSONObject(responseData);
            answer = jsonObject.getString("text");
            System.out.println(answer);
            return answer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *
     * Method's name says all
     */
    public String chatGPTWithWhisper(File file) {
        return chatGPT(whisperAI(file));
    }

    /**
     *
     * @param inputFile has to be audio file<br><br>
     * Method creates temporal file with .mp3 extension using ffmpeg.
     * Before using make sure ffmpeg is set as system path variable
     */
    public File convertToMP3(File inputFile) {
        String outputFilePath = inputFile.getAbsolutePath().replace(".tmp", ".mp3");
        ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-i", inputFile.getAbsolutePath(), "-codec:a", "libmp3lame", outputFilePath);
        try {
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }


        return new File(outputFilePath);
    }

    /**
     * Needed to delete user's chat history
     */
    public void clearChat() {
        jsonArray.clear();
    }

    public boolean isTranslating() {
        return Translating;
    }

    public void setTranslating(boolean translating) {
        Translating = translating;
    }

    public boolean isGivingFeedback() {
        return givingFeedback;
    }

    public void setGivingFeedback(boolean givingFeedback) {
        this.givingFeedback = givingFeedback;
    }
}
