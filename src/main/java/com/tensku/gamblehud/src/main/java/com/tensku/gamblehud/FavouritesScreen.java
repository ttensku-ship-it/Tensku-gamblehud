package com.tensku.gamblehud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Favourite hosts: click a name to make them the auction host. */
public class FavouritesScreen extends Screen {
    private static final int W = 260, H = 226;
    private record Hit(int x, int y, int w, int h, Runnable action) {}

    private final List<Hit> hits = new ArrayList<>();
    private int px, py;
    private EditBox addBox;
    private String status = "";

    public FavouritesScreen() {
        super(Component.literal("Favourites"));
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        addBox = new EditBox(font, px + 12, py + 44, 130, 18, Component.literal(""));
        addBox.setMaxLength(16);
        addRenderableWidget(addBox);
        addRenderableWidget(Button.builder(Component.literal("Add"), b -> {
            String n = addBox.getValue().trim();
            if (n.length() < 3) { status = "Enter a name"; return; }
            if (Config.I.favourites.size() >= 8) { status = "Max 8 favourites"; return; }
            for (String f : Config.I.favourites) if (f.equalsIgnoreCase(n)) { status = "Already added"; return; }
            Config.I.favourites.add(n);
            Config.save();
            addBox.setValue("");
            status = "Added " + n;
        }).bounds(px + 148, py + 43, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> minecraft.gui.setScreen(new HubScreen()))
            .bounds(px + 12, py + H - 34, 80, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        Theme th = Theme.current();
        g.fill(0, 0, width, height, 0x99000000);
        Ui.panel(g, th, px, py, W, H);
        g.fill(px + 2, py + 2, px + W - 2, py + 26, 0xAA0E0F16);
        g.text(font, TenskuGambleHud.TITLE + " - FAVOURITES", px + 14, py + 10, th.accent(), true);
        g.text(font, "ADD HOST", px + 12, py + 33, Theme.alpha(th.accent() & 0xFFFFFF, 0.85f), true);

        String cur = Config.I.host.isEmpty() ? "none" : Config.I.host;
        g.text(font, "HOST: " + cur, px + 12, py + 70, 0xFFFFFFFF, true);

        int y0 = py + 84;
        List<String> favs = Config.I.favourites;
        for (int i = 0; i < favs.size(); i++) {
            final String name = favs.get(i);
            int ry = y0 + i * 17;
            boolean sel = name.equalsIgnoreCase(Config.I.host);
            g.fill(px + 12, ry, px + W - 12, ry + 15, sel ? th.accent() : 0xCC151824);
            g.text(font, name, px + 18, ry + 4, sel ? 0xFF10121A : 0xFFE8E8E8, !sel);
            g.text(font, "x", px + W - 24, ry + 4, sel ? 0xFF10121A : 0xFFFF6B6B, !sel);
            hits.add(new Hit(px + W - 30, ry, 18, 15, () -> { Config.I.favourites.remove(name); Config.save(); status = "Removed"; }));
            hits.add(new Hit(px + 12, ry, W - 44, 15, () -> { Config.I.host = name; Config.save(); status = "Host set to " + name; }));
        }
        if (favs.isEmpty()) g.text(font, "No favourites yet.", px + 12, y0 + 2, 0xFFB9BDCB, true);

        if (!status.isEmpty()) g.text(font, status, px + 100, py + H - 28, 0xFF9DFFB0, true);
        String credit = TenskuGambleHud.CREDIT;
        g.text(font, credit, px + W - 14 - font.width(credit), py + H - 12, 0xFF9AA0B5, true);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
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
