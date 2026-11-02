package se.camabomedia.ekorraudio.ui;

import java.io.File;

import javafx.scene.text.Font;

/**
 * Typsnitt som är installerade bara för den inloggade användaren.
 *
 * Windows 10/11 installerar typsnitt i %LOCALAPPDATA%\Microsoft\Windows\Fonts
 * när man väljer "Installera" (inte "Installera för alla användare"). JavaFX
 * letar bara i C:\Windows\Fonts, så de typsnitten syns varken i
 * typsnittsväljaren eller i texten. Här läses de in för hand – därefter finns
 * de i Font.getFamilies() och kan användas med sitt familjenamn.
 */
public final class UserFonts {

    private UserFonts() {
    }

    /** Läser in användarens typsnitt. Anropas en gång, tidigt vid start. */
    public static void load() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null) {
            return;
        }
        File dir = new File(localAppData, "Microsoft\\Windows\\Fonts");
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        int loaded = 0;
        for (File f : files) {
            String name = f.getName().toLowerCase();
            try {
                String url = f.toURI().toString();
                if (name.endsWith(".ttc")) {
                    // Samling med flera typsnitt i en fil
                    Font[] fonts = Font.loadFonts(url, 12);
                    loaded += fonts == null ? 0 : fonts.length;
                }
                else if (name.endsWith(".ttf") || name.endsWith(".otf")) {
                    if (Font.loadFont(url, 12) != null) {
                        loaded++;
                    }
                }
            }
            catch (RuntimeException e) {
                // Ett trasigt eller okänt typsnitt ska inte hindra starten
                System.err.println("Kunde inte läsa typsnittet " + f + ": " + e.getMessage());
            }
        }
        if (loaded > 0) {
            System.err.println("Läste in " + loaded + " typsnitt från " + dir);
        }
    }
}
