package com.lengyang.fulu.item;

import java.util.function.Supplier;

import com.lengyang.fulu.registry.ModMobEffects;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 妖丹：吃掉之后获得对应怪物的能力（15 秒）。
 *
 * <p>做成"食物"而不是右键即用，是为了复用原版吃东西的那一套
 * （动作、音效、第三人称动画），而且能吃到 20 秒的食用时间给玩家反应。
 * {@code alwaysEat()} 保证饱食度满的时候也能吃。
 */
public class EssenceItem extends Item {

    /** 效果持续 15 秒。 */
    public static final int DURATION_TICKS = 300;

    private final Supplier<MobEffect> effect;
    private final String descKey;

    public EssenceItem(Supplier<MobEffect> effect, String descKey) {
        // **不再是食物**：妖丹只作为妖丹匣里的"燃料/技能"，吃了没有任何意义
        super(new Item.Properties().stacksTo(16));
        this.effect = effect;
        this.descKey = descKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable(this.descKey).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.fulu.essence.duration")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
