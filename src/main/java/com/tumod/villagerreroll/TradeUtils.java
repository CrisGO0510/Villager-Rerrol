package com.tumod.villagerreroll;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public class TradeUtils {

    // Verifica si hay Unbreaking III
    public static boolean hasUnbreakingIII(TradeOfferList offers) {
        if (offers == null || offers.isEmpty()) return false;

        for (TradeOffer offer : offers) {
            ItemStack sell = offer.getSellItem();
            ItemEnchantmentsComponent enchants = getEnchantments(sell);
            if (enchants == null) continue;

            for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
                if (entry.matchesKey(Enchantments.UNBREAKING) && enchants.getLevel(entry) == 3) {
                    return true;
                }
            }
        }
        return false;
    }

    // Método auxiliar para obtener encantamientos de Libros o Items
    private static ItemEnchantmentsComponent getEnchantments(ItemStack stack) {
        ItemEnchantmentsComponent enchants = stack.get(DataComponentTypes.STORED_ENCHANTMENTS);
        if (enchants == null) {
            enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
        }
        return enchants;
    }

    // NUEVO: Imprime los trades encontrados en el chat
    public static void printTrades(MinecraftClient client, TradeOfferList offers) {
        if (offers.isEmpty()) return;
        
        // Solo nos interesa el primer trade de libros (generalmente el primero o segundo slot)
        for (TradeOffer offer : offers) {
            ItemStack sell = offer.getSellItem();
            
            // Si es un libro encantado
            if (sell.isOf(Items.ENCHANTED_BOOK)) {
                ItemEnchantmentsComponent enchants = getEnchantments(sell);
                if (enchants != null) {
                    for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
                        // Obtener nombre traducido (o key si no hay traducción cargada)
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
