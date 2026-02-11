package dev.wyfy.createbackpackage;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenBackpackFromCuriosPayload() implements CustomPacketPayload {
    public static final Type<OpenBackpackFromCuriosPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(
            CreateBackpackage.MODID,
            "open_backpack_curios"
        )
    );

    public static final StreamCodec<
        ByteBuf,
        OpenBackpackFromCuriosPayload
    > STREAM_CODEC = StreamCodec.unit(new OpenBackpackFromCuriosPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
