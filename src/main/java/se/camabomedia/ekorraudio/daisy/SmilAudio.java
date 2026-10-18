package se.camabomedia.ekorraudio.daisy;

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
    int clipBegin;

    /**
     * End of the clip, in milliseconds.
     */
    int clipEnd;

    /**
     * Class constructor.
     * @param audio a jsoup element
     */
    public SmilAudio(Element audio) {
        src = audio.attr("src");
        clipBegin = timestampToInt(audio.attr("clip-begin"));
        clipEnd = timestampToInt(audio.attr("clip-end"));
    }

    private int timestampToInt(String t) {
        return (int) (Float.parseFloat(t.replace("npt=", "").replace("s", "")) * 1000);
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

    public int getClipBegin() {
        return clipBegin;
    }

    public int getClipEnd() {
        return clipEnd;
    }

}
