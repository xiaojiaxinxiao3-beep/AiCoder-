package aicoder.assist.license;

import aicoder.assist.Client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * One key, one computer. The key and this computer's fingerprint go to the licence server; the server
 * answers with a status and, when the key is good for this computer, a token it signed. The token is what
 * unlocks the modules, and it is checked here against the server's public key, so an answer made up by
 * anything other than the server is worth nothing.
 *
 * <p>The whole thing is off unless the build was given a server address and public key (see build.sh):
 * without them {@link #locked()} is always false.
 *
 * <p>Protocol. Request: POST to the address, form encoded, fields key, hwid, product.
 * Answer: text lines "status=..." and, for BOUND and OK, "token=PAYLOAD.SIGNATURE" in URL-safe base64.
 * The payload is the text "v1|key|hwid|expires" (expires in seconds since 1970) and the signature is
 * Ed25519 over the payload bytes. Statuses: BOUND (the key was free and now belongs to this computer),
 * OK (it already did), HWID_MISMATCH (it belongs to another computer), INVALID, EXPIRED.
 */
public final class License {
    public static final String KEY_PROPERTY = "aicoder.assist.license.key";

    private static final int TEXT = 0xFF4DFFB8, WARN = 0xFFFFC857, BAD = 0xFFFF6F8A;

    private static String url;
    private static PublicKey publicKey;
    private static volatile boolean enabled, unlocked, checking;
    private static final ConcurrentLinkedQueue<Object[]> messages = new ConcurrentLinkedQueue<>();

    private License() {
    }

    /** Whether the modules must stay off. */
    public static boolean locked() {
        return enabled && !unlocked;
    }

    public static boolean enabled() {
        return enabled;
    }

    /** Called once when the client starts, on the game thread. The slow parts run on their own thread. */
    public static void begin() {
        Properties config = new Properties();
        try (InputStream in = License.class.getResourceAsStream("/aicoder/assist/license.properties")) {
            if (in != null) config.load(in);
        } catch (Exception ignored) {
        }
        url = config.getProperty("url", "").strip();
        String encoded = config.getProperty("public-key", "").strip();
        if (url.isEmpty() || encoded.isEmpty()) return;

        // From here on the client is licensed software: nothing works until a token checks out
        enabled = true;
        try {
            publicKey = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encoded)));
        } catch (Exception e) {
            say("This build's licence settings are broken (bad public key). Tell the seller.", BAD);
            return;
        }

        seenKey = System.getProperty(KEY_PROPERTY, "");
        checking = true;
        Thread thread = new Thread(License::check, "AiCoder Assist licence");
        thread.setDaemon(true);
        thread.start();
    }

    private static String seenKey;

    /** Game thread, every tick: shows what the licence thread has to say, and checks again when the injector brings a new key. */
    public static void tick() {
        if (!enabled) return;
        String handed = System.getProperty(KEY_PROPERTY);
        if (handed != null && !handed.equals(seenKey) && !checking && publicKey != null) {
            boolean first = seenKey == null;
            seenKey = handed;
            if (!first) {
                checking = true;
                Thread thread = new Thread(License::check, "AiCoder Assist licence");
                thread.setDaemon(true);
                thread.start();
            }
        }
        if (Client.mc.level == null) return;
        Object[] message;
        while ((message = messages.poll()) != null) Client.chat("Licence", (String) message[0], (Integer) message[1]);
    }

    public static String state() {
        return !enabled ? "not needed" : unlocked ? "valid" : checking ? "checking" : "locked";
    }

    // The check

    private static void check() {
        try {
            String key = key();
            if (key == null) {
                say("No licence key. Put it in the file " + keyFile().getName() + " in the game folder (one line), then inject again.", WARN);
                return;
            }
            String hwid = HwidProvider.get();
            if (hwid == null) {
                say("This computer gives nothing to identify it by, so the key cannot be bound to it.", BAD);
                return;
            }

            // A token from an earlier run opens the modules at once; the server is still asked below
            Properties cache = loadCache();
            boolean hadToken = key.equals(cache.getProperty("key")) && valid(cache.getProperty("token"), key, hwid);
            if (hadToken) unlocked = true;

            String answer;
            try {
                answer = ask(key, hwid);
            } catch (Exception e) {
                if (hadToken) say("Licence server not reachable; using the saved licence.", WARN);
                else say("Licence server not reachable and there is no saved licence: modules stay locked. (" + e.getClass().getSimpleName() + ")", BAD);
                return;
            }

            Properties reply = new Properties();
            reply.load(new java.io.StringReader(answer));
            String status = reply.getProperty("status", "").strip();
            String token = reply.getProperty("token", "").strip();

            switch (status) {
                case "BOUND", "OK" -> {
                    if (!valid(token, key, hwid)) {
                        unlocked = false;
                        say("The licence server's answer did not carry a valid signature: modules stay locked.", BAD);
                        return;
                    }
                    unlocked = true;
                    saveCache(key, token);
                    if (status.equals("BOUND")) say("Licence activated: this key now belongs to this computer.", TEXT);
                    else say("Licence valid.", TEXT);
                }
                case "HWID_MISMATCH" -> {
                    unlocked = false;
                    clearCache();
                    say("This key is already bound to another computer. Ask the seller to move it.", BAD);
                }
                case "EXPIRED" -> {
                    unlocked = false;
                    clearCache();
                    say("This licence has expired.", BAD);
                }
                case "INVALID" -> {
                    unlocked = false;
                    clearCache();
                    say("This licence key is not valid.", BAD);
                }
                default -> {
                    if (!hadToken) say("The licence server gave an answer this version does not know: modules stay locked.", BAD);
                }
            }
        } catch (Throwable t) {
            say("Licence check failed: " + t, BAD);
        } finally {
            checking = false;
        }
    }

    /** Whether a token was signed by the server, is for this key on this computer, and has not run out. */
    static boolean valid(String token, String key, String hwid) {
        try {
            if (token == null) return false;
            int dot = token.indexOf('.');
            if (dot <= 0) return false;
            byte[] payload = Base64.getUrlDecoder().decode(token.substring(0, dot));
            byte[] signature = Base64.getUrlDecoder().decode(token.substring(dot + 1));

            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(payload);
            if (!verifier.verify(signature)) return false;

            String[] fields = new String(payload, StandardCharsets.UTF_8).split("\\|");
            return fields.length == 4 && fields[0].equals("v1") && fields[1].equals(key) && fields[2].equals(hwid)
                && Long.parseLong(fields[3]) > System.currentTimeMillis() / 1000;
        } catch (Exception e) {
            return false;
        }
    }

    private static String ask(String key, String hwid) throws Exception {
        String body = "key=" + enc(key) + "&hwid=" + enc(hwid) + "&product=aicoder-assist";
        HttpURLConnection http = (HttpURLConnection) URI.create(url).toURL().openConnection();
        http.setConnectTimeout(6000);
        http.setReadTimeout(8000);
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        http.setRequestProperty("User-Agent", "AiCoderAssist");
        try (OutputStream out = http.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
        try (InputStream in = http.getResponseCode() < 400 ? http.getInputStream() : http.getErrorStream()) {
            return in == null ? "" : new String(in.readNBytes(8192), StandardCharsets.UTF_8);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // Files in the game folder

    private static File keyFile() {
        return new File(Client.mc.gameDirectory, "aicoder-assist-license.txt");
    }

    private static File cacheFile() {
        return new File(Client.mc.gameDirectory, "aicoder-assist-license.properties");
    }

    /** The key the injector handed over, or the one in the game folder. A key from the injector is saved for next time. */
    private static String key() {
        String key = clean(System.getProperty(KEY_PROPERTY));
        if (key != null) {
            try {
                Files.writeString(keyFile().toPath(), key + System.lineSeparator(), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
            return key;
        }
        try {
            for (String line : Files.readAllLines(keyFile().toPath(), StandardCharsets.UTF_8)) {
                key = clean(line);
                if (key != null) return key;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String clean(String key) {
        if (key == null) return null;
        key = key.replace("﻿", "").strip();
        return key.isEmpty() || key.length() > 128 || !key.matches("[A-Za-z0-9_-]+") ? null : key;
    }

    private static Properties loadCache() {
        Properties cache = new Properties();
        try (FileInputStream in = new FileInputStream(cacheFile())) {
            cache.load(in);
        } catch (Exception ignored) {
        }
        return cache;
    }

    private static void saveCache(String key, String token) {
        Properties cache = new Properties();
        cache.setProperty("key", key);
        cache.setProperty("token", token);
        try (FileOutputStream out = new FileOutputStream(cacheFile())) {
            cache.store(out, "AiCoder Assist licence. Signed by the server for this computer; editing it only makes it invalid.");
        } catch (Exception ignored) {
        }
    }

    private static void clearCache() {
        cacheFile().delete();
    }

    private static void say(String text, int color) {
        System.out.println("[AiCoder Assist] licence: " + text);
        messages.add(new Object[]{text, color});
    }
}
