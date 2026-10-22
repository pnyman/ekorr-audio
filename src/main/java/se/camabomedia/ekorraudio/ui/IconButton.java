package se.camabomedia.ekorraudio.ui;

import org.kordamp.ikonli.javafx.FontIcon;

import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/**
 * Platta knappar med en ikon (Ikonli) i stället för text.
 *
 * Utseendet styrs av main.css (klassen icon-button): ingen ram eller
 * bakgrund i vila, en rundad färgad bakgrund och färgad ikon när muspekaren
 * är över knappen. Dessutom en kort skalanimation – den går inte att göra i
 * JavaFX-CSS, som saknar övergångar.
 */
public final class IconButton {

    /** Visas om ett ikonnamn inte finns i ikonpaketet. */
    private static final String FALLBACK_ICON = "mdi2h-help-circle-outline";

    private static final Duration ANIMATION = Duration.millis(110);
    private static final double HOVER_SCALE = 1.15;
    private static final double PRESSED_SCALE = 0.92;

    private IconButton() {
    }

    /**
     * @param iconLiteral Ikonli-namn, t.ex. "mdi2p-play"
     * @param tooltip     text som visas när muspekaren vilar på knappen
     * @param action      vad som händer vid klick
     */
    public static Button create(String iconLiteral, String tooltip, Runnable action) {
        Button button = new Button();
        button.getStyleClass().add("icon-button");
        button.setGraphic(icon(iconLiteral));
        button.setFocusTraversable(false);   // Mellanslag/Enter styr uppspelning, inte knappar
        button.setMnemonicParsing(false);
        if (tooltip != null) {
            Tooltip t = new Tooltip(tooltip);
            t.setShowDelay(Duration.millis(400));
            button.setTooltip(t);
        }
        button.setOnAction(e -> action.run());
        animate(button);
        return button;
    }

    /** Byter ikon, t.ex. mellan spela och pausa. */
    public static void setIcon(Button button, String iconLiteral) {
        button.setGraphic(icon(iconLiteral));
    }

    private static Node icon(String literal) {
        try {
            return new FontIcon(literal);
        }
        catch (RuntimeException e) {
            // Felstavat eller saknat namn ska inte hindra programmet från att starta
            System.err.println("Ikonen finns inte: " + literal);
            return new FontIcon(FALLBACK_ICON);
        }
    }

    /** Ikonen växer lite vid hovring och trycks ihop vid klick. */
    private static void animate(Button button) {
        Node graphic = button;
        ScaleTransition scale = new ScaleTransition(ANIMATION, graphic);

        button.hoverProperty().addListener((obs, was, hover) -> {
            if (!button.isDisabled()) {
                to(scale, hover ? HOVER_SCALE : 1.0);
            }
        });
        button.pressedProperty().addListener((obs, was, pressed) ->
            to(scale, pressed ? PRESSED_SCALE : (button.isHover() ? HOVER_SCALE : 1.0)));
        button.disabledProperty().addListener((obs, was, disabled) -> {
            if (disabled) {
                to(scale, 1.0);
            }
        });
    }

    private static void to(ScaleTransition scale, double value) {
        scale.stop();
        scale.setToX(value);
        scale.setToY(value);
        scale.playFromStart();
    }
}
