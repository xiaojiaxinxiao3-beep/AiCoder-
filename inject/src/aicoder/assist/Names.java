package aicoder.assist;

import java.io.InputStream;
import java.util.Properties;

/**
 * The game's obfuscated names for the methods that get patched, generated from Mojang's official mappings
 * at build time. Everything else in the client is compiled against readable names and converted as a whole.
 */
public final class Names {
    private final Properties table = new Properties();

    private static Names shared;

    /** The table, loaded once. */
    public static synchronized Names shared() {
        if (shared == null) {
            try {
                shared = load();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return shared;
    }

    static Names load() throws Exception {
        Names names = new Names();
        try (InputStream in = Names.class.getResourceAsStream("/aicoder/assist/names.properties")) {
            names.table.load(in);
        }
        return names;
    }

    public String get(String key) {
        String value = table.getProperty(key);
        if (value == null) throw new IllegalStateException("No name for " + key);
        return value;
    }
}
