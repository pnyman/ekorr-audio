package se.camabomedia.ekorr;

// <audio controls>
//   <source src="data:audio/wav;base64,(base64 encoded audio data)" type="audio/wav">
//   Your browser does not support the audio element.
// </audio>

class Comment {
    String commentNr = "";
    String page = "";
    String type = "";
    String seek = "";
    String wrong = "";
    String right = "";
    private String base64EncodedAudio;

    private final String ENDASH = "–";

    public Comment(String commentNr, String page, String type, String seek, String wrong, String right) {
        this.commentNr = commentNr;
        makePageNumber(page);
        this.type = sanitize(type);
        this.seek = sanitize(seek);
        this.wrong = sanitize(wrong);
        this.right = sanitize(right);

        // if (commentNr != null)
        //     this.commentNr = commentNr;
        // makePageNumber(page);
        // if (type != null)
        //     this.type = sanitize(type);
        // if (seek != null)
        //     this.seek = sanitize(seek);
        // if (wrong != null)
        //     this.wrong = sanitize(wrong);
        // if (right != null)
        //     this.right = sanitize(right);
    }

    private void makePageNumber(String page) {
        if (page == null || page.strip() == "") {
            this.page = ENDASH;
        }
        else {
            this.page = sanitize(page);
        }
    }

    private String sanitize(String s) {
        return s
            .replaceAll("&", "&amp")
            .replaceAll("<", "&lt;")
            .trim();
    }

    public String getPage() {
        return page;
    }

    public String getType() {
        return type;
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


    @Override
    public String toString() {
        String html = "<div class='comment'><table class='comment-table'><tbody>";

        html += "<tr><th class='comment-nr'>" + commentNr + ".</th><th class='comment-type'>" + type + "</th></tr>";
        // html += "<tr><td class='comment-head'>Typ:</td><td class='comment-type'>" + type + "</td></tr>";

        html += "<tr><td></td><td><table class='inner-table'><tbody>";

        if (!wrong.equals("")) {
            html += "<tr><td class='comment-head'>Fel:</td><td class='comment-wrong'>" + wrong + "</td></tr>";
        }

        if (!right.equals("")) {
            html += "<tr><td class='comment-head'>Rätt:</td><td class='comment-right'>" + right + "</td></tr>";
        }

        html += "<tr><td class='comment-head'>Sida:</td><td class='comment-page'>" + page + "</td></tr>";

        if (!seek.equals("")) {
            html += "<tr>"
                + "<td class='comment-head'>Sök:</td><td class='comment-seek'>"
                + seek + "</td></tr>";
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
