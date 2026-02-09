package dev.wyfy.createbackpackage;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

@Mod(CreateBackpackage.MODID)
public class CreateBackpackage {

    public static final String MODID = "create_backpackage";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Menu registration
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredHolder<
        MenuType<?>,
        MenuType<BackpackMenu>
    > BACKPACK_MENU = MENUS.register("backpack", () ->
        IMenuTypeExtension.create((id, inventory, buf) ->
            new BackpackMenu(id, inventory, BackpackLocation.readFromBuf(buf))
        )
    );

    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<BackpackBlock> CARDBOARD_BACKPACK_BLOCK =
        BLOCKS.register("cardboard_backpack", () ->
            new BackpackBlock(
                BlockBehaviour.Properties.of()
                    .strength(0.5f)
                    .sound(SoundType.WOOL)
                    .noOcclusion()
            )
        );

    public static final DeferredItem<Item> CARDBOARD_BACKPACK = ITEMS.register(
        "cardboard_backpack",
        () ->
            new CardboardBackpackItem(
                CARDBOARD_BACKPACK_BLOCK.get(),
                new Item.Properties().stacksTo(1)
            )
    );

    public static final DeferredHolder<
        BlockEntityType<?>,
        BlockEntityType<BackpackBlockEntity>
    > BACKPACK_BLOCK_ENTITY = BLOCK_ENTITIES.register("backpack", () ->
        BlockEntityType.Builder.of(
            BackpackBlockEntity::new,
            CARDBOARD_BACKPACK_BLOCK.get()
        ).build(null)
    );

    public static final DeferredHolder<
        CreativeModeTab,
        CreativeModeTab
    > BACKPACKAGE_TAB = CREATIVE_MODE_TABS.register("backpackage_tab", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.create_backpackage"))
            .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .icon(() -> CARDBOARD_BACKPACK.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(CARDBOARD_BACKPACK.get());
            })
            .build()
    );

    public CreateBackpackage(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MENUS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Create: Backpackage loaded!");
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID);
        registrar.playToServer(
            OpenBackpackFromCuriosPayload.TYPE,
            OpenBackpackFromCuriosPayload.STREAM_CODEC,
            this::handleOpenBackpackFromCurios
        );
    }

    private void handleOpenBackpackFromCurios(
        OpenBackpackFromCuriosPayload payload,
        net.neoforged.neoforge.network.handling.IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            // Try Curios slots first
            var curiosResult = CuriosApi.getCuriosInventory(
                serverPlayer
            ).flatMap(handler ->
                handler.findFirstCurio(CARDBOARD_BACKPACK.get())
            );

            if (curiosResult.isPresent()) {
                var result = curiosResult.get();
                var slotContext = result.slotContext();
                BackpackLocation location = new BackpackLocation.InCurios(
                    slotContext.identifier(),
                    slotContext.index()
                );
                openBackpackMenu(serverPlayer, result.stack(), location);
                return;
            }

            // Fallback: check vanilla chest slot
            var chestStack = serverPlayer.getItemBySlot(EquipmentSlot.CHEST);
            if (chestStack.getItem() instanceof CardboardBackpackItem) {
                BackpackLocation location = new BackpackLocation.InChestSlot();
                openBackpackMenu(serverPlayer, chestStack, location);
            }
        });
    }

    private void openBackpackMenu(
        ServerPlayer player,
        net.minecraft.world.item.ItemStack backpackStack,
        BackpackLocation location
    ) {
        ItemStackHandler inventory =
            CardboardBackpackItem.getInventoryFromStack(backpackStack);

        player.openMenu(
            new SimpleMenuProvider(
                (containerId, playerInventory, p) ->
                    new BackpackMenu(
                        containerId,
                        playerInventory,
                        inventory,
                        location
                    ),
                Component.translatable("container.create_backpackage.backpack")
            ),
            buf -> location.writeToBuf(buf)
        );
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Create: Backpackage starting on server");
    }
}
