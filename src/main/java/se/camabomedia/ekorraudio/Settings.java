package se.camabomedia.ekorraudio;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Settings
 */
public class Settings {

    JSONObject settings = new JSONObject();
    String localAppData = System.getenv("LOCALAPPDATA");
    String appDirectory = "CamaboMediaEkorrAudio";
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
    public void addProofreader(String proofreaderName) {
        settings.put("proofreaderName", proofreaderName);
        writeJSON();
    }

    public String getProofreader() {
        if (!settingsFile.exists()
            || settings.get("proofreaderName") == null) {
            return null;
        }

        return (String) settings.get("proofreaderName");
    }

    @SuppressWarnings("unchecked")
    public void addDefaultLocation(String defaultLocation) {
        settings.put("defaultLocation", defaultLocation);
        writeJSON();
    }

    public String getDefaultLocation() {
        if (!settingsFile.exists()
            || settings.get("defaultLocation") == null) {
            return null;
        }

        return (String) settings.get("defaultLocation");
    }

    @SuppressWarnings("unchecked")
    public void addWantsDefaultLocation(Boolean b) {
        settings.put("wantsDefaultLocation", b);
        writeJSON();
    }

    public boolean getWantsDefaultLocation() {
        if (!settingsFile.exists()
            || settings.get("wantsDefaultLocation") == null) {
            return true;
        }

        return (Boolean) settings.get("wantsDefaultLocation");
    }


    @SuppressWarnings("unchecked")
    public void addPrivateDefaultLocation(String privateDefaultLocation) {
        settings.put("privateDefaultLocation", privateDefaultLocation);
        writeJSON();
    }

    public String getPrivateDefaultLocation() {
        if (!settingsFile.exists()
            || settings.get("privateDefaultLocation") == null) {
            return null;
        }

        return (String) settings.get("privateDefaultLocation");
    }

    // @SuppressWarnings("unchecked")
    // public void addWantsPrivateDefaultLocation(Boolean b) {
    //     settings.put("wantsPrivateDefaultLocation", b);
    //     writeJSON();
    // }

    // public boolean getWantsPrivateDefaultLocation() {
    //     if (!settingsFile.exists()
    //         || settings.get("wantsPrivateDefaultLocation") == null) {
    //         return true;
    //     }

    //     return (Boolean) settings.get("wantsPrivateDefaultLocation");
    // }


    /**
     * Sparar huvudfönstrets layout i ett svep (en skrivning).
     * @param dividers  avdelarnas positioner (0–1)
     * @param bounds    x, y, bredd, höjd – eller null för att behålla tidigare
     *                  (t.ex. när fönstret är maximerat)
     * @param maximized om fönstret är maximerat
     */
    @SuppressWarnings("unchecked")
    public void addLayout(double[] dividers, double[] bounds, boolean maximized) {
        if (dividers != null) {
            settings.put("dividerPositions", join(dividers));
        }
        if (bounds != null) {
            settings.put("windowBounds", join(bounds));
        }
        settings.put("windowMaximized", maximized);
        writeJSON();
    }

    /** x, y, bredd, höjd – eller null. */
    public double[] getWindowBounds() {
        return split((String) settings.get("windowBounds"));
    }

    public boolean getWindowMaximized() {
        return Boolean.TRUE.equals(settings.get("windowMaximized"));
    }

    /** Avdelarnas positioner (0–1) – eller null. */
    public double[] getDividerPositions() {
        return split((String) settings.get("dividerPositions"));
    }

    private static String join(double[] values) {
        StringBuilder sb = new StringBuilder();
        for (double v : values) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(String.format(java.util.Locale.ROOT, "%.4f", v));
        }
        return sb.toString();
    }

    private static double[] split(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            String[] parts = s.split(",");
            double[] values = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                values[i] = Double.parseDouble(parts[i].strip());
            }
            return values;
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    /** Om ljudkurvan ska visas. */
    @SuppressWarnings("unchecked")
    public void addShowWaveform(boolean show) {
        settings.put("showWaveform", show);
        writeJSON();
    }

    /** Om ljudkurvan ska visas (standard: ja). */
    public boolean getShowWaveform() {
        return !Boolean.FALSE.equals(settings.get("showWaveform"));
    }

    /** Om vänster- respektive högerkolumnen ska visas. */
    @SuppressWarnings("unchecked")
    public void addShowSidePanes(boolean left, boolean right) {
        settings.put("showLeftPane", left);
        settings.put("showRightPane", right);
        writeJSON();
    }

    /** Vänsterkolumnen visas (standard: ja). */
    public boolean getShowLeftPane() {
        return !Boolean.FALSE.equals(settings.get("showLeftPane"));
    }

    /** Högerkolumnen visas (standard: ja). */
    public boolean getShowRightPane() {
        return !Boolean.FALSE.equals(settings.get("showRightPane"));
    }

    /** Ljudkurvan: true = fast kurva med rörlig markering, false = rörlig kurva. */
    @SuppressWarnings("unchecked")
    public void addWaveformFixed(boolean fixed) {
        settings.put("waveformFixed", fixed);
        writeJSON();
    }

    /** Fast kurva (standard: nej, kurvan rör sig). */
    public boolean getWaveformFixed() {
        return Boolean.TRUE.equals(settings.get("waveformFixed"));
    }

    /** Färgtema (namnet på Theme-konstanten, t.ex. "SOLARIZED_DARK"). */
    @SuppressWarnings("unchecked")
    public void addTheme(String theme) {
        settings.put("theme", theme);
        writeJSON();
    }

    /** Sparat tema, eller null om inget valts. */
    public String getTheme() {
        Object value = settings.get("theme");
        return value instanceof String ? (String) value : null;
    }

    /** Typsnitt för texten, eller null för bokens eget. */
    @SuppressWarnings("unchecked")
    public void addFontFamily(String family) {
        if (family == null || family.isBlank()) {
            settings.remove("fontFamily");
        }
        else {
            settings.put("fontFamily", family);
        }
        writeJSON();
    }

    /** Valt typsnitt, eller null för bokens eget. */
    public String getFontFamily() {
        Object value = settings.get("fontFamily");
        return value instanceof String ? (String) value : null;
    }

    /** Textstorlek, 1.0 = normal. */
    @SuppressWarnings("unchecked")
    public void addFontScale(double scale) {
        settings.put("fontScale", scale);
        writeJSON();
    }

    /** Sparad textstorlek, eller 0 om ingen finns. */
    public double getFontScale() {
        Object value = settings.get("fontScale");
        return value instanceof Number ? ((Number) value).doubleValue() : 0;
    }

    /** Den högre hastighet (procent) som "-" växlar till. */
    @SuppressWarnings("unchecked")
    public void addFastSpeed(int percent) {
        settings.put("fastSpeed", percent);
        writeJSON();
    }

    /** Sparad högre hastighet i procent, eller 0 om ingen finns. */
    public int getFastSpeed() {
        Object value = settings.get("fastSpeed");
        // json-simple läser tal som Long
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    @SuppressWarnings("unchecked")
    public void addLastProject(String ncc, String title, String author, String ID) {
        settings.put("lastProjectNcc", ncc);
        settings.put("lastProjectTitle", title);
        settings.put("lastProjectAuthor", author);
        settings.put("lastProjectID", ID);
        writeJSON();
    }

    /**
     * Gets data from the json-file about last project.
     * @return Hashtable.
     *         If data about last project is not in the json-file,
     *         the hashtable is returned empty.
     */
    public Hashtable<String, String> getLastProject() {
        Hashtable<String, String> lastProject = new Hashtable<>();

        if (!settingsFile.exists()) {
            return lastProject;
        }

        try {
           lastProject.put("lastProjectNcc", (String) settings.get("lastProjectNcc"));
           lastProject.put("lastProjectTitle", (String) settings.get("lastProjectTitle"));
           lastProject.put("lastProjectAuthor", (String) settings.get("lastProjectAuthor"));
           lastProject.put("lastProjectID", (String) settings.get("lastProjectID"));
        }
        catch (NullPointerException ignore) {}

        return lastProject;
    }

    // ------------------------------------------------------------ senaste böcker

    /** Så många böcker sparas i listan "Senaste". */
    private static final int MAX_RECENT = 10;

    /** En bok i listan över senast öppnade. */
    public static final class RecentBook {
        public final String ncc;
        public final String title;
        /** Position i boken (ms från början) när den lämnades. */
        public final long positionMs;

        RecentBook(String ncc, String title, long positionMs) {
            this.ncc = ncc;
            this.title = title;
            this.positionMs = positionMs;
        }
    }

    /**
     * Lägger boken först i listan över senaste böcker, med positionen
     * man är på. Finns den redan flyttas den upp.
     */
    @SuppressWarnings("unchecked")
    public void addRecentBook(String ncc, String title, long positionMs) {
        if (ncc == null) {
            return;
        }
        JSONArray list = new JSONArray();
        JSONObject entry = new JSONObject();
        entry.put("ncc", ncc);
        entry.put("title", title == null ? "" : title);
        entry.put("position", Math.max(0, positionMs));
        list.add(entry);
        for (RecentBook b : getRecentBooks()) {
            if (!b.ncc.equalsIgnoreCase(ncc) && list.size() < MAX_RECENT) {
                JSONObject e = new JSONObject();
                e.put("ncc", b.ncc);
                e.put("title", b.title);
                e.put("position", b.positionMs);
                list.add(e);
            }
        }
        settings.put("recentBooks", list);
        writeJSON();
    }

    /** Tar bort en bok ur listan (t.ex. om den inte finns längre). */
    @SuppressWarnings("unchecked")
    public void removeRecentBook(String ncc) {
        JSONArray list = new JSONArray();
        for (RecentBook b : getRecentBooks()) {
            if (!b.ncc.equalsIgnoreCase(ncc)) {
                JSONObject e = new JSONObject();
                e.put("ncc", b.ncc);
                e.put("title", b.title);
                e.put("position", b.positionMs);
                list.add(e);
            }
        }
        settings.put("recentBooks", list);
        writeJSON();
    }

    /** De senaste böckerna, senast använda först. */
    public java.util.List<RecentBook> getRecentBooks() {
        java.util.List<RecentBook> books = new java.util.ArrayList<>();
        Object value = settings.get("recentBooks");
        if (!(value instanceof JSONArray)) {
            return books;
        }
        for (Object o : (JSONArray) value) {
            if (!(o instanceof JSONObject)) {
                continue;
            }
            JSONObject e = (JSONObject) o;
            Object ncc = e.get("ncc");
            Object title = e.get("title");
            Object pos = e.get("position");
            if (ncc instanceof String) {
                books.add(new RecentBook((String) ncc,
                                         title instanceof String ? (String) title : "",
                                         pos instanceof Number ? ((Number) pos).longValue() : 0));
            }
        }
        return books;
    }

    /** Sparad position (ms) för boken, eller 0 om den inte finns i listan. */
    public long getBookPosition(String ncc) {
        for (RecentBook b : getRecentBooks()) {
            if (b.ncc.equalsIgnoreCase(ncc)) {
                return b.positionMs;
            }
        }
        return 0;
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
