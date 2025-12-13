package com.tumod.villagerreroll;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.village.TradeOffer;

public class TradeUtils {

    public static boolean hasUnbreakingIII(VillagerEntity villager) {
        for (TradeOffer offer : villager.getOffers()) {
            ItemStack sell = offer.getSellItem();

            ItemEnchantmentsComponent enchants =
                sell.get(DataComponentTypes.STORED_ENCHANTMENTS);

            if (enchants == null) continue;

            for (RegistryEntry<Enchantment> enchantment : enchants.getEnchantments()) {
                if (enchantment.matchesKey(Enchantments.UNBREAKING)
                        && enchants.getLevel(enchantment) == 3) {
                    return true;
                }
            }
        }
        return false;
    }
}
