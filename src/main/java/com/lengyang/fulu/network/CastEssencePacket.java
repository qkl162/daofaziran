package com.lengyang.fulu.network;

import java.util.function.Supplier;

import com.lengyang.fulu.refining.RefiningCast;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 施法包：告诉服务端"用第 N 格施法"。
 *
 * <p>服务端会自己核对：这一格有没有妖丹、是不是在冷却、妖丹匣在不在包里。
 * 客户端只提供"哪一格"，别的都不信。
 */
public record CastEssencePacket(int slot, int sub) {

    public static void encode(CastEssencePacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.slot());
        buf.writeByte(msg.sub());
    }

    public static CastEssencePacket decode(FriendlyByteBuf buf) {
        return new CastEssencePacket(buf.readByte(), buf.readByte());
    }

    public static void handle(CastEssencePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer player = c.getSender();
        c.enqueueWork(() -> {
            if (player != null) {
                // 不再"按了秒出"：先进入 0.5 秒读条
                RefiningCast.beginCast(player, msg.slot(), msg.sub());
            }
        });
        c.setPacketHandled(true);
    }
}
