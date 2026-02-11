package dev.wyfy.createbackpackage;

import net.minecraft.world.item.ItemStack;

public interface BackpackUpgradeItem {
    default boolean isEnabled(ItemStack stack) {
        return stack.getOrDefault(
            CreateBackpackage.UPGRADE_ENABLED.get(),
            true
        );
    }
}
