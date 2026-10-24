package com.lengyang.fulu.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 震屏：命中帧的镜头抖动 + FOV 拉远。
 *
 * <p>{@code ViewportEvent.ComputeCameraAngles} 是唯一能在**不改动玩家实体**的
 * 前提下调整视角的事件，所以"镜头抖动"在这里实现；
 * "镜头拉远"（FOV 变大）走 {@code ComputeFov}。
 */
@Mod.EventBusSubscriber(modid = "fulu", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ScreenShakeClient {

    private static int ticksLeft;
    private static int total;
    private static float strength;

    private ScreenShakeClient() {
    }

    /** 由网络包调用。 */
    public static void trigger(int ticks, float str) {
        total = Math.max(1, ticks);
        ticksLeft = total;
        strength = str;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ticksLeft > 0) {
            ticksLeft--;
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (ticksLeft <= 0) {
            return;
        }
        float k = ticksLeft / (float) total;
        float amp = strength * k * 1.6F;
        double t = System.nanoTime() / 1.0E9D;
        event.setYaw(event.getYaw() + (float) (Math.sin(t * 61.0D) * amp));
        event.setPitch(event.getPitch() + (float) (Math.cos(t * 47.0D) * amp * 0.7D));
        event.setRoll(event.getRoll() + (float) (Math.sin(t * 33.0D) * amp * 0.5D));
    }

}
