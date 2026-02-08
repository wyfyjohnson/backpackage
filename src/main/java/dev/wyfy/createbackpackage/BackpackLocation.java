package dev.wyfy.createbackpackage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

public sealed interface BackpackLocation {

    ItemStack getStack(Player player);

    void saveStack(Player player, ItemStack stack);

    void writeToBuf(FriendlyByteBuf buf);

    static BackpackLocation readFromBuf(FriendlyByteBuf buf) {
        byte type = buf.readByte();
        return switch (type) {
            case 0 -> new InHand(buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
            case 1 -> new InCurios(buf.readUtf(), buf.readInt());
            default -> throw new IllegalArgumentException("Unknown BackpackLocation type: " + type);
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
            buf.writeByte(0);
            buf.writeBoolean(hand == InteractionHand.MAIN_HAND);
        }
    }

    record InCurios(String slotType, int slotIndex) implements BackpackLocation {
        @Override
        public ItemStack getStack(Player player) {
            return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findCurio(slotType, slotIndex))
                .map(SlotResult::stack)
                .orElse(ItemStack.EMPTY);
        }

        @Override
        public void saveStack(Player player, ItemStack stack) {
            CuriosApi.getCuriosInventory(player)
                .ifPresent(handler -> handler.setEquippedCurio(slotType, slotIndex, stack));
        }

        @Override
        public void writeToBuf(FriendlyByteBuf buf) {
            buf.writeByte(1);
            buf.writeUtf(slotType);
            buf.writeInt(slotIndex);
        }
    }
}
