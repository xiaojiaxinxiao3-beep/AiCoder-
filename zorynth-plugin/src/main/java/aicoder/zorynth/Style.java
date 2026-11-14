package aicoder.zorynth;

import java.util.List;

/**
 * Palettes taken from the Meteor addons. primary/text/secondary go to the host theme; the rest
 * drives the backdrop. The three light themes (Glacier, Topography, Seismograph) keep their accent
 * but use light text, because the host's own panels stay dark.
 */
record Style(String id, String name, String tag, Kind kind, int primary, int text, int secondary,
             int top, int bottom, int a, int b, String footer) {
    enum Kind { CYBER, GLASS, AURORA, STRATA, MAGMA, GLACIER, ABYSS, TOPOGRAPHY, ATMOSPHERE, SEISMOGRAPH, GEODE, ORBIT, AMBER }

    static final List<Style> ALL = List.of(
        new Style("cyberpunk", "AiCoder Cyberpunk", "CYBER", Kind.CYBER, 0xFF00F0FF, 0xFFEAF6FF, 0xFF6F8AA6,
            0x1C0628, 0x06081A, 0x00F0FF, 0xFF2A6D, "[SYS] v0.3   LINK STABLE"),
        new Style("liquid_glass", "AiCoder Liquid Glass", "GLASS", Kind.GLASS, 0xFF3484FF, 0xFFF2F6FF, 0xFFA9B6D6,
            0x0A1230, 0x040612, 0x3484FF, 0x8E3AE8, "GLASS   BLUR ON"),
        new Style("aurora", "AiCoder Aurora", "AURORA", Kind.AURORA, 0xFF4DFFB8, 0xFFEEFAFF, 0xFF9FB4C6,
            0x061226, 0x02040E, 0x4DFFB8, 0x8F7BFF, "AURORA   KP 6"),
        new Style("strata", "AiCoder 01 Strata", "STRATA", Kind.STRATA, 0xFFE8A060, 0xFFECDCC8, 0xFFAC9278,
            0x4A3020, 0x281A14, 0xE8A060, 0xCC703E, "DEPTH 2,340 m   AGE 310 Ma   LAYER 08 / 11"),
        new Style("magma", "AiCoder 02 Magma", "MAGMA", Kind.MAGMA, 0xFFFF5A1F, 0xFFF0E2D8, 0xFFA48070,
            0x180B08, 0x0C0605, 0xFF5A1F, 0xE23414, "TEMP 1,180 C   SiO2 52%   VEI 3"),
        new Style("glacier", "AiCoder 03 Glacier", "GLACIER", Kind.GLACIER, 0xFF5AAAEB, 0xFFF2F9FF, 0xFF8EACCA,
            0x12324E, 0x0A1C30, 0x7EC2F6, 0xFFFFFF, "ICE -12 C   FLOW 34 m/yr   ALBEDO 0.82"),
        new Style("abyss", "AiCoder 04 Abyss", "ABYSS", Kind.ABYSS, 0xFF2FE0C8, 0xFFD8F0F6, 0xFF7AACBA,
            0x0A3C52, 0x041C30, 0x2FE0C8, 0x2896FF, "DEPTH 3,800 m   PRESSURE 381 atm   TEMP 2.1 C"),
        new Style("topography", "AiCoder 05 Topography", "TOPO", Kind.TOPOGRAPHY, 0xFFD08A4E, 0xFFFAF3E4, 0xFFB4A088,
            0x3A2A1C, 0x241A12, 0xD08A4E, 0x3C78BE, "N 23 58'   E 120 58'   ELEV 1,640 m"),
        new Style("atmosphere", "AiCoder 06 Atmosphere", "ATMOS", Kind.ATMOSPHERE, 0xFF6AB8FF, 0xFFE2EEFA, 0xFF84A2C0,
            0x0C2240, 0x06142A, 0x6AB8FF, 0xFF6E6E, "PRESSURE 1013 hPa   WIND NE 18 kt   HUMIDITY 78%"),
        new Style("seismograph", "AiCoder 07 Seismograph", "SEISMO", Kind.SEISMOGRAPH, 0xFFE0463C, 0xFFFBF7EF, 0xFFA69E96,
            0x2A2624, 0x181614, 0xE0463C, 0xFBF7EF, "MAG M 4.7   DEPTH 12 km   PGA 0.08 g"),
        new Style("geode", "AiCoder 08 Geode", "GEODE", Kind.GEODE, 0xFFD58CFF, 0xFFEEE2FA, 0xFFAC8EC4,
            0x3E185C, 0x220A36, 0xD58CFF, 0x965AFF, "QUARTZ SiO2   HARDNESS 7   LUSTER VITREOUS"),
        new Style("orbit", "AiCoder 09 Orbit", "ORBIT", Kind.ORBIT, 0xFF5AA9FF, 0xFFE2ECFA, 0xFF7A8EB0,
            0x060A18, 0x030610, 0x5AA9FF, 0xFFAA3C, "ALT 408 km   VEL 7.66 km/s"),
        new Style("amber", "AiCoder 10 Amber", "AMBER", Kind.AMBER, 0xFFFFC04A, 0xFFFAE4BE, 0xFFCAA26A,
            0x7A400A, 0x462006, 0xFFC04A, 0xFF9628, "AGE 99 Ma   PERIOD Cretaceous   RESIN Burmite"));
}
