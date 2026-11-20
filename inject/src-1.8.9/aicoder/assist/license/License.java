package aicoder.assist.license;

/** The 1.8.9 build has no licence check; this answers the questions the shared code asks about one. */
public final class License {
    private License() {
    }

    public static boolean locked() {
        return false;
    }

    public static String state() {
        return "none";
    }
}
