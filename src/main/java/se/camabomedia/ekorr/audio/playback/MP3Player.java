package se.camabomedia.ekorr.audio.playback;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public class MP3Player {
    private MediaPlayer mediaPlayer;

    public MP3Player() {
        // Initialize JavaFX platform
        if (!Platform.isFxApplicationThread()) {
            Platform.startup(() -> {});
        }
    }

    public void play(String filePath, double startTime, double speed, double volume) {
        Media media = new Media(filePath);
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setOnReady(() -> {

            Duration duration = mediaPlayer.getMedia().getDuration();
            Duration startDuration = Duration.millis(startTime);
            Duration endDuration = duration.subtract(startDuration);

            mediaPlayer.setStartTime(startDuration);
            mediaPlayer.setStopTime(endDuration);
            mediaPlayer.setRate(speed);
            mediaPlayer.setVolume(volume);
            mediaPlayer.setCycleCount(1);

            mediaPlayer.play();

        });

        mediaPlayer.setOnEndOfMedia(() -> mediaPlayer.stop());
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }
}
