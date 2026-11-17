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

/**
 * Client-side helper for Minecraft 26.3 (Fabric).
 *
 * When a server-provided container-style GUI opens, it closes that GUI and
 * sends the command "autoclicker" once. It does not repeatedly send the
 * command every tick while the same GUI is open.
 */
public final class AutoCloseCommandClient implements ClientModInitializer {
    private static final String MOD_ID = "autoclosecommand";

    // 54 server/container slots + 36 player inventory slots = 90 total.
    // We identify the target GUI by its slot layout, never by item types,
    // colors, names, or positions of the changing items.
    private static final int TARGET_MENU_SLOT_COUNT = 90;

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "main")
    );

    private static final KeyMapping TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.autoclosecommand.toggle",
                    InputConstants.Type.KEYBOARD,
                    InputConstants.KEY_F8,
                    CATEGORY
            )
    );

    // Enabled automatically as soon as the mod is loaded.
    private static boolean enabled = true;

    // Remember the exact screen object so a different container can trigger
    // even if the server replaces one container screen directly with another.
    private static Screen lastHandledScreen = null;

    // Wait two client ticks after closing the GUI before sending the command.
    // This gives the close-container packet time to be processed first.
    private static int pendingCommandTicks = -1;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoCloseCommandClient::onClientTick);
    }

    private static void onClientTick(Minecraft client) {
        // F8 switches the automation on/off.
        while (TOGGLE_KEY.consumeClick()) {
            enabled = !enabled;
            if (!enabled) {
                pendingCommandTicks = -1;
            }
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal(
                        "[AutoCloseCommand] " + (enabled ? "WŁĄCZONY" : "WYŁĄCZONY")
                ));
            }
        }

        if (client.player == null) {
            lastHandledScreen = null;
            pendingCommandTicks = -1;
            return;
        }

        // Send the command only after the container has been closed for two ticks.
        // Sending immediately in the same tick as the close can race the server's
        // container-close handling on some servers.
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

        // First make sure this really is a container screen. Keep the cast in
        // its own statement so the reference is available below when closing.
        if (!(screen instanceof AbstractContainerScreen<?>)) {
            lastHandledScreen = null;
            return;
        }
        AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;

        // We only target a separate server container with 54 upper slots plus
        // the 36 player-inventory slots (90 total). The normal player inventory
        // is ignored. No items, colors, names, or individual slot positions are
        // inspected.
        boolean targetScreen = containerScreen.getMenu() != client.player.inventoryMenu
                && containerScreen.getMenu().slots.size() == TARGET_MENU_SLOT_COUNT;

        if (!targetScreen) {
            lastHandledScreen = null;
            return;
        }

        if (screen == lastHandledScreen) {
            return;
        }

        // Mark it first, so this event can never fire twice for the same GUI.
        lastHandledScreen = screen;

        // Use the container screen's close handler, matching the normal Escape action.
        // It closes the container properly instead of only hiding the GUI locally.
        containerScreen.onClose();

        // Queue the command for two ticks later, after the close has been sent.
        // sendCommand() uses the normal server command path; no leading slash.
        pendingCommandTicks = 2;
    }
}
