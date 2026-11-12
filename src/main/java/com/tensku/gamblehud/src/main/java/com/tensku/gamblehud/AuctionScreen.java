package com.tensku.gamblehud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AuctionScreen extends Screen {
    private static final int W = 320, H = 226;
    private int px, py;
    private EditBox itemBox, durBox, bidBox, hostBox;
    private String status = "";

    public AuctionScreen() {
        super(Component.literal("Auction"));
    }

    private EditBox box(int x, int y, int w, String value, int max) {
        EditBox b = new EditBox(font, x, y, w, 18, Component.literal(""));
        b.setMaxLength(max);
        b.setValue(value);
        addRenderableWidget(b);
        return b;
    }

    private void btn(String label, int x, int y, int w, Runnable r) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> r.run()).bounds(x, y, w, 20).build());
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        int x = px + 12;
        itemBox = box(x, py + 44, 150, AuctionManager.item.isEmpty() ? "Auction item" : AuctionManager.item, 40);
        durBox = box(x, py + 78, 150, "60", 5);
        bidBox = box(x, py + 112, 150, Money.format(AuctionManager.startBid).replace("$", ""), 16);
        hostBox = box(x, py + 146, 150, Config.I.host, 16);

        btn("Start", x, py + 172, 74, () -> {
            double minBid = bidBox.getValue().isBlank() ? 0 : Money.parse(bidBox.getValue());
            int secs;
            try { secs = Integer.parseInt(durBox.getValue().trim()); } catch (NumberFormatException e) { secs = -1; }
            if (minBid < 0 || secs < 5) { status = "Check seconds (5+) and bid"; return; }
            Config.I.host = hostBox.getValue().trim();
            Config.save();
            AuctionManager.start(itemBox.getValue(), Math.min(secs, 3600), minBid);
            status = "Auction started";
        });
        btn("End Now", x + 78, py + 172, 72, () -> { AuctionManager.endNow(); status = "Ended"; });
        btn("Cancel", x, py + 196, 74, () -> { AuctionManager.cancel(); status = "Cancelled"; });
        btn("Back", x + 78, py + 196, 72, () -> minecraft.gui.setScreen(new HubScreen()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Theme th = Theme.current();
        g.fill(0, 0, width, height, 0x99000000);
        Ui.panel(g, th, px, py, W, H);
        g.fill(px + 2, py + 2, px + W - 2, py + 26, 0xAA0E0F16);
        g.text(font, TenskuGambleHud.TITLE + " - AUCTION", px + 14, py + 10, th.accent(), true);

        int x = px + 12;
        int lc = Theme.alpha(th.accent() & 0xFFFFFF, 0.85f);
        g.text(font, "ITEM", x, py + 33, lc, true);
        g.text(font, "DURATION (SECONDS)", x, py + 67, lc, true);
        g.text(font, "STARTING BID", x, py + 101, lc, true);
        g.text(font, "HOST (ignored as bidder)", x, py + 135, lc, true);

        int rx = px + 172, rw = 136;
        g.fill(rx, py + 33, rx + rw, py + 118, 0xCC10121A);
        g.text(font, "STATUS", rx + 6, py + 38, th.accent(), true);
        long now = System.currentTimeMillis();
        if (AuctionManager.active) {
            g.text(font, "LIVE  " + Ui.time(Math.max(0, AuctionManager.endAt - now)), rx + 6, py + 50, 0xFF7CFF9B, true);
            String top = AuctionManager.topBidder.isEmpty() ? "No bids yet"
                : Money.format(AuctionManager.topBid) + " " + AuctionManager.topBidder;
            g.text(font, Ui.fit(font, top, rw - 12), rx + 6, py + 62, 0xFFFFFFFF, true);
            g.text(font, Ui.fit(font, AuctionManager.item, rw - 12), rx + 6, py + 74, 0xFFB9BDCB, true);
        } else {
            g.text(font, "IDLE", rx + 6, py + 50, 0xFFFF6B6B, true);
            g.text(font, Config.I.auctionEnabled ? "Auction HUD: ON" : "Auction HUD: OFF", rx + 6, py + 62, 0xFFB9BDCB, true);
        }
        g.text(font, "Bids are chat lines like", rx + 6, py + 90, 0xFF9AA0B5, true);
        g.text(font, "<name> 5M  or  name: 5M", rx + 6, py + 102, 0xFF9AA0B5, true);

        g.fill(rx, py + 124, rx + rw, py + 196, 0xCC10121A);
        g.text(font, "RECENT BIDS", rx + 6, py + 129, th.accent(), true);
        for (int i = 0; i < Math.min(5, AuctionManager.log.size()); i++) {
            g.text(font, Ui.fit(font, AuctionManager.log.get(i), rw - 12), rx + 6, py + 142 + i * 10, 0xFFE8E8E8, true);
        }

        if (!status.isEmpty()) g.text(font, status, rx + 6, py + 202, 0xFF9DFFB0, true);
        g.text(font, "ESC - close", px + 14, py + H - 12, 0xFF9AA0B5, true);
        String credit = TenskuGambleHud.CREDIT;
        g.text(font, credit, px + W - 14 - font.width(credit), py + H - 12, 0xFF9AA0B5, true);

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }
}
