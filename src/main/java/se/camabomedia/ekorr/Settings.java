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
    String localAppData = System.getenv("LOCALAPPDATA");
    String appDirectory = "CamaboMediaEkorr";
    File settingsDirectory = new File(localAppData, appDirectory);
    File settingsFile = new File(settingsDirectory, "settings.json");

    public Settings() {
        settingsDirectory.mkdirs();

        if (settingsFile.exists()) {
            try {
                FileReader jsonFile = new FileReader(settingsFile);
                Object obj = new JSONParser().parse(jsonFile);
                settings = (JSONObject) obj;
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            settings = new JSONObject();
        }
    }

    @SuppressWarnings("unchecked")
    public void addProofreaderName(String proofreaderFirstName, String proofreaderLastName) {
        settings.put("proofreaderFirstName", proofreaderFirstName);
        settings.put("proofreaderLastName", proofreaderLastName);
        writeJSON();
    }

    public Hashtable<String, String> getProofreaderName() {
        String firstName = null;
        String lastName = null;
        Hashtable<String, String> name = null;

        if (!settingsFile.exists()) {
            return name;
        }

        try {
            name = new Hashtable<>();
            name.put("firstName", (String) settings.get("proofreaderFirstName"));
            name.put("lastName", (String) settings.get("proofreaderLastName"));
        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }

        return name;
    }

    @SuppressWarnings("unchecked")
    public void addLastProject(String ncc, String title, String author, String ID) {
        settings.put("lastProjectNcc", ncc);
        settings.put("lastProjectTitle", title);
        settings.put("lastProjectAuthor", author);
        settings.put("lastProjectID", ID);
        writeJSON();
    }

    public Hashtable<String, String> getLastProject() {
        String lastProjectNcc = null;
        String lastProjectTitle = null;
        String lastProjectAuthor = null;
        String lastProjectID = null;
        Hashtable<String, String> lastProject = null;

        if (!settingsFile.exists()) {
            return lastProject;
        }

        try {
           lastProject = new Hashtable<>();
           lastProject.put("lastProjectNcc", (String) settings.get("lastProjectNcc"));
           lastProject.put("lastProjectTitle", (String) settings.get("lastProjectTitle"));
           lastProject.put("lastProjectAuthor", (String) settings.get("lastProjectAuthor"));
           lastProject.put("lastProjectID", (String) settings.get("lastProjectID"));
        }
        catch (NullPointerException ignore) {
            return lastProject;
        }

        return lastProject;
    }

    private void writeJSON() {
        try (FileWriter file = new FileWriter(settingsFile)) {
            file.write(settings.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
