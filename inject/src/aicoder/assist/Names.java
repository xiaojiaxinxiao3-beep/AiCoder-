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
                // Loaded by Fabric's class loader, this is the copy that speaks intermediary names
                shared = load(Names.class.getClassLoader() == ClassLoader.getSystemClassLoader() ? "names.properties" : "names-intermediary.properties");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return shared;
    }

    static Names load() throws Exception {
        return load("names.properties");
    }

    /** {@code file} is names.properties for the plain game, names-intermediary.properties under Fabric Loader. */
    static Names load(String file) throws Exception {
        Names names = new Names();
        try (InputStream in = Names.class.getResourceAsStream("/aicoder/assist/" + file)) {
            if (in == null) throw new IllegalStateException("this download has no " + file);
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
