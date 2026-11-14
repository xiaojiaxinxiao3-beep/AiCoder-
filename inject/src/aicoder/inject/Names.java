package aicoder.inject;

import java.io.InputStream;
import java.util.Properties;

/** The game's obfuscated names for this Minecraft version, generated from Mojang's official mappings at build time. */
public final class Names {
    private final Properties table = new Properties();

    static Names load() throws Exception {
        Names names = new Names();
        try (InputStream in = Names.class.getResourceAsStream("/aicoder/inject/names.properties")) {
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
