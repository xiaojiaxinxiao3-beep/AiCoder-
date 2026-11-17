import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A small licence server to try the client against, and to show what a real one has to do.
 *
 *     java tools/LicenseServer.java <folder> [port]
 *
 * In the folder: licenses.txt lists the keys, one per line, optionally followed by a comma and the time
 * the key runs out (seconds since 1970). bindings.properties is written by the server: which computer
 * each key belongs to. Delete a key's line there to let it be bound to another computer.
 * signing-key.pkcs8 is the private key, made on the first run. The public key the client build needs is
 * printed at start: give it to build.sh as LICENSE_PUBKEY.
 *
 * This is for testing. A server on the internet also needs HTTPS in front of it, a limit on how often
 * one address may ask, and its private key kept somewhere safer than a file beside the program.
 */
public class LicenseServer {
    /** How long a token is good for. The client goes on working this long without reaching the server. */
    static final long TOKEN_SECONDS = 3 * 24 * 3600;

    public static void main(String[] args) throws Exception {
        Path folder = Path.of(args.length > 0 ? args[0] : ".");
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8765;
        Files.createDirectories(folder);

        Path keyFile = folder.resolve("signing-key.pkcs8"), publicFile = folder.resolve("public-key.txt");
        PrivateKey privateKey;
        if (Files.exists(keyFile)) {
            privateKey = KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(keyFile)));
        } else {
            KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            Files.write(keyFile, pair.getPrivate().getEncoded());
            Files.writeString(publicFile, Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
            privateKey = pair.getPrivate();
        }
        System.out.println("LICENSE_PUBKEY=" + Files.readString(publicFile).strip());

        Path licenses = folder.resolve("licenses.txt"), bindings = folder.resolve("bindings.properties");
        if (!Files.exists(licenses)) Files.writeString(licenses, "");

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/verify", exchange -> {
            String answer;
            try {
                Map<String, String> form = new HashMap<>();
                for (String pair : new String(exchange.getRequestBody().readNBytes(4096), StandardCharsets.UTF_8).split("&")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0) form.put(pair.substring(0, eq), URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
                }
                answer = verify(form.get("key"), form.get("hwid"), licenses, bindings, privateKey);
            } catch (Exception e) {
                answer = "status=INVALID\n";
            }
            System.out.println(answer.lines().findFirst().orElse(""));
            byte[] body = answer.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        System.out.println("listening on http://127.0.0.1:" + port + "/verify");
    }

    static synchronized String verify(String key, String hwid, Path licenses, Path bindings, PrivateKey privateKey) throws Exception {
        if (key == null || hwid == null || !key.matches("[A-Za-z0-9_-]{1,128}") || !hwid.matches("[0-9a-f]{64}")) return "status=INVALID\n";

        // Is it one of ours, and still in date?
        long now = System.currentTimeMillis() / 1000, until = -1;
        boolean known = false;
        for (String line : Files.readAllLines(licenses)) {
            String[] parts = line.strip().split(",");
            if (!parts[0].strip().equals(key)) continue;
            known = true;
            until = parts.length > 1 ? Long.parseLong(parts[1].strip()) : Long.MAX_VALUE;
        }
        if (!known) return "status=INVALID\n";
        if (until <= now) return "status=EXPIRED\n";

        // Whose is it?
        Properties bound = new Properties();
        if (Files.exists(bindings)) try (var in = Files.newInputStream(bindings)) { bound.load(in); }

        String status;
        String owner = bound.getProperty(key);
        if (owner == null) {
            bound.setProperty(key, hwid);
            try (var out = Files.newOutputStream(bindings)) { bound.store(out, "key = the computer it belongs to"); }
            status = "BOUND";
        } else if (owner.equals(hwid)) {
            status = "OK";
        } else {
            return "status=HWID_MISMATCH\n";
        }

        // The token: what the client will accept as proof, for this key on this computer, for a few days
        long expires = Math.min(until, now + TOKEN_SECONDS);
        byte[] payload = ("v1|" + key + "|" + hwid + "|" + expires).getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(privateKey);
        signer.update(payload);
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        return "status=" + status + "\ntoken=" + b64.encodeToString(payload) + "." + b64.encodeToString(signer.sign()) + "\n";
    }
}
