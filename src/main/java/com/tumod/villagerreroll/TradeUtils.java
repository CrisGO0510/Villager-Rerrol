package com.tumod.villagerreroll;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public class TradeUtils {

    public static boolean hasUnbreakingIII(TradeOfferList offers) {
        for (TradeOffer offer : offers) {
            ItemStack sell = offer.getSellItem();

            if (sell.getItem() instanceof EnchantedBookItem) {
                var enchants = EnchantedBookItem.getEnchantmentNbt(sell);

                for (var entry : enchants) {
                    Enchantment ench = Enchantment.byRawId(entry.getInt("id"));
                    int level = entry.getInt("lvl");

                    if (ench == Enchantments.UNBREAKING && level == 3) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

