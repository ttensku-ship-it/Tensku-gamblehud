package com.tensku.gamblehud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared panel drawing (gradient, hills, drifting particles). */
public final class Ui {
    private Ui() {}

    public static void panel(GuiGraphicsExtractor g, Theme th, int px, int py, int w, int h) {
        g.fill(px, py, px + w, py + h, 0xFF0E0F16);
        int x1 = px + 2, y1 = py + 2, x2 = px + w - 2, y2 = py + h - 2;
        g.fillGradient(x1, y1, x2, y2, th.top(), th.bottom());
        for (int x = x1; x < x2; x += 2) {
            int hh = (int) (22 + 12 * Math.sin(x * 0.03) + 6 * Math.sin(x * 0.08));
            g.fill(x, y2 - hh, x + 2, y2, th.hill());
        }
        long t = System.currentTimeMillis();
        for (int i = 0; i < 36; i++) {
            int seed = i * 7919;
            float fx = ((seed % 397) + (t * (0.004f + (i % 5) * 0.002f))) % (w - 4);
            float fy = (((seed / 3) % 211) + (t * (0.006f + (i % 3) * 0.003f))) % (h - 4);
            g.fill(x1 + (int) fx, y1 + (int) fy, x1 + (int) fx + 2, y1 + (int) fy + 2, 0x99FFFFFF);
        }
    }

    public static String time(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    public static String fit(net.minecraft.client.gui.Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) return s;
        while (s.length() > 1 && font.width(s + "..") > maxWidth) s = s.substring(0, s.length() - 1);
        return s + "..";
    }
}
