package dev.wyfy.createbackpackage;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = CreateBackpackage.MODID, dist = Dist.CLIENT)
public class CreateBackpackageClient {

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
}
