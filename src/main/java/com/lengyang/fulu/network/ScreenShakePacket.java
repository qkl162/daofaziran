package com.lengyang.fulu.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 震屏包（服务端 → 客户端）：斩击命中瞬间的镜头抖动 + FOV 拉远。
 *
 * <p>Minecraft 里没法像 CG 那样真的"运镜"，但**抖动 + 视场角突变**
 * 已经能给出"这一击很重"的体感。
 */
public record ScreenShakePacket(int ticks, float strength) {

    public static void encode(ScreenShakePacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.ticks());
        buf.writeFloat(msg.strength());
    }

    public static ScreenShakePacket decode(FriendlyByteBuf buf) {
        return new ScreenShakePacket(buf.readByte(), buf.readFloat());
    }

    public static void handle(ScreenShakePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> com.lengyang.fulu.client.ScreenShakeClient.trigger(
                msg.ticks(), msg.strength()));
        c.setPacketHandled(true);
    }
}
