package com.lengyang.fulu.client;

import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 技能运镜：**低角度 + 缓慢横移 + 蓄力时压窄视场**。
 *
 * <p>Minecraft 的 mod API 没有"移动相机位置"的事件（只有角度和 FOV），
 * 所以真正的**环绕**做不到；这里用最强的可行近似：
 * <ul>
 *   <li>把镜头**压低并上仰**（低角度仰拍感）；</li>
 *   <li>让朝向做一次**正弦横移**（20° 来回），看起来像镜头在扫；</li>
 *   <li>蓄力段 FOV 压窄（拉近、压迫感），斩击瞬间再弹回并略微拉大。</li>
 * </ul>
 *
 * <p>它只改**渲染用的视角**，不动玩家真实朝向，所以你在演出期间依然能正常转身。
 */
@Mod.EventBusSubscriber(modid = "fulu", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CinematicCameraClient {

    /** 演出总刻数。 */
    private static int total;
    /** 剩余刻数。 */
    private static int left;
    /** 横移方向（左右交替，避免每次一样）。 */
    private static float sign = 1.0F;

    private CinematicCameraClient() {
    }

    public static void start(int ticks) {
        total = Math.max(1, ticks);
        left = total;
        sign = -sign;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && left > 0) {
            left--;
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (left <= 0) {
            return;
        }
        // 视角已经完全交给"后拉第三人称"相机，这里不再动角度，
        // 否则会和相机实体的朝向打架（之前"瞄着旁边"就是这么来的）
    }

}
