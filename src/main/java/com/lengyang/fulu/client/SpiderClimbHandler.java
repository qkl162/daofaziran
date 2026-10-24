package com.lengyang.fulu.client;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 蜘蛛爬墙（客户端）。
 *
 * <p><b>为什么必须客户端做</b>：玩家的移动是**客户端权威**的 —— 服务端每刻都会收到
 * 客户端的坐标并且跟着改。所以服务端去改 {@code deltaMovement} 基本没用
 * （下一 tick 就被客户端的输入覆盖），表现就是"按了技能但爬不上去"。
 * 客户端改自己的速度才会被真正执行。
 */
@Mod.EventBusSubscriber(modid = "fulu", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SpiderClimbHandler {

    /** 爬墙时每刻给的上推速度。 */
    private static final double CLIMB_SPEED = 0.18D;

    private SpiderClimbHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        var mc = net.minecraft.client.Minecraft.getInstance();
        var player = mc.player;
        if (player == null) {
            return;
        }
        // 只在"有蛛行效果"时生效（效果由 Z 键开关，无限时长）
        if (!player.hasEffect(com.lengyang.fulu.registry.ModMobEffects.SPIDER_WALK.get())) {
            return;
        }
        // 贴着墙、并且正在往前走/往墙里推
        if (!player.horizontalCollision) {
            return;
        }
        boolean pushing = mc.options.keyUp.isDown() || player.zza > 0.01F;
        if (!pushing) {
            return;
        }
        Vec3 v = player.getDeltaMovement();
        if (v.y < CLIMB_SPEED) {
            player.setDeltaMovement(v.x, CLIMB_SPEED, v.z);
            player.fallDistance = 0.0F;
        }
        // 贴墙时衰减重力感（原版会在下一 tick 再加重力，所以每刻推一次即可）
    }
}
