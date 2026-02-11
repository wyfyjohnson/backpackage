package dev.wyfy.createbackpackage;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class BackpackBlockEntity extends BlockEntity {

    private final ItemStackHandler inventory = new ItemStackHandler(
        BackpackMenu.TOTAL_SIZE
    ) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public BackpackBlockEntity(BlockPos pos, BlockState state) {
        super(CreateBackpackage.BACKPACK_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    protected void saveAdditional(
        CompoundTag tag,
        HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(
        CompoundTag tag,
        HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }

    public void saveToItem(ItemStack stack) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < inventory.getSlots(); i++) {
            items.add(inventory.getStackInSlot(i));
        }
        stack.set(
            DataComponents.CONTAINER,
            ItemContainerContents.fromItems(items)
        );
    }

    public void loadFromItem(ItemStack stack) {
        ItemContainerContents contents = stack.getOrDefault(
            DataComponents.CONTAINER,
            ItemContainerContents.EMPTY
        );
        for (
            int i = 0;
            i < contents.getSlots() && i < inventory.getSlots();
            i++
        ) {
            inventory.setStackInSlot(i, contents.getStackInSlot(i));
        }
    }
}
