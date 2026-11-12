package com.tensku.gamblehud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfirmPayScreen extends Screen {
    private final String command;
    private final String target;
    private final double amount;

    public ConfirmPayScreen(String command, String target, double amount) {
        super(Component.literal("Confirm payment"));
        this.command = command;
        this.target = target;
        this.amount = amount;
    }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;
        addRenderableWidget(Button.builder(Component.literal("Confirm"), b -> {
            onClose();
            TenskuGambleHud.sendCommandBypass(command);
        }).bounds(cx - 105, cy + 20, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
            .bounds(cx + 5, cy + 20, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Theme th = Theme.current();
        int cx = width / 2, cy = height / 2;
        g.fill(0, 0, width, height, 0xAA000000);
        g.fill(cx - 130, cy - 50, cx + 130, cy + 50, th.accent());
        g.fillGradient(cx - 128, cy - 48, cx + 128, cy + 48, th.top(), th.bottom());
        String t = TenskuGambleHud.TITLE;
        g.text(font, t, cx - font.width(t) / 2, cy - 40, th.accent(), true);
        String l1 = "Send " + Money.format(amount) + " to " + target + "?";
        g.text(font, l1, cx - font.width(l1) / 2, cy - 14, 0xFFFFFFFF, true);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }
}
