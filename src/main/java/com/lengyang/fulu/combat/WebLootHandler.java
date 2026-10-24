package com.lengyang.fulu.combat;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.entity.WebPrisonEntity;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 空间囚笼的**掉落翻倍**：被囚笼绞杀 / 湮灭致死的怪物，掉落物 ×2。
 *
 * <p>实现：{@code WebPrisonEntity} 在击杀目标时给尸体打上
 * {@link WebPrisonEntity#DROP_TAG}，这里在掉落生成后把每一堆复制一份。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WebLootHandler {

    private WebLootHandler() {
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!event.getEntity().getPersistentData().getBoolean(WebPrisonEntity.DROP_TAG)) {
            return;
        }
        var level = event.getEntity().level();
        var copy = new java.util.ArrayList<ItemEntity>();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            ItemEntity extra = new ItemEntity(level,
                    drop.getX(), drop.getY(), drop.getZ(), stack.copy());
            extra.setDeltaMovement(drop.getDeltaMovement());
            copy.add(extra);
        }
        event.getDrops().addAll(copy);
        FuLu.LOGGER.info("[web] double drops: +{} stacks", copy.size());
    }
}
