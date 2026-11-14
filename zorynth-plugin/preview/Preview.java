import dev.zorynth.api.PluginApi;
import dev.zorynth.api.PluginApi.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/** A fake host: loads the built plugin, feeds it modules and input, writes PNGs. Not part of the plugin. */
public class Preview {
    static final int W = 960, H = 540, S = 2;
    record Screen(String module, Ui ui) {}
    static class Mod implements ModuleControl {
        final String id, name, cat; boolean on; int key = -1; String mode = "Toggle"; final List<SettingControl> settings = new ArrayList<>();
        Mod(String c, String n, boolean on) { id = n.toLowerCase().replace(' ', '_'); name = n; cat = c; this.on = on; }
        public String id() { return id; } public String name() { return name; } public String category() { return cat; }
        public boolean enabled() { return on; } public boolean alwaysOn() { return false; } public void setEnabled(boolean e) { on = e; }
        public int bindKey() { return key; } public String bindMode() { return mode; } public void bind(int k, String m) { key = k; mode = m; }
        public List<SettingControl> settings() { return settings; }
        Mod add(String name, String page, SettingKind kind, Object v, double min, double max, double step, String... choices) {
            Object[] box = {v}; SettingInfo info = new SettingInfo(name.toLowerCase(), name, page, kind, min, max, step, 64, List.of(choices));
            settings.add(new SettingControl() { public SettingInfo info() { return info; } public boolean visible() { return true; } public Object get() { return box[0]; } public void set(Object o) { box[0] = o; } });
            return this;
        }
    }
    static Graphics2D g; static int draws;
    static Color col(int argb) { return new Color(argb, true); }
    static final PluginApi.Canvas canvas = new PluginApi.Canvas() {
        final Deque<AffineTransform> stack = new ArrayDeque<>();
        public int width() { return W; } public int height() { return H; }
        public int textWidth(String t) { return g.getFontMetrics().stringWidth(t); }
        public void fill(int x, int y, int w, int h, int c) { draws++; g.setColor(col(c)); g.fillRect(x, y, w, h); }
        public void text(String t, int x, int y, int c, boolean sh) { draws++; if (sh) { g.setColor(new Color(0, 0, 0, (c >>> 24) / 2)); g.drawString(t, x + 1, y + 8); } g.setColor(col(c)); g.drawString(t, x, y + 7); }
        public void push() { stack.push(g.getTransform()); } public void pop() { g.setTransform(stack.pop()); }
        public void translate(double x, double y) { g.translate(x, y); } public void scale(double x, double y) { g.scale(x, y); } public void rotate(double d) { g.rotate(Math.toRadians(d)); }
        public void clip(int x, int y, int w, int h) { g.setClip(x, y, w, h); } public void unclip() { g.setClip(null); }
        public void gradient(int x, int y, int w, int h, int a, int b, boolean hor) { draws++; g.setPaint(hor ? new GradientPaint(x, 0, col(a), x + w, 0, col(b)) : new GradientPaint(0, y, col(a), 0, y + h, col(b))); g.fillRect(x, y, w, h); }
        public void roundedRect(int x, int y, int w, int h, double r, int c) { draws++; g.setColor(col(c)); g.fill(new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2)); }
        public void line(double a, double b, double c2, double d, double th, int c) { draws++; g.setColor(col(c)); g.setStroke(new BasicStroke((float) th)); g.draw(new Line2D.Double(a, b, c2, d)); }
        public void circle(double x, double y, double r, int c) { draws++; g.setColor(col(c)); g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2)); }
        public void polygon(double[] p, int c) { draws++; Path2D.Double path = new Path2D.Double(); path.moveTo(p[0], p[1]); for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]); path.closePath(); g.setColor(col(c)); g.fill(path); }
    };
    static BufferedImage frame(Ui ui, double mx, double my) throws Exception {
        BufferedImage img = new BufferedImage(W * S, H * S, BufferedImage.TYPE_INT_ARGB);
        g = img.createGraphics(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(120, 170, 220), 0, H * S, new Color(70, 110, 60))); g.fillRect(0, 0, W * S, H * S);
        g.scale(S, S); g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 9)); draws = 0;
        ui.render(canvas, new UiView("modules", W, H, mx, my, 0, ClientView.EMPTY));
        return img;
    }
    static void click(Ui ui, double x, double y, int button) throws Exception {
        UiView v = new UiView("modules", W, H, x, y, 0, ClientView.EMPTY);
        ui.input(v, new InputEvent(InputKind.CLICK, x, y, button, 0, 0, "", 0, 0));
        ui.input(v, new InputEvent(InputKind.RELEASE, x, y, button, 0, 0, "", 0, 0));
    }
    public static void main(String[] a) throws Exception {
        List<Mod> mods = new ArrayList<>(); String[][] names = {
            {"Combat", "Kill Aura", "Auto Crystal", "Criticals", "Velocity", "Auto Totem", "Surround", "Burrow", "Offhand", "Trigger Bot", "Reach", "Auto Armor", "Hole Filler", "Bow Aim", "Anchor Aura"},
            {"Movement", "Speed", "Flight", "Sprint", "Step", "No Fall", "Elytra Fly", "Jesus", "Long Jump", "Safe Walk"},
            {"Player", "Auto Eat", "Fast Use", "Auto Tool", "Chest Stealer", "Inventory Move", "No Rotate"},
            {"Render", "ESP", "Tracers", "Full Bright", "Nametags", "Chams", "Hole ESP", "Free Cam", "Storage ESP"},
            {"Client", "Click GUI", "HUD", "Colors", "Notifications"}};
        for (String[] row : names) for (int i = 1; i < row.length; i++) mods.add(new Mod(row[0], row[i], (i * 7 + row[0].length()) % 3 == 0)
            .add("Range", "General", SettingKind.NUMBER, 4.5, 1, 6, .1).add("Delay", "General", SettingKind.NUMBER, 3.0, 0, 20, 1)
            .add("Rotate", "General", SettingKind.BOOLEAN, true, 0, 0, 0).add("Swing", "General", SettingKind.BOOLEAN, false, 0, 0, 0)
            .add("Priority", "Targeting", SettingKind.CHOICE, "Closest", 0, 0, 0, "Closest", "Health", "Angle")
            .add("Tag", "Targeting", SettingKind.TEXT, "enemy", 0, 0, 0).add("Colour", "Render", SettingKind.COLOR, 0xFF4DFFB8, 0, 0, 0)
            .add("Pause Key", "Render", SettingKind.KEY, 75, 0, 0, 0).add("Blocks", "Render", SettingKind.BLOCK_LIST, List.of("obsidian", "bedrock"), 0, 0, 0));
        List<Screen> screens = new ArrayList<>(); Map<String, ModuleHandle> handles = new LinkedHashMap<>(); Map<String, Map<String, Object[]>> values = new HashMap<>();
        UiHost host = new UiHost() {
            public List<ModuleControl> modules() { return new ArrayList<>(mods); } public void save() {} public void open(String s) {} public void close() {}
            public String clipboard() { return ""; } public void clipboard(String t) {} public String image(String r) { return r; } public Object nativeHandle(String n) { return null; }
        };
        Context ctx = new Context() {
            public String pluginId() { return "aicoder_themes"; } public Path dataDirectory() { return Path.of("."); } public ClientView client() { return ClientView.EMPTY; } public void notify(String m) {}
            public ModuleHandle module(ModuleSpec s, PluginApi.Module m) {
                boolean[] st = {false}; Map<String, Object[]> vs = new HashMap<>(); values.put(s.id(), vs);
                ModuleHandle h = new ModuleHandle() {
                    <T> Value<T> v(String id, T def) { Object[] box = {def}; vs.put(id, box); return new Value<T>() { @SuppressWarnings("unchecked") public T get() { return (T) box[0]; } public void set(T t) { box[0] = t; } }; }
                    public String id() { return s.id(); } public boolean enabled() { return st[0]; }
                    public void setEnabled(boolean e) { boolean was = st[0]; st[0] = e; try { if (e && !was) m.onEnable(); } catch (Exception x) { throw new RuntimeException(x); } }
                    public Value<Boolean> bool(String i, String n, boolean v) { return v(i, v); } public Value<Double> number(String i, String n, double v, double x, double y, double z) { return v(i, v); }
                    public Value<String> choice(String i, String n, String v, String... c) { return v(i, v); } public Value<String> text(String i, String n, String v, int x) { return v(i, v); } public Value<Integer> color(String i, String n, int v) { return v(i, v); }
                };
                handles.put(s.id(), h); return h;
            }
            public void hud(HudSpec s, Hud h) {} public void ui(String i, String m, Layer l, int p, Ui u) {} public void theme(String m, int p, Theme t) {} public void decorateHud(String e, String m, int p, HudTransform t) {}
            public int uiVersion() { return 2; } public UiHost uiHost() { return host; }
            public void replaceScreen(String id, String m, ScreenTarget t, int p, Ui u) { screens.add(new Screen(m, u)); }
        };
        ((Plugin) Class.forName("aicoder.zorynth.AiCoderThemesPlugin").getDeclaredConstructor().newInstance()).onLoad(ctx);
        java.io.File out = new java.io.File(a[0]);
        for (Screen sc : screens) {
            handles.get(sc.module()).setEnabled(true); Ui ui = sc.ui();
            values.get(sc.module()).get("layout")[0] = "Window";
            frame(ui, 0, 0);
            click(ui, 300, 150, 0);            // select the 3rd module row
            BufferedImage img = frame(ui, 300, 150);
            javax.imageio.ImageIO.write(img, "png", new java.io.File(out, sc.module() + "-window.png"));
            int d1 = draws;
            values.get(sc.module()).get("layout")[0] = "Columns";
            img = frame(ui, 200, 100);
            javax.imageio.ImageIO.write(img, "png", new java.io.File(out, sc.module() + "-columns.png"));
            System.out.println(sc.module() + " window draws=" + d1 + " columns draws=" + draws);
        }
        // Interaction checks on the first theme, window layout.
        Screen sc = screens.get(0); Ui ui = sc.ui(); handles.get(sc.module()).setEnabled(true); values.get(sc.module()).get("layout")[0] = "Window";
        frame(ui, 0, 0); Mod crit = mods.get(3); boolean before = crit.on;
        click(ui, 300, 150, 1); frame(ui, 0, 0); System.out.println("right click toggles: " + (crit.on != before));
        click(ui, 300, 150, 0); frame(ui, 0, 0);
        UiView v = new UiView("modules", W, H, 0, 0, 0, ClientView.EMPTY);
        // slider drag on first setting row: find by scanning x for a change
        double r0 = (Double) crit.settings.get(0).get(); for (int x = 600; x < 880 && r0 == (Double) crit.settings.get(0).get(); x += 20) click(ui, x, 132, 0);
        System.out.println("slider changed Range: " + r0 + " -> " + crit.settings.get(0).get());
        frame(ui, 0, 0); for (char ch : "aura".toCharArray()) ui.input(v, new InputEvent(InputKind.CHARACTER, 0, 0, 0, 0, 0, String.valueOf(ch), 0, 0));
        javax.imageio.ImageIO.write(frame(ui, 0, 0), "png", new java.io.File(out, "search.png"));
        for (int i = 0; i < 4; i++) ui.input(v, new InputEvent(InputKind.KEY, 0, 0, 0, 259, 0, "", 0, 0));
        values.get(sc.module()).get("layout")[0] = "Columns"; frame(ui, 0, 0);
        click(ui, 200, 100, 1); javax.imageio.ImageIO.write(frame(ui, 0, 0), "png", new java.io.File(out, "popup.png"));
        // theme switch + exclusivity
        frame(ui, 0, 0); long on = handles.values().stream().filter(ModuleHandle::enabled).count();
        System.out.println("enabled themes: " + on + " " + handles.entrySet().stream().filter(e -> e.getValue().enabled()).map(Map.Entry::getKey).toList());
    }
}
