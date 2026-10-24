package com.lengyang.fulu.network;

import java.util.function.Supplier;

import com.lengyang.fulu.refining.RefiningPlayerData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 选槽包：客户端轮盘确认后告诉服务端"我现在用第 N 格"。
 *
 * <p>只传一个下标 —— 具体那一格装的是什么由服务端自己从妖丹匣里读，
 * 客户端不能伪造"我这格是烈焰妖丹"这种信息。
 */
public record SelectSlotPacket(int slot) {

    public static void encode(SelectSlotPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.slot());
    }

    public static SelectSlotPacket decode(FriendlyByteBuf buf) {
        return new SelectSlotPacket(buf.readByte());
    }

    public static void handle(SelectSlotPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer player = c.getSender();
        c.enqueueWork(() -> {
            if (player == null) {
                return;
            }
            // **必须带着妖丹匣**才允许改当前槽位（用户要求）
            if (com.lengyang.fulu.refining.RefiningCast.findBox(player).isEmpty()) {
                return;
            }
            RefiningPlayerData.setActiveSlot(player, msg.slot());
        });
        c.setPacketHandled(true);
    }
}
