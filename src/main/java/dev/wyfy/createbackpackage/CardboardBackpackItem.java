package dev.wyfy.createbackpackage;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CardboardBackpackItem extends Item {

    public CardboardBackpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level level,
        Player player,
        InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (
            !level.isClientSide && player instanceof ServerPlayer serverPlayer
        ) {
            // Load inventory from NBT
            ItemStackHandler inventory = getInventoryFromStack(stack);

            // Open the backpack GUI
            serverPlayer.openMenu(
                new SimpleMenuProvider(
                    (containerId, playerInventory, p) ->
                        new BackpackMenu(
                            containerId,
                            playerInventory,
                            inventory,
                            hand
                        ),
                    Component.translatable(
                        "container.create_backpackage.backpack"
                    )
                ),
                buf -> {
                    buf.writeBoolean(hand == InteractionHand.MAIN_HAND);
                }
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // Load inventory from ItemStack NBT
    public static ItemStackHandler getInventoryFromStack(ItemStack stack) {
        ItemStackHandler handler = new ItemStackHandler(
            BackpackMenu.BACKPACK_SIZE
        );

        ItemContainerContents contents = stack.getOrDefault(
            DataComponents.CONTAINER,
            ItemContainerContents.EMPTY
        );
        for (
            int i = 0;
            i < contents.getSlots() && i < BackpackMenu.BACKPACK_SIZE;
            i++
        ) {
            handler.setStackInSlot(i, contents.getStackInSlot(i));
        }

        return handler;
    }

    // Save inventory to ItemStack NBT
    public static void saveInventoryToStack(
        ItemStack stack,
        ItemStackHandler inventory
    ) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < inventory.getSlots(); i++) {
            items.add(inventory.getStackInSlot(i));
        }
        stack.set(
            DataComponents.CONTAINER,
            ItemContainerContents.fromItems(items)
        );
    }
}
