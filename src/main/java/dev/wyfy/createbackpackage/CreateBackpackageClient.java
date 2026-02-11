package dev.wyfy.createbackpackage;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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

    private boolean wasKeyDown = false;

    public CreateBackpackageClient(
        IEventBus modEventBus,
        ModContainer container
    ) {
        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(this::registerKeyMappings);

        NeoForge.EVENT_BUS.addListener(this::onClientTick);
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean isKeyDown = InputConstants.isKeyDown(
            mc.getWindow().getWindow(),
            OPEN_BACKPACK_KEY.getKey().getValue()
        );
        boolean keyJustPressed = isKeyDown && !wasKeyDown;
        wasKeyDown = isKeyDown;

        if (mc.screen == null) {
            // Normal gameplay: use the key mapping system
            if (OPEN_BACKPACK_KEY.consumeClick()) {
                PacketDistributor.sendToServer(
                    new OpenBackpackFromCuriosPayload()
                );
            }
        } else if (keyJustPressed) {
            // Inside a GUI: only trigger on the rising edge of the key press
            if (
                !(mc.screen instanceof
                        net.minecraft.client.gui.screens.ChatScreen) &&
                !(mc.screen instanceof
                        net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen)
            ) {
                mc.player.closeContainer();
                PacketDistributor.sendToServer(
                    new OpenBackpackFromCuriosPayload()
                );
            }
        }
    }
}
