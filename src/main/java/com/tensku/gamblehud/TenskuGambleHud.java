package com.tensku.gamblehud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.lwjgl.sdl.SDLScancode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TenskuGambleHud implements ClientModInitializer {
    public static final String MOD_ID = "tenskugamblehud";
    public static final String TITLE = "TENSKU GAMBLE HUD";
    public static final String CREDIT = "by tensku";

    private static final Pattern PAID_YOU = Pattern.compile("(\\S+) paid you \\$([0-9][0-9.,]*[KkMmBbTt]?)");
    private static final Pattern YOU_PAID = Pattern.compile("You paid (\\S+) \\$([0-9][0-9.,]*[KkMmBbTt]?)");
    private static final Pattern PAY_CMD = Pattern.compile("^pay\\s+(\\S+)\\s+(\\S+).*$", Pattern.CASE_INSENSITIVE);

    private static KeyMapping openKey;
    private static boolean bypassPayGuard = false;

    @Override
    public void onInitializeClient() {
        Config.load();

        // Keybind (default: Right Shift) - change it in Options > Controls > Key Binds.
        // 26.3 uses SDL scancodes and the 3-argument KeyMapping constructor (no InputConstants.Type).
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.tenskugamblehud.open",
            SDLScancode.SDL_SCANCODE_RSHIFT,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new HubScreen());
                }
            }
        });

        // HUD: profit card + payment animation
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), (graphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
           
            int w = mc.getWindow().getGuiScaledWidth();
            int h = mc.getWindow().getGuiScaledHeight();
            ProfitCard.render(graphics, mc);
            PaymentAnimation.render(graphics, w, h);
        });

        // Tint chat with theme colour (only parts the server did not colour)
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            if (overlay || !Config.I.chatColor) return message;
            int rgb = Theme.current().chat();
            MutableComponent out = Component.empty().withStyle(s -> s.withColor(rgb));
            out.append(message);
            return out;
        });

        // Detect payments
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = message.getString();
            Matcher in = PAID_YOU.matcher(text);
            if (in.find()) {
                double v = Money.parse(in.group(2));
                if (v > 0) {
                    Config.I.rollDay();
                    Config.I.received += v;
                    Config.save();
                    if (Config.I.paymentAlerts && v >= Config.I.alertThreshold) {
                        PaymentAnimation.trigger(v, in.group(1));
                    }
                }
                return;
            }
            Matcher out = YOU_PAID.matcher(text);
            if (out.find()) {
                double v = Money.parse(out.group(2));
                if (v > 0) {
                    Config.I.rollDay();
                    Config.I.sent += v;
                    Config.save();
                }
            }
        });

        // Pay confirm: intercept big /pay commands
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            if (bypassPayGuard || !Config.I.payConfirm) return true;
            Matcher m = PAY_CMD.matcher(command.trim());
            if (!m.matches()) return true;
            double v = Money.parse(m.group(2));
            if (v < 0 || v < Config.I.payThreshold) return true;
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> mc.gui.setScreen(new ConfirmPayScreen(command, m.group(1), v)));
            return false;
        });
    }

    /** Sends a command without being caught by the pay guard again. */
    public static void sendCommandBypass(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        bypassPayGuard = true;
        try {
            mc.getConnection().sendCommand(command);
        } finally {
            bypassPayGuard = false;
        }
    }
}
