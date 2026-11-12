package aicoder.earth.gui;

import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.List;

/**
 * Everything that differs between the ten themes: palette, corner shapes, the switch glyph, the slider
 * kind, the backdrop and the decorative readouts. Layout is shared.
 */
public final class Style {
    public enum Toggle { STRATA, EMBER, DIAMOND, SPHERE, TARGET, RADIO, CHECK, HEXAGON, PLANET, BEAD }

    public enum Backdrop { STRATA, MAGMA, GLACIER, ABYSS, TOPOGRAPHY, ATMOSPHERE, SEISMOGRAPH, GEODE, ORBIT, AMBER }

    /** Theme name as Meteor lists it in the GUI tab. */
    public final String name;
    /** Header subtitle, e.g. {@code // STRATA}. */
    public final String subtitle;
    /** Chinese name, only drawn with the vanilla font (Meteor's custom fonts have no CJK glyphs). */
    public final String cjk;
    /** Text of the pill at the right of the header. */
    public final String tag;
    /** Footer readouts as label, value pairs: two on the left, one on the right. */
    public final String[] readouts;

    public final Toggle toggle;
    public final Backdrop backdrop;

    /** Light themes have dark text and skip the glows that only read on a dark ground. */
    public boolean light;
    /** Hard offset shadow under windows, for the paper themes. */
    public boolean paperShadow;
    /** Segmented slider instead of a line with a knob. */
    public boolean segmented;

    /** Corners in design pixels: positive is a radius, negative a 45 degree cut. */
    public double corner, plateCorner, fieldCorner;

    public Color windowTop, windowBottom, outline;
    public Color accent, accent2;
    public Color text, dim, faint, title;
    public Color plate, plateSelected;
    public Color field, fieldLine;
    public Color bar, barLine;
    public Color slideFrom, slideTo, knob;

    private Style(String name, String subtitle, String cjk, String tag, Toggle toggle, Backdrop backdrop, String... readouts) {
        this.name = name;
        this.subtitle = subtitle;
        this.cjk = cjk;
        this.tag = tag;
        this.toggle = toggle;
        this.backdrop = backdrop;
        this.readouts = readouts;
    }

    private static Color c(int r, int g, int b) {
        return new Color(r, g, b, 255);
    }

    private static Color c(int r, int g, int b, int a) {
        return new Color(r, g, b, a);
    }

    public static final Style STRATA = new Style("AiCoder 01 Strata", "STRATA", "地層", "DEPTH 2,340 m",
        Toggle.STRATA, Backdrop.STRATA, "DEPTH ", "2,340 m", "AGE ", "310 Ma", "LAYER ", "08 / 11");

    public static final Style MAGMA = new Style("AiCoder 02 Magma", "MAGMA", "火山", "TEMP 1,180 °C",
        Toggle.EMBER, Backdrop.MAGMA, "TEMP ", "1,180 °C", "SiO2 ", "52%", "VEI ", "3");

    public static final Style GLACIER = new Style("AiCoder 03 Glacier", "GLACIER", "冰河", "ICE -12 °C",
        Toggle.DIAMOND, Backdrop.GLACIER, "ICE ", "-12 °C", "FLOW ", "34 m/yr", "ALBEDO ", "0.82");

    public static final Style ABYSS = new Style("AiCoder 04 Abyss", "ABYSS", "深海", "DEPTH 3,800 m",
        Toggle.SPHERE, Backdrop.ABYSS, "DEPTH ", "3,800 m", "PRESSURE ", "381 atm", "TEMP ", "2.1 °C");

    public static final Style TOPOGRAPHY = new Style("AiCoder 05 Topography", "TOPOGRAPHY", "地形圖", "GRID 25 m",
        Toggle.TARGET, Backdrop.TOPOGRAPHY, "N ", "23°58'", "E ", "120°58'", "ELEV ", "1,640 m");

    public static final Style ATMOSPHERE = new Style("AiCoder 06 Atmosphere", "ATMOSPHERE", "大氣", "1013 hPa",
        Toggle.RADIO, Backdrop.ATMOSPHERE, "PRESSURE ", "1013 hPa", "WIND ", "NE 18 kt", "HUMIDITY ", "78%");

    public static final Style SEISMOGRAPH = new Style("AiCoder 07 Seismograph", "SEISMOGRAPH", "地震儀", "STA TW.SEIS",
        Toggle.CHECK, Backdrop.SEISMOGRAPH, "MAG ", "M 4.7", "DEPTH ", "12 km", "PGA ", "0.08 g");

    public static final Style GEODE = new Style("AiCoder 08 Geode", "GEODE", "礦物晶體", "Mohs 7",
        Toggle.HEXAGON, Backdrop.GEODE, "QUARTZ ", "SiO2", "HARDNESS ", "7", "LUSTER ", "VITREOUS");

    public static final Style ORBIT = new Style("AiCoder 09 Orbit", "ORBIT", "衛星地球", "ALT 408 km",
        Toggle.PLANET, Backdrop.ORBIT, "ALT ", "408 km", "VEL ", "7.66 km/s", "UTC ", "");

    public static final Style AMBER = new Style("AiCoder 10 Amber", "AMBER", "琥珀化石", "99 Ma · Cretaceous",
        Toggle.BEAD, Backdrop.AMBER, "AGE ", "99 Ma", "PERIOD ", "Cretaceous", "RESIN ", "Burmite");

    public static final List<Style> ALL = List.of(STRATA, MAGMA, GLACIER, ABYSS, TOPOGRAPHY, ATMOSPHERE, SEISMOGRAPH, GEODE, ORBIT, AMBER);

    static {
        Style s = STRATA;
        s.segmented = true;
        s.corner = 12; s.plateCorner = 8; s.fieldCorner = 5;
        s.windowTop = c(74, 48, 32, 236); s.windowBottom = c(40, 26, 20, 242); s.outline = c(160, 118, 76, 150);
        s.accent = c(232, 160, 96); s.accent2 = c(204, 112, 62);
        s.text = c(236, 220, 200); s.dim = c(172, 146, 120); s.faint = c(122, 100, 82); s.title = c(246, 233, 214);
        s.plate = c(255, 214, 160, 16); s.plateSelected = c(232, 160, 96, 44);
        s.field = c(30, 18, 12, 140); s.fieldLine = c(196, 146, 94, 110);
        s.bar = c(62, 46, 32, 180); s.barLine = c(196, 152, 104, 140);
        s.slideFrom = c(232, 160, 96); s.slideTo = c(232, 160, 96); s.knob = c(246, 233, 214);

        s = MAGMA;
        s.corner = 4; s.plateCorner = 3; s.fieldCorner = 3;
        s.windowTop = c(24, 11, 8, 236); s.windowBottom = c(12, 6, 5, 242); s.outline = c(255, 90, 31, 140);
        s.accent = c(255, 90, 31); s.accent2 = c(226, 52, 20);
        s.text = c(240, 226, 216); s.dim = c(164, 128, 112); s.faint = c(112, 82, 72); s.title = c(250, 240, 232);
        s.plate = c(255, 120, 60, 14); s.plateSelected = c(255, 90, 31, 46);
        s.field = c(40, 14, 8, 150); s.fieldLine = c(255, 90, 31, 90);
        s.bar = c(16, 8, 6, 205); s.barLine = c(255, 90, 31, 150);
        s.slideFrom = c(200, 40, 10); s.slideTo = c(255, 112, 40); s.knob = c(240, 226, 190);

        s = GLACIER;
        s.light = true;
        s.corner = 16; s.plateCorner = 10; s.fieldCorner = 8;
        s.windowTop = c(242, 249, 255, 228); s.windowBottom = c(204, 228, 246, 228); s.outline = c(255, 255, 255, 210);
        s.accent = c(31, 127, 208); s.accent2 = c(90, 170, 235);
        s.text = c(32, 62, 96); s.dim = c(92, 126, 160); s.faint = c(142, 172, 202); s.title = c(16, 44, 80);
        s.plate = c(120, 170, 215, 46); s.plateSelected = c(150, 200, 240, 120);
        s.field = c(255, 255, 255, 130); s.fieldLine = c(120, 170, 215, 160);
        s.bar = c(240, 248, 255, 205); s.barLine = c(255, 255, 255, 225);
        s.slideFrom = c(126, 194, 246); s.slideTo = c(31, 127, 208); s.knob = c(255, 255, 255);

        s = ABYSS;
        s.corner = 18; s.plateCorner = 12; s.fieldCorner = 10;
        s.windowTop = c(10, 60, 82, 218); s.windowBottom = c(4, 28, 48, 232); s.outline = c(47, 224, 200, 92);
        s.accent = c(47, 224, 200); s.accent2 = c(40, 150, 255);
        s.text = c(216, 240, 246); s.dim = c(122, 172, 186); s.faint = c(72, 122, 142); s.title = c(236, 250, 252);
        s.plate = c(60, 160, 170, 28); s.plateSelected = c(47, 224, 200, 52);
        s.field = c(4, 40, 56, 150); s.fieldLine = c(47, 224, 200, 82);
        s.bar = c(8, 50, 70, 205); s.barLine = c(47, 224, 200, 92);
        s.slideFrom = c(30, 120, 255); s.slideTo = c(47, 224, 200); s.knob = c(236, 246, 250);

        s = TOPOGRAPHY;
        s.light = true; s.paperShadow = true;
        s.corner = 2; s.plateCorner = 2; s.fieldCorner = 2;
        s.windowTop = c(250, 243, 228, 240); s.windowBottom = c(246, 236, 216, 240); s.outline = c(150, 110, 80, 205);
        s.accent = c(160, 82, 28); s.accent2 = c(60, 120, 190);
        s.text = c(72, 52, 36); s.dim = c(132, 106, 86); s.faint = c(172, 152, 132); s.title = c(50, 34, 22);
        s.plate = c(160, 110, 70, 28); s.plateSelected = c(200, 140, 90, 62);
        s.field = c(240, 228, 205, 205); s.fieldLine = c(170, 140, 110, 165);
        s.bar = c(250, 243, 228, 236); s.barLine = c(150, 110, 80, 205);
        s.slideFrom = c(70, 120, 190); s.slideTo = c(160, 82, 28); s.knob = c(250, 243, 228);

        s = ATMOSPHERE;
        s.corner = 10; s.plateCorner = 8; s.fieldCorner = 6;
        s.windowTop = c(12, 34, 64, 226); s.windowBottom = c(6, 20, 42, 236); s.outline = c(106, 184, 255, 102);
        s.accent = c(106, 184, 255); s.accent2 = c(255, 110, 110);
        s.text = c(226, 238, 250); s.dim = c(132, 162, 192); s.faint = c(86, 116, 152); s.title = c(240, 247, 255);
        s.plate = c(90, 140, 200, 26); s.plateSelected = c(106, 184, 255, 52);
        s.field = c(8, 26, 50, 160); s.fieldLine = c(106, 184, 255, 82);
        s.bar = c(8, 22, 44, 212); s.barLine = c(106, 184, 255, 112);
        s.slideFrom = c(255, 110, 120); s.slideTo = c(106, 184, 255); s.knob = c(236, 241, 248);

        s = SEISMOGRAPH;
        s.light = true; s.paperShadow = true; s.segmented = true;
        s.corner = 0; s.plateCorner = 0; s.fieldCorner = 0;
        s.windowTop = c(251, 247, 239, 246); s.windowBottom = c(248, 243, 234, 246); s.outline = c(120, 110, 105, 232);
        s.accent = c(192, 36, 28); s.accent2 = c(60, 50, 45);
        s.text = c(52, 46, 42); s.dim = c(122, 114, 106); s.faint = c(166, 158, 150); s.title = c(30, 26, 24);
        s.plate = c(120, 100, 90, 24); s.plateSelected = c(192, 36, 28, 36);
        s.field = c(236, 231, 223, 222); s.fieldLine = c(150, 140, 135, 205);
        s.bar = c(251, 247, 239, 242); s.barLine = c(100, 95, 90, 232);
        s.slideFrom = c(192, 36, 28); s.slideTo = c(192, 36, 28); s.knob = c(251, 247, 239);

        s = GEODE;
        s.segmented = true;
        s.corner = -12; s.plateCorner = 2; s.fieldCorner = 2;
        s.windowTop = c(62, 24, 92, 204); s.windowBottom = c(34, 10, 54, 228); s.outline = c(213, 140, 255, 112);
        s.accent = c(213, 140, 255); s.accent2 = c(150, 90, 255);
        s.text = c(238, 226, 250); s.dim = c(172, 142, 196); s.faint = c(122, 96, 152); s.title = c(248, 240, 255);
        s.plate = c(200, 140, 255, 22); s.plateSelected = c(213, 140, 255, 52);
        s.field = c(40, 14, 64, 160); s.fieldLine = c(213, 140, 255, 92);
        s.bar = c(26, 8, 40, 218); s.barLine = c(213, 140, 255, 122);
        s.slideFrom = c(213, 140, 255); s.slideTo = c(213, 140, 255); s.knob = c(248, 240, 255);

        s = ORBIT;
        s.corner = 4; s.plateCorner = 3; s.fieldCorner = 3;
        s.windowTop = c(6, 10, 24, 234); s.windowBottom = c(3, 6, 16, 242); s.outline = c(60, 110, 190, 142);
        s.accent = c(90, 169, 255); s.accent2 = c(255, 170, 60);
        s.text = c(226, 236, 250); s.dim = c(122, 142, 176); s.faint = c(76, 94, 126); s.title = c(240, 246, 255);
        s.plate = c(70, 110, 180, 22); s.plateSelected = c(60, 130, 230, 52);
        s.field = c(8, 14, 32, 170); s.fieldLine = c(60, 110, 190, 122);
        s.bar = c(4, 8, 20, 218); s.barLine = c(60, 110, 190, 152);
        s.slideFrom = c(255, 170, 90); s.slideTo = c(90, 169, 255); s.knob = c(236, 241, 250);

        s = AMBER;
        s.corner = 28; s.plateCorner = 14; s.fieldCorner = 10;
        s.windowTop = c(122, 64, 10, 204); s.windowBottom = c(70, 32, 6, 228); s.outline = c(255, 190, 80, 122);
        s.accent = c(255, 192, 74); s.accent2 = c(255, 150, 40);
        s.text = c(250, 228, 190); s.dim = c(202, 162, 106); s.faint = c(152, 114, 66); s.title = c(255, 240, 215);
        s.plate = c(255, 190, 90, 26); s.plateSelected = c(255, 192, 74, 62);
        s.field = c(80, 40, 8, 150); s.fieldLine = c(255, 190, 80, 102);
        s.bar = c(90, 46, 8, 205); s.barLine = c(255, 190, 80, 132);
        s.slideFrom = c(255, 130, 30); s.slideTo = c(255, 192, 74); s.knob = c(250, 236, 216);
    }
}
