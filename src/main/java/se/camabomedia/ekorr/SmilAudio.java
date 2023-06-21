package se.camabomedia.ekorr;

import org.jsoup.nodes.Element;

/**
 * Holds data from an audio-tag in a par-element.
 *
 * @author Patrik Nyman
 */
public class SmilAudio {

    /**
     * The name om the sound file.
     */
    String src;

    /**
     * Beginning of the clip, in milliseconds.
     */
    float clipBegin;

    /**
     * End of the clip, in milliseconds.
     */
    float clipEnd;

    /**
     * Class constructor.
     * @param audio a jsoup element
     */
    public SmilAudio(Element audio) {
        src = audio.attr("src");
        clipBegin = timestampToFloat(audio.attr("clip-begin"));
        clipEnd = timestampToFloat(audio.attr("clip-end"));
    }

    private float timestampToFloat(String timestamp) {
        return Float.parseFloat(timestamp.replace("npt=", "").replace("s", "")) * 1000;
    }

    @Override
    public String toString() {
        return "SmilAudio [src=" + src
            + ", clipBegin=" + clipBegin
            + ", clipEnd=" + clipEnd
            + "]";
    }

    public String getSrc() {
        return src;
    }

    public float getClipBegin() {
        return clipBegin;
    }

    public float getClipEnd() {
        return clipEnd;
    }

}
