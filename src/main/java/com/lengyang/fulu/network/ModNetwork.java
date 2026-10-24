package com.lengyang.fulu.network;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.standin.ClientStandInData;
import com.lengyang.fulu.standin.StandInData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * 网络通道：目前只有一个包 —— 把玩家身上的替身阵数据推给**他自己**的客户端。
 *
 * <p>为什么要网络：替身标记 / 次数盾点数存在服务端的玩家持久数据里，
 * 而"画血条"和"播全屏碎裂图案"都在客户端。所以数据一变就推一份。
 *
 * <p><b>用的是 47.x 的 API</b>：{@code NetworkRegistry.newSimpleChannel} +
 * {@code SimpleChannel.registerMessage}。不是 1.21 的 {@code ChannelBuilder}
 * —— 那个在这个版本里根本不存在（查过 forge 的 jar 才确认）。
 *
 * <p>协议版本是字符串常量，两端不一致时 Forge 会直接拒绝连接，
 * 而不是让字段错位（错位的表现是"数据莫名其妙"、很难查）。
 */
public final class ModNetwork {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FuLu.MOD_ID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private ModNetwork() {
    }

    /** 由 FuLu 主类调用一次。 */
    public static void register() {
        CHANNEL.registerMessage(0, StandInSyncPacket.class,
                StandInSyncPacket::encode,
                StandInSyncPacket::decode,
                StandInSyncPacket::handle);
        // 轮盘选槽
        CHANNEL.registerMessage(3, SelectSlotPacket.class,
                SelectSlotPacket::encode,
                SelectSlotPacket::decode,
                SelectSlotPacket::handle);
        // 施法
        CHANNEL.registerMessage(4, CastEssencePacket.class,
                CastEssencePacket::encode,
                CastEssencePacket::decode,
                CastEssencePacket::handle);
        // 震屏（斩击瞬间）
        CHANNEL.registerMessage(6, ScreenShakePacket.class,
                ScreenShakePacket::encode,
                ScreenShakePacket::decode,
                ScreenShakePacket::handle);
        // 运镜（技能演出）
        CHANNEL.registerMessage(7, CinematicPacket.class,
                CinematicPacket::encode,
                CinematicPacket::decode,
                CinematicPacket::handle);
        // 冷却同步（服务端 -> 客户端，HUD 画扇形用）
        CHANNEL.registerMessage(5, CooldownSyncPacket.class,
                CooldownSyncPacket::encode,
                CooldownSyncPacket::decode,
                CooldownSyncPacket::handle);
    }

    /** 把某个玩家的替身阵数据推给他自己。 */
    public static void sync(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StandInSyncPacket(
                StandInData.hasStandIn(player),
                StandInData.hasResistance(player),
                StandInData.hitShield(player),
                StandInData.shatterTicks(player)));
    }

    /**
     * 同步包：四个字段。
     *
     * <p>{@code shatterTicks > 0} 表示"这次要播碎裂动画" —— 客户端收到就把本地计时器
     * 重置一次，动画播完自己收尾。<b>不逐刻同步进度</b>：那样每刻一个包，纯浪费。
     */
    public record StandInSyncPacket(boolean hasStandIn, boolean hasResistance,
                                    int hitShield, int shatterTicks) {

        public void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(hasStandIn);
            buf.writeBoolean(hasResistance);
            buf.writeVarInt(hitShield);
            buf.writeVarInt(shatterTicks);
        }

        public static StandInSyncPacket decode(FriendlyByteBuf buf) {
            return new StandInSyncPacket(buf.readBoolean(), buf.readBoolean(),
                    buf.readVarInt(), buf.readVarInt());
        }

        public static void handle(StandInSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context context = ctxSupplier.get();
            boolean shatter = msg.shatterTicks > 0;
            // 纯客户端的类只能在这里间接引用，否则服务端加载这个类就会炸
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientStandInData.apply(msg.hasStandIn, msg.hasResistance,
                            msg.hitShield, shatter)));
            context.setPacketHandled(true);
        }
    }
}
