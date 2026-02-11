package dev.wyfy.createbackpackage;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(
            CreateBackpackage.MODID,
            "textures/gui/backpack.png"
        );
    private static final int GUI_HEIGHT = 168;
    private static final int INVENTORY_LABEL_OFFSET = 94;

    public BackpackScreen(
        BackpackMenu menu,
        Inventory playerInventory,
        Component title
    ) {
        super(menu, playerInventory, title);
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = this.imageHeight - INVENTORY_LABEL_OFFSET;
    }

    @Override
    protected void renderBg(
        GuiGraphics guiGraphics,
        float partialTick,
        int mouseX,
        int mouseY
    ) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(
            TEXTURE,
            x,
            y,
            0,
            0,
            this.imageWidth,
            this.imageHeight
        );
    }

    @Override
    public void render(
        GuiGraphics guiGraphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
