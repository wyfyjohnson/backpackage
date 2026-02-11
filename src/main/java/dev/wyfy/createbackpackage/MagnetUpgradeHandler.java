package dev.wyfy.createbackpackage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import top.theillusivec4.curios.api.CuriosApi;

public class MagnetUpgradeHandler {

    private static final int SCAN_INTERVAL = 5;
    private static final double PICKUP_RADIUS = 5.0;
    private static final float MIN_SPEED = 0.15F;
    private static final float MAX_SPEED = 0.45F;
    private static final int MAX_ITEMS_PER_TICK = 200;
    private static final int MAX_PICKUP_DELAY = 40;
    // Don't attract items thrown by the player for this many ticks
    private static final int THROW_GRACE_TICKS = 60;

    private final WeakHashMap<Player, List<ItemEntity>> cachedNearbyItems =
        new WeakHashMap<>();
    private final WeakHashMap<Player, Set<ItemEntity>> trackedItems =
        new WeakHashMap<>();

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) return;

        if (
            player.isShiftKeyDown() ||
            player.isDeadOrDying() ||
            player.isSpectator()
        ) {
            releaseTrackedItems(player);
            return;
        }

        ItemStack backpackStack = findEquippedBackpack(player);
        if (backpackStack.isEmpty()) {
            releaseTrackedItems(player);
            return;
        }

        ItemStackHandler inventory =
            CardboardBackpackItem.getInventoryFromStack(backpackStack);

        ItemStack upgradeStack = inventory.getStackInSlot(
            BackpackMenu.BACKPACK_SIZE
        );
        if (!(upgradeStack.getItem() instanceof MagnetUpgradeItem)) {
            releaseTrackedItems(player);
            return;
        }
        if (
            !upgradeStack.getOrDefault(
                CreateBackpackage.UPGRADE_ENABLED.get(),
                true
            )
        ) {
            releaseTrackedItems(player);
            return;
        }

        // Refresh entity cache on scan ticks
        if (player.tickCount % SCAN_INTERVAL == 0) {
            AABB pickupArea = player.getBoundingBox().inflate(PICKUP_RADIUS);
            cachedNearbyItems.put(
                player,
                new ArrayList<>(
                    player
                        .level()
                        .getEntitiesOfClass(
                            ItemEntity.class,
                            pickupArea,
                            item ->
                                item.isAlive() &&
                                getPickupDelay(item) < MAX_PICKUP_DELAY &&
                                !isThrownByPlayer(item, player)
                        )
                )
            );
        }

        List<ItemEntity> nearbyItems = cachedNearbyItems.getOrDefault(
            player,
            List.of()
        );
        if (nearbyItems.isEmpty()) {
            releaseTrackedItems(player);
            return;
        }

        Set<ItemEntity> currentlyTracked = trackedItems.computeIfAbsent(
            player,
            k -> new HashSet<>()
        );
        Set<ItemEntity> stillTracked = new HashSet<>();

        Vec3 playerPos = player.position().add(0, 0.75, 0);
        int processed = 0;

        for (ItemEntity itemEntity : nearbyItems) {
            if (processed >= MAX_ITEMS_PER_TICK) break;
            if (!itemEntity.isAlive()) continue;

            Vec3 itemPos = itemEntity.position();
            double distance = playerPos.distanceTo(itemPos);

            // Pull item toward player
            Vec3 direction = playerPos.subtract(itemPos).normalize();
            float t = (float) Math.clamp(
                1.0 - distance / PICKUP_RADIUS,
                0.0,
                1.0
            );
            float speed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * t * t;

            itemEntity.setNoGravity(true);
            itemEntity.setDeltaMovement(direction.scale(speed));
            itemEntity.hasImpulse = true;
            stillTracked.add(itemEntity);

            processed++;
        }

        // Release items we were tracking but are no longer pulling
        for (ItemEntity old : currentlyTracked) {
            if (!stillTracked.contains(old) && old.isAlive()) {
                old.setNoGravity(false);
            }
        }
        currentlyTracked.clear();
        currentlyTracked.addAll(stillTracked);
    }

    /**
     * Intercept vanilla item pickup. When a player with an active magnet backpack
     * picks up an item, route it into the backpack instead of the player inventory.
     */
    @SubscribeEvent
    public void onItemPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide) return;

        if (player.isShiftKeyDown()) return;

        ItemStack backpackStack = findEquippedBackpack(player);
        if (backpackStack.isEmpty()) return;

        ItemStackHandler inventory =
            CardboardBackpackItem.getInventoryFromStack(backpackStack);

        ItemStack upgradeStack = inventory.getStackInSlot(
            BackpackMenu.BACKPACK_SIZE
        );
        if (!(upgradeStack.getItem() instanceof MagnetUpgradeItem)) return;
        if (
            !upgradeStack.getOrDefault(
                CreateBackpackage.UPGRADE_ENABLED.get(),
                true
            )
        ) return;

        ItemEntity itemEntity = event.getItemEntity();

        // Don't intercept items the player recently threw
        if (isThrownByPlayer(itemEntity, player)) return;

        ItemStack remaining = itemEntity.getItem().copy();

        for (int slot = 0; slot < BackpackMenu.BACKPACK_SIZE; slot++) {
            remaining = inventory.insertItem(slot, remaining, false);
            if (remaining.isEmpty()) break;
        }

        if (remaining.getCount() < itemEntity.getItem().getCount()) {
            // Some or all items went into the backpack
            CardboardBackpackItem.saveInventoryToStack(
                backpackStack,
                inventory
            );

            if (remaining.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(remaining);
            }

            // Cancel vanilla pickup entirely
            event.setCanPickup(TriState.FALSE);
        }
        // If nothing fit in the backpack, let vanilla handle it normally
    }

    private void releaseTrackedItems(Player player) {
        Set<ItemEntity> tracked = trackedItems.remove(player);
        if (tracked != null) {
            for (ItemEntity item : tracked) {
                if (item.isAlive()) {
                    item.setNoGravity(false);
                }
            }
        }
        cachedNearbyItems.remove(player);
    }

    private static int getPickupDelay(ItemEntity item) {
        return item.hasPickUpDelay() ? MAX_PICKUP_DELAY : 0;
    }

    private static boolean isThrownByPlayer(ItemEntity item, Player player) {
        return item.getOwner() == player && item.getAge() < THROW_GRACE_TICKS;
    }

    static ItemStack findEquippedBackpack(Player player) {
        var curiosOpt = CuriosApi.getCuriosInventory(player);
        if (curiosOpt.isPresent()) {
            var handler = curiosOpt.get();
            for (String slotType : new String[] { "back", "body" }) {
                var result = handler.findCurio(slotType, 0);
                if (
                    result.isPresent() &&
                    result.get().stack().getItem() instanceof
                        CardboardBackpackItem
                ) {
                    return result.get().stack();
                }
            }
        }

        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestStack.getItem() instanceof CardboardBackpackItem) {
            return chestStack;
        }

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
