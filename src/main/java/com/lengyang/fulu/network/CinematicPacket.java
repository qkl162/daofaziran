package com.lengyang.fulu.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 运镜包（服务端 → 客户端）：技能演出期间给镜头加"低角度环绕 + 蓄力慢推"。
 *
 * <p>服务端只负责**什么时候开始、持续多久**，具体怎么动在客户端算。
 */
public record CinematicPacket(int ticks) {

    public static void encode(CinematicPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.ticks());
    }

    public static CinematicPacket decode(FriendlyByteBuf buf) {
        return new CinematicPacket(buf.readVarInt());
    }

    public static void handle(CinematicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> com.lengyang.fulu.client.CinematicCameraClient.start(msg.ticks()));
        c.setPacketHandled(true);
    }
}
