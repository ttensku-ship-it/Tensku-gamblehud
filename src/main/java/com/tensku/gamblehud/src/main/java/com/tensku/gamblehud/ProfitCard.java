package com.tensku.gamblehud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ProfitCard {
    private ProfitCard() {}

    public static void render(GuiGraphicsExtractor g, Minecraft mc) {
        if (!Config.I.profitCard) return;
        Theme th = Theme.current();
        double net = Config.I.net();
        int x = 6, y = 6, w = 124, h = Config.I.dailyGoal > 0 ? 40 : 30;

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, th.accent());
        g.fillGradient(x, y, x + w, y + h, th.top(), th.bottom());
        g.text(mc.font, "PROFIT", x + 5, y + 4, Theme.alpha(th.accent(), 1f), true);
        String v = (net >= 0 ? "+" : "") + Money.format(net);
        g.text(mc.font, v, x + 5, y + 15, net >= 0 ? 0xFF7CFF9B : 0xFFFF6B6B, true);

        if (Config.I.dailyGoal > 0) {
            float p = (float) Math.max(0, Math.min(1, net / Config.I.dailyGoal));
            g.fill(x + 5, y + h - 8, x + w - 5, y + h - 4, 0x99000000);
            g.fill(x + 5, y + h - 8, x + 5 + (int) ((w - 10) * p), y + h - 4, th.accent());
        }
    }
}
