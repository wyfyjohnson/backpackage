package dev.wyfy.createbackpackage;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class BackpackMenu extends AbstractContainerMenu {

    public static final int BACKPACK_ROWS = 3;
    public static final int BACKPACK_COLS = 8;
    public static final int BACKPACK_SIZE = BACKPACK_ROWS * BACKPACK_COLS;
    public static final int UPGRADE_SLOTS = 1;
    public static final int TOTAL_SIZE = BACKPACK_SIZE + UPGRADE_SLOTS;

    private static final int UPGRADE_SLOT_X = 62;
    private static final int UPGRADE_SLOT_Y = 0;

    private static final int PLAYER_INV_ROWS = 3;
    private static final int PLAYER_INV_COLS = 9;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_START_X = 8;
    private static final int BACKPACK_SLOT_START_Y = 18;
    private static final int PLAYER_INV_START_Y = 86;
    private static final int HOTBAR_START_Y = 144;
    private static final int PLAYER_INV_FIRST_SLOT = 9;
    private static final double MAX_INTERACTION_DIST_SQ = 64.0;

    private final ItemStackHandler backpackInventory;
    private final Player player;
    private final BackpackLocation location;

    public BackpackMenu(
        int containerId,
        Inventory playerInventory,
        BackpackLocation location
    ) {
        this(
            containerId,
            playerInventory,
            new ItemStackHandler(TOTAL_SIZE),
            location
        );
    }

    public BackpackMenu(
        int containerId,
        Inventory playerInventory,
        ItemStackHandler backpackInventory,
        BackpackLocation location
    ) {
        super(CreateBackpackage.BACKPACK_MENU.get(), containerId);
        this.backpackInventory = backpackInventory;
        this.player = playerInventory.player;
        this.location = location;

        addBackpackSlots();
        addUpgradeSlots();
        addPlayerInventorySlots(playerInventory);
        addHotbarSlots(playerInventory);
    }

    private void addBackpackSlots() {
        for (int row = 0; row < BACKPACK_ROWS; row++) {
            for (int col = 0; col < BACKPACK_COLS; col++) {
                this.addSlot(
                    new SlotItemHandler(
                        backpackInventory,
                        col + row * BACKPACK_COLS,
                        SLOT_START_X + col * SLOT_SIZE,
                        BACKPACK_SLOT_START_Y + row * SLOT_SIZE
                    ) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            if (
                                stack.getItem() instanceof CardboardBackpackItem
                            ) {
                                return !hasContents(stack);
                            }
                            return true;
                        }
                    }
                );
            }
        }
    }

    private void addUpgradeSlots() {
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            this.addSlot(
                new SlotItemHandler(
                    backpackInventory,
                    BACKPACK_SIZE + i,
                    UPGRADE_SLOT_X + i * SLOT_SIZE,
                    UPGRADE_SLOT_Y
                ) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof BackpackUpgradeItem;
                    }
                }
            );
        }
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        for (int row = 0; row < PLAYER_INV_ROWS; row++) {
            for (int col = 0; col < PLAYER_INV_COLS; col++) {
                this.addSlot(
                    new Slot(
                        playerInventory,
                        col + row * PLAYER_INV_COLS + PLAYER_INV_FIRST_SLOT,
                        SLOT_START_X + col * SLOT_SIZE,
                        PLAYER_INV_START_Y + row * SLOT_SIZE
                    )
                );
            }
        }
    }

    private void addHotbarSlots(Inventory playerInventory) {
        for (int col = 0; col < PLAYER_INV_COLS; col++) {
            this.addSlot(
                new Slot(
                    playerInventory,
                    col,
                    SLOT_START_X + col * SLOT_SIZE,
                    HOTBAR_START_Y
                )
            );
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        int playerInvStart = BACKPACK_SIZE + UPGRADE_SLOTS;

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index < playerInvStart) {
                // From backpack (storage or upgrade) -> player inventory
                if (
                    !this.moveItemStackTo(
                        stackInSlot,
                        playerInvStart,
                        this.slots.size(),
                        true
                    )
                ) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player inventory -> backpack
                if (stackInSlot.getItem() instanceof BackpackUpgradeItem) {
                    // Upgrades: try upgrade slots first, then storage
                    if (
                        !this.moveItemStackTo(
                            stackInSlot,
                            BACKPACK_SIZE,
                            BACKPACK_SIZE + UPGRADE_SLOTS,
                            false
                        ) &&
                        !this.moveItemStackTo(
                            stackInSlot,
                            0,
                            BACKPACK_SIZE,
                            false
                        )
                    ) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Regular items: storage slots only
                    if (
                        !this.moveItemStackTo(
                            stackInSlot,
                            0,
                            BACKPACK_SIZE,
                            false
                        )
                    ) {
                        return ItemStack.EMPTY;
                    }
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

        if (!player.level().isClientSide) {
            if (location instanceof BackpackLocation.InBlock inBlock) {
                if (
                    player.level().getBlockEntity(inBlock.pos()) instanceof
                        BackpackBlockEntity be
                ) {
                    be.setChanged();
                }
            } else {
                ItemStack backpackStack = location.getStack(player);
                if (backpackStack.getItem() instanceof CardboardBackpackItem) {
                    CardboardBackpackItem.saveInventoryToStack(
                        backpackStack,
                        this.backpackInventory
                    );
                    location.saveStack(player, backpackStack);
                }
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (location instanceof BackpackLocation.InBlock inBlock) {
            return (
                player.level().getBlockEntity(inBlock.pos()) instanceof
                    BackpackBlockEntity &&
                player.distanceToSqr(
                    inBlock.pos().getX() + 0.5,
                    inBlock.pos().getY() + 0.5,
                    inBlock.pos().getZ() + 0.5
                ) <=
                MAX_INTERACTION_DIST_SQ
            );
        }
        return true;
    }

    public ItemStackHandler getBackpackInventory() {
        return backpackInventory;
    }

    private static boolean hasContents(ItemStack stack) {
        ItemContainerContents contents = stack.getOrDefault(
            DataComponents.CONTAINER,
            ItemContainerContents.EMPTY
        );
        for (int i = 0; i < contents.getSlots(); i++) {
            if (!contents.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
