package se.camabomedia.ekorraudio.report;

// <audio controls>
//   <source src="data:audio/wav;base64,(base64 encoded audio data)" type="audio/wav">
//   Your browser does not support the audio element.
// </audio>

import org.jsoup.nodes.Entities;

/**
 * En kommentar i korrekturrapporten. Fälten innehåller ren text – HTML-escaping
 * görs först i {@link #toString()}, när rapporten skrivs.
 */
public class Comment {
    String commentNr = "";
    String page = "";
    String type = "";
    String seek = "";
    String wrong = "";
    String right = "";
    String base64EncodedAudio = null;
    private final String ENDASH = "–";

    public Comment(String commentNr, String page, String type, String seek,
                   String wrong, String right, String base64EncodedAudio) {
        this.commentNr = commentNr;
        makePageNumber(page);
        this.type = clean(type);
        this.seek = clean(seek);
        this.wrong = clean(wrong);
        this.right = clean(right);
        this.base64EncodedAudio = base64EncodedAudio;
    }

    private void makePageNumber(String page) {
        if (page == null || page.isBlank()) {
            this.page = ENDASH;
        }
        else {
            this.page = page.strip();
        }
    }

    private static String clean(String s) {
        return s == null ? "" : s.strip();
    }

    /** Escapar &, <, > och citattecken för HTML. */
    private static String html(String s) {
        return Entities.escape(s);
    }

    public String getPage() {
        return page;
    }

    public String getType() {
        return type;
    }

    public String hasAudio() {
        if (base64EncodedAudio != null) {
            return "✓";
        }
        else {
            return "";
        }
    }

    public String getSeek() {
        return seek;
    }

    public String getWrong() {
        return wrong;
    }

    public String getRight() {
        return right;
    }

    public void setNr(String nr) {
        commentNr = nr;
    }

    public String getNr() {
        return commentNr;
    }

    public String getAudioData() {
        return base64EncodedAudio;
    }


    @Override
    public String toString() {
        String html = "<div class='comment'><table class='comment-table'><tbody>";

        html += "<tr><th class='comment-nr'>" + commentNr + ".</th><th class='comment-type'>" + html(type) + "</th></tr>";
        // html += "<tr><td class='comment-head'>Typ:</td><td class='comment-type'>" + type + "</td></tr>";

        html += "<tr><td></td><td><table class='inner-table'><tbody>";

        if (!wrong.equals("")) {
            html += "<tr><td class='comment-head'>Fel:</td><td class='comment-wrong'>" + html(wrong) + "</td></tr>";
        }

        if (!right.equals("")) {
            html += "<tr><td class='comment-head'>Rätt:</td><td class='comment-right'>" + html(right) + "</td></tr>";
        }

        if (base64EncodedAudio != null) {
            html += "<tr class='comment-audio-row'><td class='comment-head'>Ljud:</td>"
                + "<td class='comment-audio'>"
                + "<audio controls>"
                + "<source src='data:audio/wav;base64," + base64EncodedAudio + "' type='audio/wav'>"
                + "Din browser stöder inte uppspelning av ljud."
                + "</audio>"
                + "</td></tr>";
        }

        html += "<tr><td class='comment-head'>Sida:</td><td class='comment-page'>" + html(page) + "</td></tr>";

        if (!seek.equals("")) {
            html += "<tr>"
                + "<td class='comment-head'>Sök:</td><td class='comment-seek'>"
                + html(seek) + "</td></tr>";
        }

        // Close elements....
        html += "</tbody></table></td></tr>";
        // Extra tr for spacing when pasted into Outlook
        html += "<tr><td>&nbsp;</td><td>&nbsp;</td></tr>";
        // Close rest of elements
        html += "</tbody></table></div>";
        return html;
    }
}
