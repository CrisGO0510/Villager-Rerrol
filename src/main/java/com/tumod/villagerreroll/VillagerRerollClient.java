package com.tumod.villagerreroll;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class VillagerRerollClient implements ClientModInitializer {

    private static boolean enabled = false;

    @Override
    public void onInitializeClient() {
        System.out.println(">>> VILLAGER REROLL CLIENT INIT <<<");

        Keybinds.register();

        ClientTickEvents.START_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null) return;
        if (client.currentScreen != null) return;

        while (Keybinds.TOGGLE.wasPressed()) {
            enabled = !enabled;
            client.player.sendMessage(
                Text.literal("Villager Reroll: " + (enabled ? "ON" : "OFF")),
                true
            );
        }

        if (!enabled) return;

        // aquí luego va la lógica del reroll
    }
}
