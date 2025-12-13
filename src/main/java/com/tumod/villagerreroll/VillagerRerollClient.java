package com.tumod.villagerreroll;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class VillagerRerollClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        System.out.println("Villager Reroll cargado");

        MinecraftClient.getInstance().execute(() -> {
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("Villager Reroll activo"), false
                );
            }
        });
    }
}

