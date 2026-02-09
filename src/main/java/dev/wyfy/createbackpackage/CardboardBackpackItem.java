package dev.wyfy.createbackpackage;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.ItemStackHandler;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class CardboardBackpackItem
    extends BlockItem
    implements ICurioItem, Equipable
{

    public CardboardBackpackItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // Right-click on a surface: place the backpack
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level level,
        Player player,
        InteractionHand hand
    ) {
        // Right-click into air (no block targeted): open the GUI
        ItemStack stack = player.getItemInHand(hand);

        if (
            !level.isClientSide && player instanceof ServerPlayer serverPlayer
        ) {
            ItemStackHandler inventory = getInventoryFromStack(stack);
            BackpackLocation location = new BackpackLocation.InHand(hand);

            serverPlayer.openMenu(
                new SimpleMenuProvider(
                    (containerId, playerInventory, p) ->
                        new BackpackMenu(
                            containerId,
                            playerInventory,
                            inventory,
                            location
                        ),
                    Component.translatable(
                        "container.create_backpackage.backpack"
                    )
                ),
                buf -> location.writeToBuf(buf)
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // Vanilla Equipable: allow placing in the chestplate slot
    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    // NeoForge extension: stack-sensitive equipment slot
    @Override
    public @org.jetbrains.annotations.Nullable EquipmentSlot getEquipmentSlot(
        ItemStack stack
    ) {
        return EquipmentSlot.CHEST;
    }

    // Curios: don't auto-equip on right-click (right-click opens GUI instead)
    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return false;
    }

    public static ItemStackHandler getInventoryFromStack(ItemStack stack) {
        ItemStackHandler handler = new ItemStackHandler(
            BackpackMenu.TOTAL_SIZE
        );

        ItemContainerContents contents = stack.getOrDefault(
            DataComponents.CONTAINER,
            ItemContainerContents.EMPTY
        );
        for (
            int i = 0;
            i < contents.getSlots() && i < BackpackMenu.TOTAL_SIZE;
            i++
        ) {
            handler.setStackInSlot(i, contents.getStackInSlot(i));
        }

        return handler;
    }

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
