package com.tensku.gamblehud;

/** Colour themes (ARGB). Chat colour is the RGB used to tint chat messages. */
public record Theme(String name, int top, int bottom, int hill, int accent, int chat) {

    public static final Theme[] ALL = {
        new Theme("Winter",    0xFF1B2A44, 0xFF3C5E86, 0xFF8FB4D6, 0xFF9AD1FF, 0x9AD1FF),
        new Theme("Aurora",    0xFF0B2A33, 0xFF1D7A6B, 0xFF2BB59A, 0xFF52F2C8, 0x52F2C8),
        new Theme("Galaxy",    0xFF1A0B33, 0xFF4B1D8C, 0xFF7B3CD6, 0xFFC08BFF, 0xC08BFF),
        new Theme("Desert",    0xFF2A1608, 0xFFD9822B, 0xFF3A2410, 0xFFFFB347, 0xFFB347),
        new Theme("Aesthetic", 0xFF8A4E8F, 0xFFF08FB0, 0xFFB0587F, 0xFFFFA6D5, 0xFFA6D5),
        new Theme("Volcano",   0xFF1C0805, 0xFF8A1E0F, 0xFF2A0F0A, 0xFFFF5A36, 0xFF5A36),
        new Theme("Space",     0xFF05060F, 0xFF16264F, 0xFF0B1230, 0xFF5B7CFF, 0x5B7CFF),
    };

    public static Theme current() {
        int i = Config.I.theme;
        return ALL[Math.max(0, Math.min(ALL.length - 1, i))];
    }

    public static int alpha(int rgb, float a) {
        int al = Math.max(0, Math.min(255, (int) (a * 255f)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }
}
