package aicoder.zorynth;

import java.util.List;

/**
 * One row per theme, palettes copied from the Meteor addons.
 * accent/text/dim also go to the host theme (HUD colours); win*, plate*, field* are the window chrome;
 * top/bottom/a/b drive the backdrop. corner: positive = radius, negative = 45 degree cut, 0 = square.
 */
record Style(String id, String name, String tag, Kind kind, Toggle toggle, boolean light, boolean segmented,
             int corner, int rowCorner, int winTop, int winBottom, int outline, int accent, int accent2,
             int text, int dim, int faint, int plate, int plateSelected, int field, int fieldLine,
             int top, int bottom, int a, int b, String footer) {
    enum Kind { CYBER, GLASS, AURORA, STRATA, MAGMA, GLACIER, ABYSS, TOPOGRAPHY, ATMOSPHERE, SEISMOGRAPH, GEODE, ORBIT, AMBER }
    enum Toggle { HEXAGON, DROPLET, BEAD, STRATA, EMBER, DIAMOND, SPHERE, TARGET, RADIO, CHECK, PLANET }

    int primary() { return accent; }
    int secondary() { return dim; }

    static final List<Style> ALL = List.of(
        new Style("cyberpunk", "AiCoder Cyberpunk", "CYBER", Kind.CYBER, Toggle.HEXAGON, false, true, -9, -5,
            0xD81C0628, 0xE206081A, 0x73FF2A6D, 0xFF00F0FF, 0xFFFF2A6D, 0xFFEAF6FF, 0xFF6F8AA6, 0xFF4A6078,
            0x1400F0FF, 0x38FF2A6D, 0x9006081A, 0x6000E5FF, 0x1C0628, 0x06081A, 0x00F0FF, 0xFF2A6D, "[SYS] v0.3   LINK STABLE"),
        new Style("liquid_glass", "AiCoder Liquid Glass", "GLASS", Kind.GLASS, Toggle.DROPLET, false, false, 16, 9,
            0xB0283C78, 0xB8221C54, 0x80FFFFFF, 0xFF6AA8FF, 0xFFB07CFF, 0xFFF2F6FF, 0xFFB4C0DE, 0xFF8090B4,
            0x1CFFFFFF, 0x403484FF, 0x50060A1E, 0x50FFFFFF, 0x0A1230, 0x040612, 0x3484FF, 0x8E3AE8, "GLASS   TINT ON"),
        new Style("aurora", "AiCoder Aurora", "AURORA", Kind.AURORA, Toggle.BEAD, false, false, 14, 8,
            0xA8123048, 0xB00C1C3A, 0x90C8FFF0, 0xFF4DFFB8, 0xFF8F7BFF, 0xFFEEFAFF, 0xFFA8BCCC, 0xFF74889C,
            0x18E2F4FF, 0x384DFFB8, 0x6B030A1A, 0x50A0FFE1, 0x061226, 0x02040E, 0x4DFFB8, 0x8F7BFF, "AURORA   KP 6"),
        new Style("strata", "AiCoder 01 Strata", "STRATA", Kind.STRATA, Toggle.STRATA, false, true, 7, 5,
            0xEC4A3020, 0xF2281A14, 0x96A0764C, 0xFFE8A060, 0xFFCC703E, 0xFFECDCC8, 0xFFAC9278, 0xFF7A6452,
            0x10FFD6A0, 0x2CE8A060, 0x8C1E120C, 0x6EC4925E, 0x4A3020, 0x281A14, 0xE8A060, 0xCC703E, "DEPTH 2,340 m   AGE 310 Ma   LAYER 08 / 11"),
        new Style("magma", "AiCoder 02 Magma", "MAGMA", Kind.MAGMA, Toggle.EMBER, false, false, 3, 2,
            0xEC180B08, 0xF20C0605, 0x8CFF5A1F, 0xFFFF5A1F, 0xFFE23414, 0xFFF0E2D8, 0xFFA48070, 0xFF705248,
            0x0EFF783C, 0x2EFF5A1F, 0x96280E08, 0x5AFF5A1F, 0x180B08, 0x0C0605, 0xFF5A1F, 0xE23414, "TEMP 1,180 C   SiO2 52%   VEI 3"),
        new Style("glacier", "AiCoder 03 Glacier", "GLACIER", Kind.GLACIER, Toggle.DIAMOND, true, false, 10, 6,
            0xE4F2F9FF, 0xE4CCE4F6, 0xD2FFFFFF, 0xFF1F7FD0, 0xFF5AAAEB, 0xFF203E60, 0xFF5C7EA0, 0xFF8EACCA,
            0x2E78AAD7, 0x7896C8F0, 0x82FFFFFF, 0xA078AAD7, 0x12324E, 0x0A1C30, 0x7EC2F6, 0xFFFFFF, "ICE -12 C   FLOW 34 m/yr   ALBEDO 0.82"),
        new Style("abyss", "AiCoder 04 Abyss", "ABYSS", Kind.ABYSS, Toggle.SPHERE, false, false, 11, 7,
            0xDA0A3C52, 0xE8041C30, 0x5C2FE0C8, 0xFF2FE0C8, 0xFF2896FF, 0xFFD8F0F6, 0xFF7AACBA, 0xFF487A8E,
            0x1C3CA0AA, 0x342FE0C8, 0x96042838, 0x522FE0C8, 0x0A3C52, 0x041C30, 0x2FE0C8, 0x2896FF, "DEPTH 3,800 m   PRESSURE 381 atm   TEMP 2.1 C"),
        new Style("topography", "AiCoder 05 Topography", "TOPO", Kind.TOPOGRAPHY, Toggle.TARGET, true, false, 2, 1,
            0xF0FAF3E4, 0xF0F6ECD8, 0xCD966E50, 0xFFA0521C, 0xFF3C78BE, 0xFF483424, 0xFF846A56, 0xFFAC9884,
            0x1CA06E46, 0x3EC88C5A, 0xCDF0E4CD, 0xA5AA8C6E, 0x3A2A1C, 0x241A12, 0xD08A4E, 0x3C78BE, "N 23 58'   E 120 58'   ELEV 1,640 m"),
        new Style("atmosphere", "AiCoder 06 Atmosphere", "ATMOS", Kind.ATMOSPHERE, Toggle.RADIO, false, false, 6, 5,
            0xE20C2240, 0xEC06142A, 0x666AB8FF, 0xFF6AB8FF, 0xFFFF6E6E, 0xFFE2EEFA, 0xFF84A2C0, 0xFF567498,
            0x1A5A8CC8, 0x346AB8FF, 0xA0081A32, 0x526AB8FF, 0x0C2240, 0x06142A, 0x6AB8FF, 0xFF6E6E, "PRESSURE 1013 hPa   WIND NE 18 kt   HUMIDITY 78%"),
        new Style("seismograph", "AiCoder 07 Seismograph", "SEISMO", Kind.SEISMOGRAPH, Toggle.CHECK, true, true, 0, 0,
            0xF6FBF7EF, 0xF6F8F3EA, 0xE8786E69, 0xFFC0241C, 0xFF3C322D, 0xFF342E2A, 0xFF7A726A, 0xFFA69E96,
            0x1878645A, 0x24C0241C, 0xDEECE7DF, 0xCD968C87, 0x2A2624, 0x181614, 0xE0463C, 0xFBF7EF, "MAG M 4.7   DEPTH 12 km   PGA 0.08 g"),
        new Style("geode", "AiCoder 08 Geode", "GEODE", Kind.GEODE, Toggle.HEXAGON, false, true, -7, 1,
            0xCC3E185C, 0xE4220A36, 0x70D58CFF, 0xFFD58CFF, 0xFF965AFF, 0xFFEEE2FA, 0xFFAC8EC4, 0xFF7A6098,
            0x16C88CFF, 0x34D58CFF, 0xA0280E40, 0x5CD58CFF, 0x3E185C, 0x220A36, 0xD58CFF, 0x965AFF, "QUARTZ SiO2   HARDNESS 7   LUSTER VITREOUS"),
        new Style("orbit", "AiCoder 09 Orbit", "ORBIT", Kind.ORBIT, Toggle.PLANET, false, false, 3, 2,
            0xEA060A18, 0xF2030610, 0x8E3C6EBE, 0xFF5AA9FF, 0xFFFFAA3C, 0xFFE2ECFA, 0xFF7A8EB0, 0xFF4C5E7E,
            0x16466EB4, 0x343C82E6, 0xAA080E20, 0x7A3C6EBE, 0x060A18, 0x030610, 0x5AA9FF, 0xFFAA3C, "ALT 408 km   VEL 7.66 km/s"),
        new Style("amber", "AiCoder 10 Amber", "AMBER", Kind.AMBER, Toggle.BEAD, false, false, 16, 9,
            0xCC7A400A, 0xE4462006, 0x7AFFBE50, 0xFFFFC04A, 0xFFFF9628, 0xFFFAE4BE, 0xFFCAA26A, 0xFF987242,
            0x1AFFBE5A, 0x3EFFC04A, 0x96502808, 0x66FFBE50, 0x7A400A, 0x462006, 0xFFC04A, 0xFF9628, "AGE 99 Ma   PERIOD Cretaceous   RESIN Burmite"));
}
