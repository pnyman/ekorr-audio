package se.camabomedia.ekorraudio.report;

/**
 * Var i boken en kommentar gjordes: boktid, rubriktid och – för talböcker
 * utan text – event och rubrik (med överordnade rubriker). Skrivs i
 * rapporten men visas inte i formuläret.
 *
 * Alla fält är text, precis som de står i rapporten; tomt = saknas.
 */
public final class Position {

    /** Total förfluten tid i boken, t.ex. "01h23m45s678ms". */
    public final String bookTime;
    /** Förfluten tid i rubriken (smil-filen). */
    public final String headingTime;
    /** Eventnummer i smil-filen (bara böcker utan text), annars "". */
    public final String event;
    /** "Rubrik nivå1 > Rubrik nivå2 > …" (bara böcker utan text), annars "". */
    public final String heading;

    public Position(String bookTime, String headingTime, String event, String heading) {
        this.bookTime = clean(bookTime);
        this.headingTime = clean(headingTime);
        this.event = clean(event);
        this.heading = clean(heading);
    }

    /** Sant om det inte finns något att skriva (t.ex. kommentarer från äldre rapporter). */
    public boolean isEmpty() {
        return bookTime.isEmpty() && headingTime.isEmpty() && event.isEmpty() && heading.isEmpty();
    }

    /** ms -> "00h00m00s000ms". */
    public static String format(long ms) {
        long t = Math.max(0, ms);
        return String.format("%02dh%02dm%02ds%03dms",
                             t / 3_600_000, (t / 60_000) % 60, (t / 1000) % 60, t % 1000);
    }

    private static String clean(String s) {
        return s == null ? "" : s.strip();
    }
}
