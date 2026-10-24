package com.lengyang.fulu.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 冷却同步包（服务端 → 客户端）：告诉客户端"第 slot 格还有 ticks 刻冷却"。
 *
 * <p>为什么需要它：冷却真正的判定在服务端（读条完成 / 被打断），
 * 客户端只负责显示。不把冷却推过去的话，右下角 HUD 就永远画不出扇形倒计时。
 */
public record CooldownSyncPacket(int slot, int sub, int ticks) {

    public static void encode(CooldownSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.slot());
        buf.writeByte(msg.sub());
        buf.writeVarInt(msg.ticks());
    }

    public static CooldownSyncPacket decode(FriendlyByteBuf buf) {
        return new CooldownSyncPacket(buf.readByte(), buf.readByte(), buf.readVarInt());
    }

    public static void handle(CooldownSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> com.lengyang.fulu.client.ClientEssence
                .startCooldownTicks(msg.slot(), msg.sub(), msg.ticks()));
        c.setPacketHandled(true);
    }
}
