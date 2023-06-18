package se.camabomedia.ekorr;

import org.jsoup.nodes.Element;

/**
 * SmilAudio
 */
public class SmilAudio {

    String src;
    float clipBegin;
    float clipEnd;

    public SmilAudio(Element audio) {
        src = audio.attr("src");
        clipBegin = timestampToFloat(audio.attr("clip-begin"));
        clipEnd = timestampToFloat(audio.attr("clip-end"));
    }

    private float timestampToFloat(String timestamp) {
        return Float.parseFloat(timestamp.replace("npt=", "").replace("s", ""));
    }

    @Override
    public String toString() {
        return "SmilAudio [src=" + src
            + ", clipBegin=" + clipBegin
            + ", clipEnd=" + clipEnd
            + "]";
    }

}
