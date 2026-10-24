package com.lengyang.fulu.client;

import com.lengyang.fulu.entity.LightWingAuraEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 「第四视角」：技能期间把相机挂到**前方的一台相机实体**上，回望玩家。
 *
 * <p>Minecraft 里移动相机位置的唯一干净办法就是 {@code setCameraEntity}，
 * 它本来是给旁观者用的，但普通客户端也能调用（本地预测，不需要服务端同意）。
 *
 * <p>这里只做两件事：
 * <ol>
 *   <li>发现"属于我 + 是相机"的 aura 实体 → 把相机切过去；</li>
 *   <li>它消失（技能结束）→ **一定**切回玩家，避免相机卡死。</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = "fulu", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CameraDirector {

    private CameraDirector() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // 不再接管相机；只做一件事：如果相机被别的东西抢走，切回玩家
        Entity current = mc.getCameraEntity();
        if (current != null && current != mc.player) {
            mc.setCameraEntity(mc.player);
        }
    }
}
