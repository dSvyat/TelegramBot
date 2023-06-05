import com.deepl.api.*;
public class Translator {
    private static final String authKey = "f63c02c5-f056-...";  // Replace with your key
    private static com.deepl.api.Translator translator = new com.deepl.api.Translator(authKey);

    public static String translate(String text, String language){
        try {
            TextResult result = translator.translateText(text, null, language);
            return result.getText();
        } catch (DeepLException | InterruptedException e){
            e.printStackTrace();
        }
        return null;
    }
}
