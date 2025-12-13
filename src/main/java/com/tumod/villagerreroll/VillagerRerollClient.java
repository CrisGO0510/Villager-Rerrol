package com.tumod.villagerreroll;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.text.Text;
import net.minecraft.village.VillagerProfession;

public class VillagerRerollClient implements ClientModInitializer {

    private static boolean enabled = false;
    private static int delayTicks = 0;

    @Override
    public void onInitializeClient() {
        Keybinds.register();
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null) return;

        while (Keybinds.TOGGLE.wasPressed()) {
            enabled = !enabled;
            client.player.sendMessage(
                Text.literal("Villager Reroll: " + (enabled ? "ON" : "OFF")),
                true
            );
        }

        if (!enabled) return;
        if (delayTicks-- > 0) return;

        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof EntityHitResult ehr)) return;
        if (!(ehr.getEntity() instanceof VillagerEntity villager)) return;

        if (villager.getVillagerData().profession() != VillagerProfession.LIBRARIAN)
            return;

        if (TradeUtils.hasUnbreakingIII(villager)) {
            client.player.sendMessage(
                Text.literal("§a✔ Unbreaking III encontrado"),
                false
            );
            enabled = false;
            return;
        }

        // rompe el lectern (bloque más común: debajo)
        client.interactionManager.attackBlock(
            villager.getBlockPos().down(),
            client.player.getHorizontalFacing()
        );

        delayTicks = 20; // 1 segundo
    }
}
