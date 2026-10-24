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
    /** Boktid, rubriktid m.m., eller null (äldre kommentarer). */
    private Position position = null;
    /** Om raden Sök: ska skrivas – inte för talböcker utan text. */
    private boolean showSeek = true;
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

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position == null || position.isEmpty() ? null : position;
    }

    /** Sätts av Report när rapporten skrivs: false för talböcker utan text. */
    void setShowSeek(boolean showSeek) {
        this.showSeek = showSeek;
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

        // Sida, och på samma rad boktid, rubriktid och (utan text) event.
        // Varje värde i ett eget span, så att rapporten kan läsas in igen.
        String sep = "&nbsp;&nbsp;&nbsp;";
        html += "<tr><td class='comment-head'>Sida:</td><td class='comment-page-row'>"
            + "<span class='comment-page'>" + html(page) + "</span>";
        if (position != null) {
            if (!position.bookTime.isEmpty()) {
                html += sep + "Boktid: <span class='comment-booktime'>" + html(position.bookTime) + "</span>";
            }
            if (!position.headingTime.isEmpty()) {
                html += sep + "Rubriktid: <span class='comment-headingtime'>" + html(position.headingTime) + "</span>";
            }
            if (!position.event.isEmpty()) {
                html += sep + "Event: <span class='comment-event'>" + html(position.event) + "</span>";
            }
        }
        html += "</td></tr>";

        if (position != null && !position.heading.isEmpty()) {
            html += "<tr><td class='comment-head'>Rubrik:</td><td class='comment-heading'>"
                + html(position.heading) + "</td></tr>";
        }

        if (showSeek && !seek.equals("")) {
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
