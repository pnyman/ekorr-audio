package se.camabomedia.ekorr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Settings
 */
public class Settings {

    JSONObject settings = new JSONObject();
    JSONParser jsonParser = new JSONParser();
    String localAppData = System.getenv("LOCALAPPDATA");
    String appDirectory = "CamaboMediaEkorr";
    File settingsDirectory = new File(localAppData, appDirectory);
    File settingsFile = new File(settingsDirectory, "settings.json");

    public Settings() {
        settingsDirectory.mkdirs();
    }

    @SuppressWarnings("unchecked")
    public void addProofreaderName(String proofreaderFirstName, String proofreaderLastName) {
        settings.put("proofreaderFirstName", proofreaderFirstName);
        settings.put("proofreaderLastName", proofreaderLastName);
        writeJSON();
    }

    private void writeJSON() {
        try (FileWriter file = new FileWriter(settingsFile)) {
            file.write(settings.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Hashtable<String, String> getProofreaderName() throws FileNotFoundException {
        String firstName = null;
        String lastName = null;
        Hashtable<String, String> name = null;

        try {
            FileReader jsonFile = new FileReader(settingsFile);
            Object obj = new JSONParser().parse(jsonFile);
            JSONObject jo = (JSONObject) obj;
            firstName = (String) jo.get("proofreaderFirstName");
            lastName = (String) jo.get("proofreaderLastName");
            name = new Hashtable<>();
            name.put("firstName", firstName);
            name.put("lastName", lastName);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        // catch (FileNotFoundException e) {
        //     e.printStackTrace();
        // }
        catch (IOException e) {
            e.printStackTrace();
        }

        return name;
    }

}
