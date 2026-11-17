package com.dawixo.autoclosecommand;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Client-side automation for Minecraft 26.3 (Fabric). */
public final class AutoCloseCommandClient implements ClientModInitializer {
    private static final String MOD_ID = "autoclosecommand";
    private static final int TARGET_MENU_SLOT_COUNT = 90;
    private static final int TICKS_PER_SECOND = 20;

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "main")
    );

    // F8: original GUI + /autoclicker automation.
    private static final KeyMapping TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.autoclosecommand.toggle", InputConstants.Type.KEYBOARD,
                    InputConstants.KEY_F8, CATEGORY)
    );

    // F7: separate AutoSell toggle. It does not change the F8 automation.
    private static final KeyMapping TOGGLE_AUTOSELL_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.autoclosecommand.toggle_sellall", InputConstants.Type.KEYBOARD,
                    InputConstants.KEY_F7, CATEGORY)
    );

    private static boolean enabled = true;
    private static Screen lastHandledScreen = null;
    private static int pendingCommandTicks = -1;
    private static long autoSellElapsedTicks = 0L;

    @Override
    public void onInitializeClient() {
        AutoCloseCommandConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(AutoCloseCommandClient::onClientTick);
    }

    private static void onClientTick(Minecraft client) {
        // F8 controls only the original AutoCloseCommand feature.
        while (TOGGLE_KEY.consumeClick()) {
            enabled = !enabled;
            if (!enabled) {
                pendingCommandTicks = -1;
            }
            sendStatus(client, "AutoCloseCommand " + (enabled ? "WŁĄCZONY" : "WYŁĄCZONY"));
        }

        // F7 controls only AutoSell and persists its state.
        while (TOGGLE_AUTOSELL_KEY.consumeClick()) {
            boolean newState = !AutoCloseCommandConfig.isAutoSellEnabled();
            AutoCloseCommandConfig.setAutoSellEnabled(newState);
            autoSellElapsedTicks = 0L;
            sendStatus(client, "AutoSell /sellall " + (newState ? "WŁĄCZONY" : "WYŁĄCZONY")
                    + " (co " + AutoCloseCommandConfig.getAutoSellIntervalSeconds() + " s)");
        }

        if (client.player == null) {
            lastHandledScreen = null;
            pendingCommandTicks = -1;
            autoSellElapsedTicks = 0L;
            return;
        }

        // AutoSell runs independently from F8. It waits one full interval
        // after connecting or being enabled before sending the first command.
        if (AutoCloseCommandConfig.isAutoSellEnabled()) {
            autoSellElapsedTicks++;
            long intervalTicks = (long) AutoCloseCommandConfig.getAutoSellIntervalSeconds()
                    * TICKS_PER_SECOND;
            if (autoSellElapsedTicks >= intervalTicks) {
                client.player.connection.sendCommand("sellall");
                autoSellElapsedTicks = 0L;
            }
        } else {
            autoSellElapsedTicks = 0L;
        }

        // Preserve V5: close the matching GUI, then wait two ticks before /autoclicker.
        if (pendingCommandTicks >= 0) {
            if (!enabled) {
                pendingCommandTicks = -1;
                return;
            }
            pendingCommandTicks--;
            if (pendingCommandTicks < 0) {
                client.player.connection.sendCommand("autoclicker");
            }
            return;
        }

        if (!enabled) {
            return;
        }

        Screen screen = client.gui.screen();
        if (!(screen instanceof AbstractContainerScreen<?>)) {
            lastHandledScreen = null;
            return;
        }

        AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;
        boolean targetScreen = containerScreen.getMenu() != client.player.inventoryMenu
                && containerScreen.getMenu().slots.size() == TARGET_MENU_SLOT_COUNT;

        if (!targetScreen) {
            lastHandledScreen = null;
            return;
        }

        if (screen == lastHandledScreen) {
            return;
        }

        lastHandledScreen = screen;
        containerScreen.onClose();
        pendingCommandTicks = 2;
    }

    private static void sendStatus(Minecraft client, String message) {
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal("[AutoCloseCommand] " + message));
        }
    }
}
