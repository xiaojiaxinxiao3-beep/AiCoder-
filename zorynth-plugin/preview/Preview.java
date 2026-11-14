import dev.zorynth.api.PluginApi;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/** Loads the built plugin against a fake host and writes one PNG per theme. Not part of the plugin. */
public class Preview {
    record Reg(String module, PluginApi.Layer layer, PluginApi.Ui ui) {}
    public static void main(String[] a) throws Exception {
        List<Reg> uis = new ArrayList<>(); Map<String, PluginApi.Theme> themes = new LinkedHashMap<>();
        Map<String, boolean[]> on = new LinkedHashMap<>(); Map<String, String> names = new HashMap<>();
        PluginApi.Context ctx = new PluginApi.Context() {
            public PluginApi.ModuleHandle module(PluginApi.ModuleSpec s, PluginApi.Module m) {
                boolean[] st = {false}; on.put(s.id(), st); names.put(s.id(), s.name());
                return new PluginApi.ModuleHandle() {
                    public boolean enabled() { return st[0]; } public void setEnabled(boolean e) { st[0] = e; }
                    public PluginApi.Value<String> text(String i, String n, String v, int x) { return () -> v; }
                    public PluginApi.Value<Boolean> bool(String i, String n, boolean v) { return () -> v; }
                    public PluginApi.Value<Integer> color(String i, String n, int v) { return () -> v; }
                    public PluginApi.Value<Double> number(String i, String n, double v, double x, double y, double z) { return () -> v; }
                    public PluginApi.Value<String> choice(String i, String n, String v, String... o) { return () -> v; }
                };
            }
            public void hud(PluginApi.HudSpec s, PluginApi.Hud h) {}
            public void theme(String m, int p, PluginApi.Theme t) { themes.put(m, t); }
            public void decorateHud(String h, String m, int p, PluginApi.HudTransform t) {}
            public void ui(String id, String m, PluginApi.Layer l, int p, PluginApi.Ui u) { uis.add(new Reg(m, l, u)); }
            public Path dataDirectory() { return Path.of("."); }
        };
        PluginApi.Plugin plugin = (PluginApi.Plugin) Class.forName("aicoder.zorynth.AiCoderThemesPlugin").getDeclaredConstructor().newInstance();
        plugin.onLoad(ctx);
        int W = 960, H = 540, S = 2; long fills = 0;
        for (String id : on.keySet()) {
            on.values().forEach(s -> s[0] = false); on.get(id)[0] = true;
            BufferedImage img = new BufferedImage(W * S, H * S, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setPaint(new GradientPaint(0, 0, new Color(120, 170, 220), 0, H * S, new Color(70, 110, 60))); g.fillRect(0, 0, W * S, H * S);
            g.scale(S, S); g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 9));
            int[] count = {0};
            PluginApi.Canvas c = new PluginApi.Canvas() {
                public int width() { return W; } public int height() { return H; }
                public void fill(int x, int y, int w, int h, int argb) { count[0]++; g.setColor(new Color(argb, true)); g.fillRect(x, y, w, h); }
                public void text(String t, int x, int y, int argb, boolean sh) { count[0]++;
                    if (sh) { g.setColor(new Color(0, 0, 0, (argb >>> 24) / 2)); g.drawString(t, x + 1, y + 9); }
                    g.setColor(new Color(argb, true)); g.drawString(t, x, y + 8); }
            };
            PluginApi.UiView view = new PluginApi.UiView(W, H);
            for (Reg r : uis) if (r.module().equals(id) && r.layer() != PluginApi.Layer.FOREGROUND) r.ui().render(c, view);
            // stand-in for the host's own window, in the theme's three colours
            PluginApi.Theme t = themes.get(id);
            g.setColor(new Color(12, 14, 22, 215)); g.fillRect(300, 150, 360, 240);
            g.setColor(new Color(t.primary(), true)); g.fillRect(300, 150, 360, 2);
            g.drawString(names.get(id), 312, 172); g.fillRect(312, 200, 120, 6);
            g.setColor(new Color(t.text(), true)); g.drawString("Body text / module name", 312, 232);
            g.setColor(new Color(t.secondary(), true)); g.drawString("Secondary text / description", 312, 250);
            for (Reg r : uis) if (r.module().equals(id) && r.layer() == PluginApi.Layer.FOREGROUND) r.ui().render(c, view);
            javax.imageio.ImageIO.write(img, "png", new java.io.File(a[0], id + ".png"));
            System.out.println(id + " draws=" + count[0]);
        }
        // exclusivity check
        on.values().forEach(s -> s[0] = false);
        PluginApi.Canvas nul = new PluginApi.Canvas() { public int width(){return 0;} public int height(){return 0;} public void fill(int a1,int b,int c1,int d,int e){} public void text(String s,int x,int y,int c1,boolean b){} };
        on.get("aurora")[0] = true; uis.get(0).ui().render(nul, new PluginApi.UiView(0,0));
        on.get("magma")[0] = true; uis.get(0).ui().render(nul, new PluginApi.UiView(0,0));
        System.out.println("exclusive: aurora=" + on.get("aurora")[0] + " magma=" + on.get("magma")[0]);
    }
}
