package se.camabomedia.ekorraudio.playback;

import java.util.concurrent.CountDownLatch;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Popup;
import javafx.util.Duration;

public class MP3Player {
    public MediaPlayer mediaPlayer;
    private Media media;
    private CountDownLatch latch;
    private Timeline positionUpdater;
    public Duration currentPosition;
    private IntegerProperty currentTime;

    public MP3Player() {
        // latch = new CountDownLatch(1);
        this.currentTime = new SimpleIntegerProperty(this, "currentTime");
        // Initialize JavaFX platform
        if (!Platform.isFxApplicationThread()) {
            Platform.startup(() -> {});
        }
    }

    public Integer getCurrentTime() {
        return currentTime.get();
    }

    public void setCurrentTime(Integer s) {
        currentTime.set(s);
    }

    public IntegerProperty currentTimeProperty() {
        return this.currentTime;
    }

    public void play(String mediaFile, double startTime, double endTime, double speed, double volume) {
        mediaFile = "file:///" + mediaFile.replace("\\", "/");
        System.out.println(mediaFile + ": " + startTime + " " + endTime);

        // // Create a CountDownLatch with an initial count of 1
        latch = new CountDownLatch(1);

        media = new Media(mediaFile);
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setOnReady(() -> {
                mediaPlayer.setStartTime(Duration.millis(startTime));
                mediaPlayer.setStopTime(Duration.millis(endTime));
                mediaPlayer.setRate(speed);
                mediaPlayer.setVolume(volume);
                mediaPlayer.play();
                // startPositionUpdater();
            });

        mediaPlayer.setOnPlaying(() -> {
                Timeline positionUpdater
                    = new Timeline(new KeyFrame(Duration.millis(100), event -> {
                                currentPosition = mediaPlayer.getCurrentTime();
                                // System.out.println("Current Position: " + currentPosition);
                                setCurrentTime((int)currentPosition.toMillis());
                    }));
                positionUpdater.setCycleCount(Timeline.INDEFINITE);
                positionUpdater.play();
        });

        mediaPlayer.setOnEndOfMedia(() -> {
                // mediaPlayer.seek(Duration.millis(endTime));
                latch.countDown();
            });

        // mediaPlayer.setOnError(() -> {
        //     latch.countDown();
        //     System.out.println("Error occurred while playing the sound clip.");
        // });

        // mediaPlayer.setOnStopped(() -> {
        //     latch.countDown();
        //     System.out.println("Sound clip playback stopped prematurely.");
        // });

        // mediaPlayer.setOnPaused(() -> {
        //     latch.countDown();
        //     System.out.println("Sound clip playback paused.");
        // });

        // mediaPlayer.setOnHalted(() -> {
        //     latch.countDown();
        //     System.out.println("Sound clip playback halted.");
        // });

        // mediaPlayer.setOnStalled(() -> {
        //     latch.countDown();
        //     System.out.println("Sound clip playback stalled.");
        // });

        // mediaPlayer.seek(Duration.ZERO); // Reset the playback position
        // mediaPlayer.seek(Duration.millis(endTime));
        // mediaPlayer.play();

        // mediaPlayer.stop();
        // mediaPlayer.setStartTime(Duration.seconds(startTime));
        // mediaPlayer.setStopTime(Duration.seconds(endTime));
        // mediaPlayer.play();

    }


     public Duration getCurrentPosition() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            return mediaPlayer.getCurrentTime();
        }
        return Duration.ZERO;
    }

    private void startPositionUpdater() {
        positionUpdater = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            Duration currentPosition = getCurrentPosition();
            // System.out.println("Current Position: " + currentPosition);
            // Do something with the current position
        }));
        positionUpdater.setCycleCount(Timeline.INDEFINITE);
        positionUpdater.play();
    }

    private void stopPositionUpdater() {
        if (positionUpdater != null) {
            positionUpdater.stop();
        }
    }

    public void waitUntilFinished() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }
}
