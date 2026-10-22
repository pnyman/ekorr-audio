package se.camabomedia.ekorraudio.ui;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;

import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * Färgar fönstrets namnlist efter temat. Namnlisten ritas av Windows, inte av
 * JavaFX, så det görs via DwmSetWindowAttribute i dwmapi.dll (med JNA).
 *
 *  - Windows 11: valfri färg på namnlist, text och ram.
 *  - Windows 10 (version 1809 och senare): bara ljus eller mörk namnlist.
 *  - Äldre Windows och andra system: ingenting händer.
 *
 * Alla fel fångas – namnlisten är kosmetisk och får aldrig stoppa programmet.
 */
public final class TitleBar {

    private static final boolean WINDOWS =
        System.getProperty("os.name", "").toLowerCase().startsWith("windows");

    private TitleBar() {
    }

    /** Färgar namnlisten nu om fönstret visas, annars när det visas. */
    public static void applyWhenShown(Stage stage, Theme theme) {
        if (!WINDOWS || stage == null) {
            return;
        }
        if (stage.isShowing()) {
            apply(stage, theme);
        }
        else {
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> apply(stage, theme));
        }
    }

    /** Färgar namnlisten på ett fönster som visas. */
    public static void apply(Stage stage, Theme theme) {
        if (!WINDOWS || stage == null || !stage.isShowing()) {
            return;
        }
        try {
            Win.apply(stage, theme);
        }
        catch (Throwable t) {
            // JNA saknas, okänd Windows-version el. dyl. – behåll systemets namnlist
            System.err.println("Kunde inte färga namnlisten: " + t);
        }
    }

    /**
     * Allt som rör JNA ligger här, så att klassen bara laddas på Windows
     * och ett saknat JNA ger ett fel som fångas ovan.
     */
    private static final class Win {

        // DWMWINDOWATTRIBUTE
        private static final int DWMWA_USE_IMMERSIVE_DARK_MODE_OLD = 19;   // Windows 10 1809–1909
        private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;       // Windows 10 2004+, 11
        private static final int DWMWA_BORDER_COLOR = 34;                  // Windows 11
        private static final int DWMWA_CAPTION_COLOR = 35;                 // Windows 11
        private static final int DWMWA_TEXT_COLOR = 36;                    // Windows 11
        private static final int DWMWA_COLOR_DEFAULT = 0xFFFFFFFF;

        private static final int SWP_NOSIZE = 0x0001;
        private static final int SWP_NOMOVE = 0x0002;
        private static final int SWP_NOZORDER = 0x0004;
        private static final int SWP_NOACTIVATE = 0x0010;
        private static final int SWP_FRAMECHANGED = 0x0020;

        interface Dwm extends Library {
            Dwm INSTANCE = Native.load("dwmapi", Dwm.class);

            /** Returnerar 0 (S_OK) om attributet sattes. */
            int DwmSetWindowAttribute(HWND hwnd, int attribute, IntByReference value, int size);
        }

        static void apply(Stage stage, Theme theme) {
            HWND hwnd = findWindow(stage);
            if (hwnd == null) {
                System.err.println("Hittar inte fönstret för namnlisten: " + stage.getTitle());
                return;
            }

            int dark = theme.isDarkTitleBar() ? 1 : 0;
            if (set(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, dark) != 0) {
                set(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_OLD, dark);
            }

            // Bara Windows 11 – på Windows 10 svarar anropen med fel, som ignoreras
            set(hwnd, DWMWA_CAPTION_COLOR, colorRef(theme.getTitleBarColor()));
            set(hwnd, DWMWA_BORDER_COLOR, colorRef(theme.getTitleBarColor()));
            set(hwnd, DWMWA_TEXT_COLOR, colorRef(theme.getTitleBarText()));

            // Windows 10 ritar inte alltid om namnlisten av sig själv
            User32.INSTANCE.SetWindowPos(hwnd, null, 0, 0, 0, 0,
                SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE | SWP_FRAMECHANGED);
        }

        /**
         * JavaFX ger inte ut fönstrets Windows-handtag. Ge fönstret en unik
         * titel en kort stund och leta upp det på den.
         */
        private static HWND findWindow(Stage stage) {
            String original = stage.getTitle();
            String marker = "ekorr-titlebar-" + System.nanoTime();
            try {
                stage.setTitle(marker);
                return User32.INSTANCE.FindWindow(null, marker);
            }
            finally {
                stage.setTitle(original);
            }
        }

        private static int set(HWND hwnd, int attribute, int value) {
            return Dwm.INSTANCE.DwmSetWindowAttribute(hwnd, attribute, new IntByReference(value), 4);
        }

        /** "#rrggbb" -> COLORREF (0x00bbggrr), null -> systemets färg. */
        private static int colorRef(String hex) {
            if (hex == null) {
                return DWMWA_COLOR_DEFAULT;
            }
            int rgb = Integer.parseInt(hex.substring(1), 16);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            return r | (g << 8) | (b << 16);
        }
    }
}
