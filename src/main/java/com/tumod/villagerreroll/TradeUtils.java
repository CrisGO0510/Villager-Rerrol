package com.tumod.villagerreroll;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public class TradeUtils {

    // ESTE ES EL MÉTODO QUE FALTABA
    public static boolean matchesTarget(TradeOfferList offers) {
        if (offers == null || offers.isEmpty()) return false;

        for (TradeOffer offer : offers) {
            ItemStack sell = offer.getSellItem();
            ItemEnchantmentsComponent enchants = getEnchantments(sell);
            if (enchants == null) continue;

            for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
                // Comparamos el ID del encantamiento con el configurado
                if (entry.matchesId(VillagerRerollClient.targetEnchantment)) {
                    // Verificamos si el nivel es IGUAL o MAYOR al deseado
                    if (enchants.getLevel(entry) >= VillagerRerollClient.targetLevel) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static ItemEnchantmentsComponent getEnchantments(ItemStack stack) {
        ItemEnchantmentsComponent enchants = stack.get(DataComponentTypes.STORED_ENCHANTMENTS);
        if (enchants == null) {
            enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
        }
        return enchants;
    }

    public static void printTrades(MinecraftClient client, TradeOfferList offers) {
        if (offers.isEmpty()) return;
        
        for (TradeOffer offer : offers) {
            ItemStack sell = offer.getSellItem();
            if (sell.isOf(Items.ENCHANTED_BOOK)) {
                ItemEnchantmentsComponent enchants = getEnchantments(sell);
                if (enchants != null) {
                    for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
                        String name = entry.value().description().getString();
                        int level = enchants.getLevel(entry);
                        
                        client.player.sendMessage(
                            Text.literal("§7Reroll: §f" + name + " " + level), 
                            false
                        );
                    }
                }
            }
        }
    }
}
