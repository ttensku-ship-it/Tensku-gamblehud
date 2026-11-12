package com.tensku.gamblehud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Big-payment animation: glow, sliding banner and a burst of gold coins. */
public final class PaymentAnimation {
    private PaymentAnimation() {}

    private static final float LIFE = 3.4f;
    private static final Random RNG = new Random();
    private static final List<Fx> ACTIVE = new ArrayList<>();

    private static final class Fx {
        final long start = System.nanoTime();
        final double amount;
        final String from;
        final float[] ang = new float[40];
        final float[] spd = new float[40];
        final int[] col = new int[40];
        Fx(double amount, String from) {
            this.amount = amount;
            this.from = from;
            for (int i = 0; i < ang.length; i++) {
                ang[i] = (float) (-Math.PI * (0.05 + 0.9 * RNG.nextDouble()));
                spd[i] = 90f + RNG.nextFloat() * 220f;
                col[i] = (i % 3 == 0) ? 0xFFF2C94C : (i % 3 == 1 ? 0xFFFFE27A : 0xFFE0A526);
            }
        }
        float t() { return (System.nanoTime() - start) / 1_000_000_000f; }
    }

    public static void trigger(double amount, String from) {
        synchronized (ACTIVE) {
            ACTIVE.add(new Fx(amount, from));
        }
        if (Config.I.alertSound) {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> mc.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.2f)));
        }
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158f, c3 = c1 + 1f;
        float p = x - 1f;
        return 1f + c3 * p * p * p + c1 * p * p;
    }

    public static void render(GuiGraphicsExtractor g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        Theme th = Theme.current();
        synchronized (ACTIVE) {
            ACTIVE.removeIf(f -> f.t() > LIFE);
            for (Fx f : ACTIVE) draw(g, mc, th, f, sw, sh);
        }
    }

    private static void draw(GuiGraphicsExtractor g, Minecraft mc, Theme th, Fx f, int sw, int sh) {
        float t = f.t();
        float fade = t > LIFE - 0.7f ? Math.max(0f, (LIFE - t) / 0.7f) : 1f;

        // screen-edge glow pulse
        float pulse = (float) (0.5 + 0.5 * Math.sin(t * 8f));
        float glow = fade * (0.18f + 0.12f * pulse) * Math.min(1f, t * 4f);
        int gc = Theme.alpha(th.accent(), glow);
        g.fillGradient(0, 0, sw, 40, gc, 0x00000000);
        g.fillGradient(0, sh - 40, sw, sh, 0x00000000, gc);

        // banner
        String tier = f.amount >= 1e9 ? "JACKPOT" : f.amount >= 1e8 ? "HUGE PAYMENT" : "BIG PAYMENT";
        String amt = "+" + Money.format(f.amount);
        String sub = tier + "  -  " + f.from;

        int bw = Math.max(170, mc.font.width(sub) + 30);
        int bh = 46;
        float slide = Math.min(1f, t / 0.55f);
        float y = -bh + (22 + bh) * easeOutBack(slide);
        int bx = (sw - bw) / 2;
        int by = (int) y;

        int border = Theme.alpha(0xF2C94C, fade * (0.7f + 0.3f * pulse));
        g.fill(bx - 2, by - 2, bx + bw + 2, by + bh + 2, border);
        g.fillGradient(bx, by, bx + bw, by + bh, Theme.alpha(th.top() & 0xFFFFFF, fade * 0.95f),
                       Theme.alpha(th.bottom() & 0xFFFFFF, fade * 0.95f));

        // amount (scaled x2) with pop-in
        float pop = 1f + 0.25f * Math.max(0f, 1f - t * 4f);
        float scale = 2f * pop;
        int aw = mc.font.width(amt);
        g.pose().pushMatrix();
        g.pose().translate(sw / 2f, by + 6f);
        g.pose().scale(scale, scale);
        g.text(mc.font, amt, -aw / 2, 0, Theme.alpha(0x7CFF9B, fade), true);
        g.pose().popMatrix();

        int sw2 = mc.font.width(sub);
        g.text(mc.font, sub, (sw - sw2) / 2, by + bh - 13, Theme.alpha(0xFFE9B0, fade), true);

        // shimmer sweep
        float sweep = (t * 1.4f) % 1.6f;
        int sx = bx + (int) (sweep * bw) - 10;
        if (sx > bx && sx < bx + bw - 6) {
            g.fill(sx, by, sx + 6, by + bh, Theme.alpha(0xFFFFFF, fade * 0.18f));
        }

        // coin burst
        float ox = sw / 2f, oy = by + bh / 2f;
        for (int i = 0; i < f.ang.length; i++) {
            float vx = (float) Math.cos(f.ang[i]) * f.spd[i];
            float vy = (float) Math.sin(f.ang[i]) * f.spd[i];
            float px = ox + vx * t;
            float py = oy + vy * t + 0.5f * 380f * t * t;
            if (py > sh + 8) continue;
            int size = (i % 4 == 0) ? 4 : 3;
            int c = (f.col[i] & 0xFFFFFF) | (Math.max(0, Math.min(255, (int) (fade * 255))) << 24);
            g.fill((int) px, (int) py, (int) px + size, (int) py + size, c);
            g.fill((int) px, (int) py, (int) px + 1, (int) py + size, Theme.alpha(0xFFFFFF, fade * 0.7f));
        }
    }
}
