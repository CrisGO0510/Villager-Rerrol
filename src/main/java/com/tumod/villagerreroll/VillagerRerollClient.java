package com.tumod.villagerreroll;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;

public class VillagerRerollClient implements ClientModInitializer {

    private enum State {
        IDLE,
        FIND_WORKSTATION,
        BREAK_BLOCK,
        WAIT_FOR_JOB_LOSS,
        PLACE_BLOCK,
        WAIT_FOR_JOB_GAIN,
        OPEN_GUI,
        WAIT_FOR_GUI_OPEN,
        VERIFY_TRADES
    }

    private static boolean enabled = false;
    private static State currentState = State.IDLE;
    private static int timer = 0;
    private static int timeoutGui = 0;
    
    private VillagerEntity targetVillager;
    private BlockPos workstationPos;

    @Override
    public void onInitializeClient() {
        Keybinds.register();
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        // Toggle Keybind
        while (Keybinds.TOGGLE.wasPressed()) {
            enabled = !enabled;
            currentState = State.IDLE;
            targetVillager = null;
            workstationPos = null;
            client.player.sendMessage(Text.literal("§6Villager Reroll: " + (enabled ? "§aON" : "§cOFF")), true);
            // Si cancelamos, aseguramos dejar de romper
            client.interactionManager.cancelBlockBreaking();
        }

        if (!enabled) return;
        if (timer > 0) {
            timer--;
            return;
        }

        switch (currentState) {
            case IDLE:
                if (client.crosshairTarget instanceof EntityHitResult ehr 
                    && ehr.getEntity() instanceof VillagerEntity villager) {
                    
                    this.targetVillager = villager;
                    if (isLibrarian(villager) || isUnemployed(villager)) {
                        client.player.sendMessage(Text.literal("§eIniciando ciclo..."), true);
                        currentState = State.FIND_WORKSTATION;
                    }
                }
                break;

            case FIND_WORKSTATION:
                workstationPos = findNearestLectern(client, targetVillager.getBlockPos());
                if (workstationPos != null) {
                    if (isLibrarian(targetVillager)) {
                        currentState = State.BREAK_BLOCK;
                    } else {
                        currentState = State.PLACE_BLOCK;
                    }
                } else {
                    client.player.sendMessage(Text.literal("§cNo se encontró atril (radio 4)."), false);
                    enabled = false;
                }
                break;

            case BREAK_BLOCK:
                // 1. Cerrar GUI si estorba
                if (client.currentScreen != null) {
                    client.player.closeHandledScreen();
                }

                // 2. Verificar si el bloque ya se rompió (es Aire)
                if (client.world.getBlockState(workstationPos).isAir()) {
                    // Ya se rompió, paramos de minar y avanzamos
                    client.interactionManager.cancelBlockBreaking();
                    timer = 5; 
                    currentState = State.WAIT_FOR_JOB_LOSS;
                    return;
                }

                // 3. SURVIVAL MODE: Hay que mantener la mirada y el "click"
                lookAtBlock(client, workstationPos);
                
                // updateBlockBreakingProgress simula mantener el click izquierdo
                // Si devuelve true, es que se rompió en este tick.
                boolean broken = client.interactionManager.updateBlockBreakingProgress(workstationPos, Direction.UP);
                client.player.swingHand(Hand.MAIN_HAND);

                if (broken) {
                    timer = 5;
                    currentState = State.WAIT_FOR_JOB_LOSS;
                }
                // Si no se rompió (false), el estado NO CAMBIA. 
                // Volverá a entrar aquí en el siguiente tick para seguir minando.
                break;

            case WAIT_FOR_JOB_LOSS:
                if (isUnemployed(targetVillager)) {
                    timer = 5;
                    currentState = State.PLACE_BLOCK;
                }
                break;

            case PLACE_BLOCK:
                if (!client.player.getMainHandStack().isOf(Items.LECTERN)) {
                    client.player.sendMessage(Text.literal("§c¡Necesitas un Atril en la mano!"), false);
                    enabled = false;
                    return;
                }

                // Mirar para ponerlo bien
                lookAtBlock(client, workstationPos);

                BlockHitResult hitResult = new BlockHitResult(
                    workstationPos.toCenterPos(), Direction.UP, workstationPos, false
                );
                
                // Interactuar una sola vez
                client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
                client.player.swingHand(Hand.MAIN_HAND);
                
                timer = 20; // Esperar a que el aldeano lo detecte
                currentState = State.WAIT_FOR_JOB_GAIN;
                break;

            case WAIT_FOR_JOB_GAIN:
                if (isLibrarian(targetVillager)) {
                    timer = 10; 
                    currentState = State.OPEN_GUI;
                }
                break;

            case OPEN_GUI:
                lookAtEntity(client, targetVillager);
                client.interactionManager.interactEntity(client.player, targetVillager, Hand.MAIN_HAND);
                timeoutGui = 40; 
                currentState = State.WAIT_FOR_GUI_OPEN;
                break;

            case WAIT_FOR_GUI_OPEN:
                if (client.player.currentScreenHandler instanceof MerchantScreenHandler) {
                    timer = 5; 
                    currentState = State.VERIFY_TRADES;
                } else {
                    timeoutGui--;
                    if (timeoutGui <= 0) {
                        currentState = State.OPEN_GUI;
                    }
                }
                break;

            case VERIFY_TRADES:
                if (client.player.currentScreenHandler instanceof MerchantScreenHandler merchantHandler) {
                    TradeOfferList offers = merchantHandler.getRecipes();
                    
                    // Log
                    TradeUtils.printTrades(client, offers);

                    // Check
                    if (TradeUtils.hasUnbreakingIII(offers)) {
                        client.player.sendMessage(Text.literal("§a✔ ¡Unbreaking III ENCONTRADO!"), false);
                        enabled = false;
                        currentState = State.IDLE;
                    } else {
                        client.player.closeHandledScreen();
                        timer = 10; 
                        currentState = State.BREAK_BLOCK;
                    }
                } else {
                    currentState = State.OPEN_GUI;
                }
                break;
        }
    }

    // --- Helpers ---

    private void lookAtBlock(MinecraftClient client, BlockPos pos) {
        Vec3d playerPos = client.player.getEyePos();
        Vec3d targetPos = pos.toCenterPos();
        Vec3d dir = targetPos.subtract(playerPos);

        double diffX = dir.x;
        double diffZ = dir.z;
        double diffY = dir.y;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(diffY, dist) * 180.0 / Math.PI));

        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
    }
    
    private void lookAtEntity(MinecraftClient client, VillagerEntity entity) {
        Vec3d playerPos = client.player.getEyePos();
        
        double targetX = entity.getX();
        double targetY = entity.getY() + entity.getEyeHeight(entity.getPose());
        double targetZ = entity.getZ();
        
        Vec3d targetPos = new Vec3d(targetX, targetY, targetZ);
        Vec3d dir = targetPos.subtract(playerPos);

        double diffX = dir.x;
        double diffZ = dir.z;
        double diffY = dir.y;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(diffY, dist) * 180.0 / Math.PI));

        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
    }

    private boolean isLibrarian(VillagerEntity villager) {
        return villager.getVillagerData().profession().matchesKey(VillagerProfession.LIBRARIAN);
    }

    private boolean isUnemployed(VillagerEntity villager) {
        return villager.getVillagerData().profession().matchesKey(VillagerProfession.NONE);
    }

    private BlockPos findNearestLectern(MinecraftClient client, BlockPos center) {
        int radius = 4;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (client.world.getBlockState(pos).isOf(Blocks.LECTERN)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
}
