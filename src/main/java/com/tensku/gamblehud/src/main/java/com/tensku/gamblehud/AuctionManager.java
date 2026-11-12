package com.tensku.gamblehud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Chat-driven auction: bids are chat lines that are just an amount, e.g. "<name> 5M" or "name: 5M". */
public final class AuctionManager {
    private AuctionManager() {}

    private static final Pattern BID = Pattern.compile(
        "(?i)^(?:\\[[^\\]]*\\]\\s*)*(?:<(\\w{3,16})>|(\\w{3,16})\\s*[»:>])\\s*(?:bid\\s+)?\\$?(\\d[\\d.,]*[kmbt]?)\\s*$");

    public record Result(String item, String winner, double amount) {}

    public static boolean active = false;
    public static String item = "";
    public static double startBid = 0;
    public static double topBid = 0;
    public static String topBidder = "";
    public static long endAt = 0;
    public static long totalMs = 1;
    public static final List<String> log = new ArrayList<>();

    public static Result result = null;
    public static long bannerUntil = 0;
    public static long resultUntil = 0;

    public static void start(String name, int seconds, double minBid) {
        item = name.isBlank() ? "Auction item" : name.trim();
        startBid = Math.max(0, minBid);
        topBid = 0;
        topBidder = "";
        totalMs = Math.max(5, seconds) * 1000L;
        endAt = System.currentTimeMillis() + totalMs;
        log.clear();
        active = true;
        result = null;
        bannerUntil = 0;
        resultUntil = 0;
        Config.I.auctionEnabled = true;
        Config.save();
    }

    public static void cancel() {
        active = false;
        result = null;
        bannerUntil = 0;
        resultUntil = 0;
    }

    public static void endNow() {
        if (active) finish();
    }

    private static void finish() {
        active = false;
        String winner = topBidder.isEmpty() ? null : topBidder;
        result = new Result(item, winner, topBid);
        long now = System.currentTimeMillis();
        bannerUntil = winner != null ? now + Config.I.winnerBannerSeconds * 1000L : 0;
        resultUntil = Config.I.resultDisplaySeconds > 0 ? now + Config.I.resultDisplaySeconds * 1000L : 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            String msg = winner == null
                ? "[Tensku] Auction ended: no bids for " + item
                : "[Tensku] Auction ended: " + winner + " won " + item + " for " + Money.format(topBid);
            mc.player.sendSystemMessage(Component.literal(msg));
        }
    }

    /** Called for every game chat line. */
    public static void onChat(String text) {
        if (!active || !Config.I.auctionEnabled) return;
        Matcher m = BID.matcher(text.trim());
        if (!m.matches()) return;
        String who = m.group(1) != null ? m.group(1) : m.group(2);
        if (who.equalsIgnoreCase(Config.I.host)) return; // host does not bid on own auction
        double amount = Money.parse(m.group(3));
        if (amount <= 0 || amount < startBid || amount <= topBid) return;
        topBid = amount;
        topBidder = who;
        log.add(0, who + "  " + Money.format(amount));
        while (log.size() > 8) log.remove(log.size() - 1);
    }

    public static void render(GuiGraphicsExtractor g, Minecraft mc, int sw, int sh) {
        if (!Config.I.auctionEnabled) return;
        long now = System.currentTimeMillis();
        if (active && now >= endAt) finish();
        Theme th = Theme.current();
        int w = 134, x = sw - w - 6, y = 6;

        if (active) {
            long left = Math.max(0, endAt - now);
            int h = 54;
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, th.accent());
            g.fillGradient(x, y, x + w, y + h, th.top(), th.bottom());
            g.text(mc.font, "AUCTION", x + 5, y + 4, th.accent(), true);
            String tm = Ui.time(left);
            boolean urgent = left <= 10_000;
            int tc = urgent && (now / 400) % 2 == 0 ? 0xFFFF6B6B : 0xFFFFFFFF;
            g.text(mc.font, tm, x + w - 5 - mc.font.width(tm), y + 4, tc, true);
            g.text(mc.font, Ui.fit(mc.font, item, w - 10), x + 5, y + 16, 0xFFFFFFFF, true);
            String top = topBidder.isEmpty()
                ? "No bids - from " + Money.format(startBid)
                : Money.format(topBid) + " - " + topBidder;
            g.text(mc.font, Ui.fit(mc.font, top, w - 10), x + 5, y + 28, 0xFF7CFF9B, true);
            if (!Config.I.host.isEmpty()) {
                g.text(mc.font, Ui.fit(mc.font, "Host: " + Config.I.host, w - 10), x + 5, y + 39, 0xFFB9BDCB, true);
            }
            float frac = Math.max(0f, Math.min(1f, left / (float) totalMs));
            g.fill(x + 5, y + h - 5, x + w - 5, y + h - 2, 0x99000000);
            g.fill(x + 5, y + h - 5, x + 5 + (int) ((w - 10) * frac), y + h - 2, th.accent());
            y += h + 4;
        }

        if (result != null && Config.I.resultDisplaySeconds > 0 && now < resultUntil) {
            int h = 42;
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, th.accent());
            g.fillGradient(x, y, x + w, y + h, th.top(), th.bottom());
            g.text(mc.font, "AUCTION RESULT", x + 5, y + 4, th.accent(), true);
            g.text(mc.font, Ui.fit(mc.font, result.item(), w - 10), x + 5, y + 16, 0xFFFFFFFF, true);
            String line = result.winner() == null ? "No bids"
                : result.winner() + " - " + Money.format(result.amount());
            g.text(mc.font, Ui.fit(mc.font, line, w - 10), x + 5, y + 28, result.winner() == null ? 0xFFB9BDCB : 0xFF7CFF9B, true);
        }

        if (result != null && result.winner() != null && now < bannerUntil) {
            float leftS = (bannerUntil - now) / 1000f;
            float fade = Math.min(1f, leftS / 0.5f);
            float pop = Math.min(1f, (Config.I.winnerBannerSeconds - leftS) / 0.35f);
            String l1 = "AUCTION WON";
            String l2 = result.winner() + "  " + Money.format(result.amount());
            String l3 = Ui.fit(mc.font, result.item(), 220);
            int bw = Math.max(200, (int) (mc.font.width(l2) * 1.6f) + 30);
            int bh = 50;
            int bx = (sw - bw) / 2;
            int by = (int) (sh / 4f - 30 + 30 * pop);
            float pulse = (float) (0.5 + 0.5 * Math.sin(now / 120.0));
            g.fill(bx - 2, by - 2, bx + bw + 2, by + bh + 2, Theme.alpha(0xF2C94C, fade * (0.7f + 0.3f * pulse)));
            g.fillGradient(bx, by, bx + bw, by + bh, Theme.alpha(th.top() & 0xFFFFFF, fade * 0.95f),
                           Theme.alpha(th.bottom() & 0xFFFFFF, fade * 0.95f));
            g.text(mc.font, l1, (sw - mc.font.width(l1)) / 2, by + 5, Theme.alpha(th.accent() & 0xFFFFFF, fade), true);
            g.pose().pushMatrix();
            g.pose().translate(sw / 2f, by + 17f);
            g.pose().scale(1.6f, 1.6f);
            g.text(mc.font, l2, -mc.font.width(l2) / 2, 0, Theme.alpha(0x7CFF9B, fade), true);
            g.pose().popMatrix();
            g.text(mc.font, l3, (sw - mc.font.width(l3)) / 2, by + bh - 12, Theme.alpha(0xFFE9B0, fade), true);
        }
    }
}
