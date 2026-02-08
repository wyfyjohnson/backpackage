package dev.wyfy.createbackpackage;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class BackpackMenu extends AbstractContainerMenu {
    private final ItemStackHandler backpackInventory;
    private final Player player;
    private final InteractionHand hand;

    public static final int BACKPACK_SIZE = 24;
    public static final int UPGRADE_SLOTS = 1;

    public BackpackMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new ItemStackHandler(BACKPACK_SIZE), InteractionHand.MAIN_HAND);
    }

    public BackpackMenu(int containerId, Inventory playerInventory, ItemStackHandler backpackInventory, InteractionHand hand) {
        super(CreateBackpackage.BACKPACK_MENU.get(), containerId);
        this.backpackInventory = backpackInventory;
        this.player = playerInventory.player;
        this.hand = hand;

        // Add backpack slots (3 rows of 8) with validation
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 8; col++) {
                this.addSlot(new SlotItemHandler(backpackInventory, col + row * 8,
                    8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        // Prevent backpacks from being placed in backpack inventory
                        return !(stack.getItem() instanceof CardboardBackpackItem);
                    }
                });
            }
        }

        // Add player inventory (adjusted Y position for 3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                    8 + col * 18, 86 + row * 18));
            }
        }

        // Add player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 144));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index < BACKPACK_SIZE) {
                // Moving from backpack to player inventory
                if (!this.moveItemStackTo(stackInSlot, BACKPACK_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to backpack
                if (!this.moveItemStackTo(stackInSlot, 0, BACKPACK_SIZE, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        // Save inventory back to the backpack ItemStack
        if (!player.level().isClientSide) {
            ItemStack backpackStack = player.getItemInHand(hand);
            if (backpackStack.getItem() instanceof CardboardBackpackItem) {
                CardboardBackpackItem.saveInventoryToStack(backpackStack, this.backpackInventory);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public ItemStackHandler getBackpackInventory() {
        return backpackInventory;
    }
}
