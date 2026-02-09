package dev.wyfy.createbackpackage;

import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

public class MagnetUpgradeHandler {

    private static final int CHECK_INTERVAL = 10;
    private static final double PICKUP_RADIUS = 8.0;

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) return;
        if (player.tickCount % CHECK_INTERVAL != 0) return;
        if (player.isShiftKeyDown() || player.isDeadOrDying() || player.isSpectator()) return;

        ItemStack backpackStack = findEquippedBackpack(player);
        if (backpackStack.isEmpty()) return;

        ItemStackHandler inventory = CardboardBackpackItem.getInventoryFromStack(backpackStack);

        ItemStack upgradeStack = inventory.getStackInSlot(BackpackMenu.BACKPACK_SIZE);
        if (!(upgradeStack.getItem() instanceof MagnetUpgradeItem)) return;

        AABB pickupArea = player.getBoundingBox().inflate(PICKUP_RADIUS);
        List<ItemEntity> nearbyItems = player.level().getEntitiesOfClass(
            ItemEntity.class, pickupArea,
            item -> !item.hasPickUpDelay() && item.isAlive()
        );

        if (nearbyItems.isEmpty()) return;

        boolean collected = false;

        for (ItemEntity itemEntity : nearbyItems) {
            ItemStack remaining = itemEntity.getItem().copy();

            for (int slot = 0; slot < BackpackMenu.BACKPACK_SIZE; slot++) {
                remaining = inventory.insertItem(slot, remaining, false);
                if (remaining.isEmpty()) break;
            }

            if (remaining.getCount() < itemEntity.getItem().getCount()) {
                collected = true;
                if (remaining.isEmpty()) {
                    itemEntity.discard();
                } else {
                    itemEntity.setItem(remaining);
                }
            }
        }

        if (collected) {
            CardboardBackpackItem.saveInventoryToStack(backpackStack, inventory);
        }
    }

    private static ItemStack findEquippedBackpack(Player player) {
        // Curios slots
        var curiosOpt = CuriosApi.getCuriosInventory(player);
        if (curiosOpt.isPresent()) {
            var handler = curiosOpt.get();
            for (String slotType : new String[]{"back", "body"}) {
                var result = handler.findCurio(slotType, 0);
                if (result.isPresent() && result.get().stack().getItem() instanceof CardboardBackpackItem) {
                    return result.get().stack();
                }
            }
        }

        // Vanilla chest slot
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestStack.getItem() instanceof CardboardBackpackItem) {
            return chestStack;
        }

        // Hands
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof CardboardBackpackItem) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof CardboardBackpackItem) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }
}
