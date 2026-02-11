package dev.wyfy.createbackpackage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

public sealed interface BackpackLocation {
    byte TYPE_IN_HAND = 0;
    byte TYPE_IN_CURIOS = 1;
    byte TYPE_IN_BLOCK = 2;
    byte TYPE_IN_CHEST_SLOT = 3;

    ItemStack getStack(Player player);

    void saveStack(Player player, ItemStack stack);

    void writeToBuf(FriendlyByteBuf buf);

    static BackpackLocation readFromBuf(FriendlyByteBuf buf) {
        byte type = buf.readByte();
        return switch (type) {
            case TYPE_IN_HAND -> new InHand(
                buf.readBoolean()
                    ? InteractionHand.MAIN_HAND
                    : InteractionHand.OFF_HAND
            );
            case TYPE_IN_CURIOS -> new InCurios(buf.readUtf(), buf.readInt());
            case TYPE_IN_BLOCK -> new InBlock(buf.readBlockPos());
            case TYPE_IN_CHEST_SLOT -> new InChestSlot();
            default -> throw new IllegalArgumentException(
                "Unknown BackpackLocation type: " + type
            );
        };
    }

    record InHand(InteractionHand hand) implements BackpackLocation {
        @Override
        public ItemStack getStack(Player player) {
            return player.getItemInHand(hand);
        }

        @Override
        public void saveStack(Player player, ItemStack stack) {
            // The stack reference is the same object in the player's hand,
            // so modifying it in-place (via saveInventoryToStack) is sufficient.
            // This explicit set ensures safety if the reference was ever copied.
            player.setItemInHand(hand, stack);
        }

        @Override
        public void writeToBuf(FriendlyByteBuf buf) {
            buf.writeByte(TYPE_IN_HAND);
            buf.writeBoolean(hand == InteractionHand.MAIN_HAND);
        }
    }

    record InCurios(
        String slotType,
        int slotIndex
    ) implements BackpackLocation {
        @Override
        public ItemStack getStack(Player player) {
            return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findCurio(slotType, slotIndex))
                .map(SlotResult::stack)
                .orElse(ItemStack.EMPTY);
        }

        @Override
        public void saveStack(Player player, ItemStack stack) {
            CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.setEquippedCurio(slotType, slotIndex, stack)
            );
        }

        @Override
        public void writeToBuf(FriendlyByteBuf buf) {
            buf.writeByte(TYPE_IN_CURIOS);
            buf.writeUtf(slotType);
            buf.writeInt(slotIndex);
        }
    }

    record InBlock(BlockPos pos) implements BackpackLocation {
        @Override
        public ItemStack getStack(Player player) {
            return ItemStack.EMPTY;
        }

        @Override
        public void saveStack(Player player, ItemStack stack) {
            // Saving is handled directly via the block entity in BackpackMenu.removed()
        }

        @Override
        public void writeToBuf(FriendlyByteBuf buf) {
            buf.writeByte(TYPE_IN_BLOCK);
            buf.writeBlockPos(pos);
        }
    }

    record InChestSlot() implements BackpackLocation {
        @Override
        public ItemStack getStack(Player player) {
            return player.getItemBySlot(EquipmentSlot.CHEST);
        }

        @Override
        public void saveStack(Player player, ItemStack stack) {
            player.setItemSlot(EquipmentSlot.CHEST, stack);
        }

        @Override
        public void writeToBuf(FriendlyByteBuf buf) {
            buf.writeByte(TYPE_IN_CHEST_SLOT);
        }
    }
}
