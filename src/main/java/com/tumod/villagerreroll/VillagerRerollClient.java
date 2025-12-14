package com.tumod.villagerreroll;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;

public class VillagerRerollClient implements ClientModInitializer {

    // --- VARIABLES DE CONFIGURACIÓN ---
    public static Identifier targetEnchantment = Identifier.of("minecraft", "unbreaking");
    public static int targetLevel = 3;

    private enum State {
        IDLE, FIND_WORKSTATION, BREAK_BLOCK, WAIT_FOR_JOB_LOSS, PLACE_BLOCK, WAIT_FOR_JOB_GAIN, OPEN_GUI, WAIT_FOR_GUI_OPEN, VERIFY_TRADES
    }

    private static boolean enabled = false;
    private static State currentState = State.IDLE;
    private static int timer = 0;
    private static int timeoutGui = 0;
    private boolean isMining = false;
    
    private VillagerEntity targetVillager;
    private BlockPos workstationPos;

    @Override
    public void onInitializeClient() {
        Keybinds.register();
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
        registerCommands();
    }

    // --- COMANDOS ---
    private void registerCommands() {
        SuggestionProvider<FabricClientCommandSource> ENCHANTMENT_SUGGESTIONS = (context, builder) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                Registry<?> registry = client.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
                return CommandSource.suggestMatching(
                    registry.getIds().stream().map(Identifier::toString),
                    builder
                );
            }
            return CommandSource.suggestMatching(new String[]{}, builder);
        };

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("vr")
                .then(ClientCommandManager.literal("status")
                    .executes(context -> {
                        context.getSource().sendFeedback(Text.literal(
                            "§eBuscando actualmente: §f" + targetEnchantment + " §anivel " + targetLevel
                        ));
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("set")
                    .then(ClientCommandManager.argument("name", IdentifierArgumentType.identifier())
                        .suggests(ENCHANTMENT_SUGGESTIONS)
                        .then(ClientCommandManager.argument("level", IntegerArgumentType.integer(1, 5))
                            .executes(context -> {
                                // CORRECCIÓN AQUÍ: Usamos getArgument directamente para evitar error de tipos
                                Identifier id = context.getArgument("name", Identifier.class);
                                int level = IntegerArgumentType.getInteger(context, "level");

                                MinecraftClient client = MinecraftClient.getInstance();
                                if (client.world != null) {
                                    boolean exists = client.world.getRegistryManager()
                                            .getOrThrow(RegistryKeys.ENCHANTMENT)
                                            .containsId(id);
                                    
                                    if (!exists) {
                                        context.getSource().sendFeedback(Text.literal("§cError: El encantamiento '" + id + "' no existe."));
                                        return 0;
                                    }
                                }

                                targetEnchantment = id;
                                targetLevel = level;

                                context.getSource().sendFeedback(Text.literal(
                                    "§aConfigurado! Buscando: §f" + id + " §eLv " + level
                                ));
                                return 1;
                            })
                        )
                    )
                )
            );
        });
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        while (Keybinds.TOGGLE.wasPressed()) {
            enabled = !enabled;
            resetState(client); 
            client.player.sendMessage(Text.literal("§6Villager Reroll: " + (enabled ? "§aON" : "§cOFF") + 
                " §7(" + targetEnchantment.getPath() + " " + targetLevel + ")"), true);
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
                        client.player.sendMessage(Text.literal("§eIniciando... Buscando: " + targetEnchantment.getPath() + " " + targetLevel), true);
                        currentState = State.FIND_WORKSTATION;
                    }
                }
                break;

            case FIND_WORKSTATION:
                workstationPos = findNearestLectern(client, targetVillager.getBlockPos());
                if (workstationPos != null) {
                    if (isLibrarian(targetVillager)) {
                        currentState = State.BREAK_BLOCK;
                        isMining = false; 
                    } else {
                        currentState = State.PLACE_BLOCK;
                    }
                } else {
                    client.player.sendMessage(Text.literal("§cNo se encontró atril (radio 4)."), false);
                    enabled = false;
                }
                break;

            case BREAK_BLOCK:
                if (client.world.getBlockState(workstationPos).isAir()) {
                    client.interactionManager.cancelBlockBreaking();
                    isMining = false;
                    timer = 5; 
                    currentState = State.WAIT_FOR_JOB_LOSS;
                    return;
                }
                if (client.currentScreen != null) client.player.closeHandledScreen();
                lookAtBlock(client, workstationPos);
                
                if (!isMining) {
                    client.interactionManager.attackBlock(workstationPos, Direction.UP);
                    client.player.swingHand(Hand.MAIN_HAND);
                    isMining = true;
                } else {
                    client.interactionManager.updateBlockBreakingProgress(workstationPos, Direction.UP);
                    client.player.swingHand(Hand.MAIN_HAND);
                }
                break;

            case WAIT_FOR_JOB_LOSS:
                if (isUnemployed(targetVillager)) {
                    timer = 5;
                    currentState = State.PLACE_BLOCK;
                }
                break;

            case PLACE_BLOCK:
                if (!client.player.getOffHandStack().isOf(Items.LECTERN)) {
                    client.player.sendMessage(Text.literal("§c¡Necesitas el Atril en la MANO SECUNDARIA!"), false);
                    enabled = false;
                    return;
                }
                lookAtBlock(client, workstationPos);
                BlockHitResult hitResult = new BlockHitResult(workstationPos.toCenterPos(), Direction.UP, workstationPos, false);
                client.interactionManager.interactBlock(client.player, Hand.OFF_HAND, hitResult);
                client.player.swingHand(Hand.OFF_HAND);
                timer = 20; 
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
                    if (timeoutGui <= 0) currentState = State.OPEN_GUI;
                }
                break;

            case VERIFY_TRADES:
                if (client.player.currentScreenHandler instanceof MerchantScreenHandler merchantHandler) {
                    TradeOfferList offers = merchantHandler.getRecipes();
                    TradeUtils.printTrades(client, offers);

                    // Verifica que TradeUtils tenga el método matchesTarget
                    if (TradeUtils.matchesTarget(offers)) {
                        client.player.sendMessage(Text.literal("§a✔ ¡OBJETIVO ENCONTRADO!"), false);
                        enabled = false;
                        resetState(client);
                    } else {
                        client.player.closeHandledScreen();
                        timer = 10; 
                        currentState = State.BREAK_BLOCK;
                        isMining = false; 
                    }
                } else {
                    currentState = State.OPEN_GUI;
                }
                break;
        }
    }

    private void resetState(MinecraftClient client) {
        currentState = State.IDLE;
        targetVillager = null;
        workstationPos = null;
        isMining = false;
        if (client.interactionManager != null) client.interactionManager.cancelBlockBreaking();
    }

    private void lookAtBlock(MinecraftClient client, BlockPos pos) {
        Vec3d playerPos = client.player.getEyePos();
        Vec3d targetPos = pos.toCenterPos();
        Vec3d dir = targetPos.subtract(playerPos);
        double diffX = dir.x; double diffZ = dir.z; double diffY = dir.y;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(diffY, dist) * 180.0 / Math.PI));
        client.player.setYaw(yaw); client.player.setPitch(pitch);
    }
    
    private void lookAtEntity(MinecraftClient client, VillagerEntity entity) {
        Vec3d playerPos = client.player.getEyePos();
        double targetX = entity.getX();
        double targetY = entity.getY() + entity.getEyeHeight(entity.getPose());
        double targetZ = entity.getZ();
        Vec3d targetPos = new Vec3d(targetX, targetY, targetZ);
        Vec3d dir = targetPos.subtract(playerPos);
        double diffX = dir.x; double diffZ = dir.z; double diffY = dir.y;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(diffY, dist) * 180.0 / Math.PI));
        client.player.setYaw(yaw); client.player.setPitch(pitch);
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
                    if (client.world.getBlockState(pos).isOf(Blocks.LECTERN)) return pos;
                }
            }
        }
        return null;
    }
}
