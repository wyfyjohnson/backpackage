package dev.wyfy.createbackpackage;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@Mod(value = CreateBackpackage.MODID, dist = Dist.CLIENT)
public class CreateBackpackageClient {

    public static final KeyMapping OPEN_BACKPACK_KEY = new KeyMapping(
        "key.create_backpackage.open_backpack",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_B,
        "key.categories.create_backpackage"
    );

    public CreateBackpackageClient(
        IEventBus modEventBus,
        ModContainer container
    ) {
        container.registerExtensionPoint(
            IConfigScreenFactory.class,
            ConfigurationScreen::new
        );

        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(this::registerKeyMappings);

        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        CreateBackpackage.LOGGER.info("HELLO FROM CLIENT SETUP");
        CreateBackpackage.LOGGER.info(
            "MINECRAFT NAME >> {}",
            Minecraft.getInstance().getUser().getName()
        );
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(
            CreateBackpackage.BACKPACK_MENU.get(),
            BackpackScreen::new
        );
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BACKPACK_KEY);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        if (
            Minecraft.getInstance().screen == null &&
            OPEN_BACKPACK_KEY.consumeClick()
        ) {
            PacketDistributor.sendToServer(new OpenBackpackFromCuriosPayload());
        }
    }
}
