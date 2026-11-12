package com.tensku.gamblehud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class HubScreen extends Screen {
    private static final int W = 400, H = 240;
    private static final String[] TABS = {"Overview", "Auction", "Profit", "Pay Confirm", "Alerts", "Chat", "Themes"};

    private record Hit(int x, int y, int w, int h, Runnable action) {}

    private final List<Hit> hits = new ArrayList<>();
    private int tab = 0;
    private int px, py;
    private EditBox payBox, alertBox, goalBox, winnerBox, resultBox;
    private String status = "";

    public HubScreen() {
        super(Component.literal(TenskuGambleHud.TITLE));
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        int cx = px + 124, y0 = py + 36;

        payBox = box(cx, y0 + 46, Money.format(Config.I.payThreshold).replace("$", ""));
        goalBox = box(cx, y0 + 46, Config.I.dailyGoal > 0 ? Money.format(Config.I.dailyGoal).replace("$", "") : "");
        alertBox = box(cx, y0 + 70, Money.format(Config.I.alertThreshold).replace("$", ""));
        winnerBox = box(cx, y0 + 78, String.valueOf(Config.I.winnerBannerSeconds));
        resultBox = box(cx, y0 + 116, String.valueOf(Config.I.resultDisplaySeconds));
        updateBoxes();
    }

    private EditBox box(int x, int y, String value) {
        EditBox b = new EditBox(font, x, y, 110, 18, Component.literal("amount"));
        b.setMaxLength(16);
        b.setValue(value);
        addRenderableWidget(b);
        return b;
    }

    private void updateBoxes() {
        payBox.setVisible(tab == 3);
        goalBox.setVisible(tab == 2);
        alertBox.setVisible(tab == 4);
        winnerBox.setVisible(tab == 1);
        resultBox.setVisible(tab == 1);
    }

    // ---------- drawing helpers ----------

    private void toggle(GuiGraphicsExtractor g, Theme th, String label, boolean on, int y, Runnable r) {
        int x = px + 124, w = 168, h = 20;
        g.fill(x, y, x + w, y + h, 0xCC151824);
        g.text(font, label, x + 6, y + 6, 0xFFE8E8E8, true);
        int sx = x + w - 34, sy = y + 5;
        g.fill(sx, sy, sx + 28, sy + 10, on ? th.accent() : 0xFF555A68);
        int kx = on ? sx + 18 : sx;
        g.fill(kx, sy, kx + 10, sy + 10, 0xFFFFFFFF);
        hits.add(new Hit(x, y, w, h, r));
    }

    private void button(GuiGraphicsExtractor g, Theme th, String label, int x, int y, int w, int h, Runnable r) {
        g.fill(x, y, x + w, y + h, 0xCC151824);
        g.fill(x, y, x + w, y + 1, th.accent());
        g.text(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, 0xFFE8E8E8, true);
        hits.add(new Hit(x, y, w, h, r));
    }

    private static int parseSeconds(String s) {
        try {
            int v = Integer.parseInt(s.trim());
            return (v >= 0 && v <= 60) ? v : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void label(GuiGraphicsExtractor g, Theme th, String s, int y) {
        g.text(font, s, px + 124, y, Theme.alpha(th.accent() & 0xFFFFFF, 0.8f), true);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        Theme th = Theme.current();
        updateBoxes();

        g.fill(0, 0, width, height, 0x99000000);
        Ui.panel(g, th, px, py, W, H);
        g.fill(px + 2, py + 2, px + W - 2, py + 28, 0xAA0E0F16);

        g.text(font, TenskuGambleHud.TITLE, px + 14, py + 10, th.accent(), true);
        g.text(font, "ESC - close", px + 14, py + H - 14, 0xFF9AA0B5, true);
        String credit = TenskuGambleHud.CREDIT;
        g.text(font, credit, px + W - 14 - font.width(credit), py + H - 14, 0xFF9AA0B5, true);

        // tabs
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            int ty = py + 36 + i * 26;
            boolean sel = i == tab;
            g.fill(px + 12, ty, px + 112, ty + 22, sel ? th.accent() : 0xCC151824);
            g.text(font, TABS[i], px + 20, ty + 7, sel ? 0xFF10121A : 0xFFE8E8E8, !sel);
            hits.add(new Hit(px + 12, ty, 100, 22, () -> { tab = idx; updateBoxes(); status = ""; }));
        }

        int y0 = py + 36;
        Config c = Config.I;
        switch (tab) {
            case 0 -> {
                label(g, th, "OVERVIEW", y0 - 12);
                toggle(g, th, "Auction", c.auctionEnabled, y0, () -> { c.auctionEnabled = !c.auctionEnabled; Config.save(); });
                toggle(g, th, "Payment Alerts", c.paymentAlerts, y0 + 22, () -> { c.paymentAlerts = !c.paymentAlerts; Config.save(); });
                toggle(g, th, "Profit Tracker", c.profitCard, y0 + 44, () -> { c.profitCard = !c.profitCard; Config.save(); });
                toggle(g, th, "Pay Confirm", c.payConfirm, y0 + 66, () -> { c.payConfirm = !c.payConfirm; Config.save(); });
                toggle(g, th, "Chat Color", c.chatColor, y0 + 88, () -> { c.chatColor = !c.chatColor; Config.save(); });
                button(g, th, "Open Auction", px + 124, y0 + 114, 82, 20, () -> minecraft.gui.setScreen(new AuctionScreen()));
                button(g, th, "Test Animation", px + 210, y0 + 114, 82, 20, () -> PaymentAnimation.trigger(5_000_000, "Tensku"));
            }
            case 1 -> {
                label(g, th, "AUCTION", y0 - 12);
                toggle(g, th, "Auction (bids + HUD)", c.auctionEnabled, y0, () -> { c.auctionEnabled = !c.auctionEnabled; Config.save(); });
                button(g, th, "Open Auction", px + 124, y0 + 24, 82, 20, () -> minecraft.gui.setScreen(new AuctionScreen()));
                button(g, th, "Favourites", px + 210, y0 + 24, 82, 20, () -> minecraft.gui.setScreen(new FavouritesScreen()));
                g.text(font, "HOST: " + (c.host.isEmpty() ? "-" : c.host), px + 124, y0 + 50, 0xFFE8E8E8, true);
                label(g, th, "WINNER BANNER TIME (0-60s)", y0 + 66);
                button(g, th, "Set", px + 124 + 116, y0 + 78, 52, 18, () -> {
                    int v = parseSeconds(winnerBox.getValue());
                    if (v >= 0) { c.winnerBannerSeconds = v; Config.save(); status = "Banner time saved"; } else status = "Use 0-60";
                });
                label(g, th, "RESULT DISPLAY (0-60s; 0=off)", y0 + 104);
                button(g, th, "Set", px + 124 + 116, y0 + 116, 52, 18, () -> {
                    int v = parseSeconds(resultBox.getValue());
                    if (v >= 0) { c.resultDisplaySeconds = v; Config.save(); status = "Result time saved"; } else status = "Use 0-60";
                });
            }
            case 2 -> {
                label(g, th, "PROFIT", y0 - 12);
                toggle(g, th, "Profit Tracker + Card", c.profitCard, y0, () -> { c.profitCard = !c.profitCard; Config.save(); });
                label(g, th, "DAILY GOAL (bar on the card)", y0 + 34);
                button(g, th, "Set", px + 124 + 116, y0 + 46, 52, 18, () -> {
                    double v = goalBox.getValue().isBlank() ? 0 : Money.parse(goalBox.getValue());
                    if (v >= 0) { c.dailyGoal = v; Config.save(); status = "Goal saved"; } else status = "Invalid amount";
                });
                button(g, th, "Reset Today's Profit", px + 124, y0 + 76, 168, 20, () -> { c.received = 0; c.sent = 0; Config.save(); status = "Reset"; });
            }
            case 3 -> {
                label(g, th, "PAY CONFIRM", y0 - 12);
                toggle(g, th, "Pay Confirm", c.payConfirm, y0, () -> { c.payConfirm = !c.payConfirm; Config.save(); });
                label(g, th, "ASK BEFORE /pay AT OR ABOVE", y0 + 34);
                button(g, th, "Set", px + 124 + 116, y0 + 46, 52, 18, () -> {
                    double v = Money.parse(payBox.getValue());
                    if (v >= 0) { c.payThreshold = v; Config.save(); status = "Saved"; } else status = "Invalid amount";
                });
                g.text(font, "A big /pay opens a Confirm / Cancel", px + 124, y0 + 76, 0xFFB9BDCB, true);
                g.text(font, "screen before anything is sent.", px + 124, y0 + 88, 0xFFB9BDCB, true);
            }
            case 4 -> {
                label(g, th, "ALERTS", y0 - 12);
                toggle(g, th, "Payment Alerts", c.paymentAlerts, y0, () -> { c.paymentAlerts = !c.paymentAlerts; Config.save(); });
                toggle(g, th, "Alert Sound", c.alertSound, y0 + 26, () -> { c.alertSound = !c.alertSound; Config.save(); });
                label(g, th, "BIG PAYMENT AT OR ABOVE", y0 + 58);
                button(g, th, "Set", px + 124 + 116, y0 + 70, 52, 18, () -> {
                    double v = Money.parse(alertBox.getValue());
                    if (v >= 0) { c.alertThreshold = v; Config.save(); status = "Saved"; } else status = "Invalid amount";
                });
                button(g, th, "Test Animation", px + 124, y0 + 98, 168, 20, () -> PaymentAnimation.trigger(Math.max(c.alertThreshold, 5_000_000), "Tensku"));
            }
            case 5 -> {
                label(g, th, "CHAT", y0 - 12);
                toggle(g, th, "Chat Color", c.chatColor, y0, () -> { c.chatColor = !c.chatColor; Config.save(); });
                g.text(font, "Chat text uses the theme colour.", px + 124, y0 + 32, 0xFFB9BDCB, true);
                g.text(font, "Server-coloured parts stay as is.", px + 124, y0 + 44, 0xFFB9BDCB, true);
                g.text(font, "Preview:", px + 124, y0 + 64, 0xFFB9BDCB, true);
                g.text(font, "<tensku> this is the chat colour", px + 124, y0 + 76, 0xFF000000 | th.chat(), true);
            }
            case 6 -> {
                label(g, th, "GUI THEMES", y0 - 12);
                for (int i = 0; i < Theme.ALL.length; i++) {
                    final int idx = i;
                    Theme t = Theme.ALL[i];
                    int ry = y0 + i * 20;
                    boolean sel = c.theme == i;
                    g.fill(px + 124, ry, px + 292, ry + 18, sel ? t.accent() : 0xCC151824);
                    g.fillGradient(px + 128, ry + 3, px + 148, ry + 15, t.top(), t.bottom());
                    g.text(font, t.name(), px + 154, ry + 5, sel ? 0xFF10121A : 0xFFE8E8E8, !sel);
                    hits.add(new Hit(px + 124, ry, 168, 18, () -> { c.theme = idx; Config.save(); status = "Selected " + t.name() + "."; }));
                }
            }
            default -> { }
        }

        if (!status.isEmpty()) g.text(font, status, px + 124, py + H - 28, 0xFF9DFFB0, true);

        // right side stat card
        int sx = px + 300, sw = 88;
        g.fill(sx, py + 36, sx + sw, py + 122, 0xCC10121A);
        g.text(font, "BALANCE", sx + 6, py + 42, 0xFFFFFFFF, true);
        String bal = c.balance >= 0 ? Money.format(c.balance) : "type /bal";
        g.text(font, bal, sx + 6, py + 53, c.balance >= 0 ? 0xFFFFFFFF : 0xFF9AA0B5, true);
        g.text(font, "TODAY", sx + 6, py + 68, 0xFFFFFFFF, true);
        double net = c.net();
        g.text(font, (net >= 0 ? "+" : "") + Money.format(net), sx + 6, py + 79, net >= 0 ? 0xFF7CFF9B : 0xFFFF6B6B, true);
        g.text(font, "IN  " + Money.format(c.received), sx + 6, py + 93, 0xFFB9BDCB, true);
        g.text(font, "OUT " + Money.format(c.sent), sx + 6, py + 105, 0xFFB9BDCB, true);

        g.fill(sx, py + 128, sx + sw, py + 196, 0xCC10121A);
        g.text(font, "STATUS", sx + 6, py + 133, th.accent(), true);
        statusLine(g, "Auction", c.auctionEnabled, sx + 6, py + 146);
        statusLine(g, "Alerts", c.paymentAlerts, sx + 6, py + 157);
        statusLine(g, "Profit", c.profitCard, sx + 6, py + 168);
        statusLine(g, "Pay guard", c.payConfirm, sx + 6, py + 179);

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void statusLine(GuiGraphicsExtractor g, String name, boolean on, int x, int y) {
        g.text(font, name, x, y, 0xFFB9BDCB, true);
        String s = on ? "ON" : "OFF";
        g.text(font, s, x + 76 - font.width(s), y, on ? 0xFF7CFF9B : 0xFFFF6B6B, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x() && mx < h.x() + h.w() && my >= h.y() && my < h.y() + h.h()) {
                h.action().run();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
