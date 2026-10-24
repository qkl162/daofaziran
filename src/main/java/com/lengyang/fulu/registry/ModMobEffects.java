package com.lengyang.fulu.registry;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.effect.AntiGravityEffect;
import com.lengyang.fulu.effect.InfightingEffect;
import com.lengyang.fulu.effect.ResistanceUpEffect;
import com.lengyang.fulu.effect.ShiftBlameMarkEffect;
import com.lengyang.fulu.effect.BlindMarkEffect;
import com.lengyang.fulu.effect.FearEffect;
import com.lengyang.fulu.effect.SlipperyMarkEffect;
import com.lengyang.fulu.effect.SummonMarkEffect;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 状态效果登记表。
 *
 * <p>和物品、方块一样，效果也必须先注册再能用在游戏里，否则
 * {@code /effect give @e fulu:infighting} 会直接报"未知的效果"。
 *
 * <p>三件事要对上（少一件就是紫黑格或者没有图标）：
 * <ol>
 *   <li>这里注册的名字 {@code infighting}；</li>
 *   <li>图标 {@code assets/fulu/textures/mob_effect/infighting.png}（18×18）；</li>
 *   <li>语言文件 key {@code effect.fulu.infighting}。</li>
 * </ol>
 *
 * <p>注意图标路径是 {@code mob_effect}（单数、下划线）—— 其他资源目录都是复数
 * （{@code textures/item}、{@code textures/block}），只有这个是单数，
 * 名字写错了表现是"效果有效果、但图标是紫黑格子"。
 */
public final class ModMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FuLu.MOD_ID);

    // ---- 炼妖 Buff（六个）
    /** 腐化之力：攻击附带 3 秒饥饿 */
    public static final RegistryObject<MobEffect> ZOMBIE_POWER =
            MOB_EFFECTS.register("zombie_power", () -> new com.lengyang.fulu.effect.EssenceBuffEffect(0x6BB83F));
    /** 精准：射出的箭会自动追踪敌人 */
    public static final RegistryObject<MobEffect> PRECISION =
            MOB_EFFECTS.register("precision", () -> new com.lengyang.fulu.effect.EssenceBuffEffect(0xE8ECF2));
    /** 爆炸狂怒：按 G 引发一次不破坏地形的爆炸 */
    public static final RegistryObject<MobEffect> BLAST_FURY =
            MOB_EFFECTS.register("blast_fury", () -> new com.lengyang.fulu.effect.EssenceBuffEffect(0x6BD94F));
    /** 蛛行：移动速度 +20%，可以贴墙攀爬 */
    public static final RegistryObject<MobEffect> SPIDER_WALK =
            MOB_EFFECTS.register("spider_walk", () -> new com.lengyang.fulu.effect.EssenceBuffEffect(0x8C5AC0));
    /** 空间跳跃：按 H 与准星指向的目标互换位置（30 格内） */
    public static final RegistryObject<MobEffect> SPACE_JUMP =
            MOB_EFFECTS.register("space_jump", () -> new com.lengyang.fulu.effect.EssenceBuffEffect(0x9E38CC));
    /** 烈火：免疫火焰伤害，攻击点燃目标 */
    /** 阴火：每次攻击都会反伤自己（烈焰妖丹）。 */
    public static final RegistryObject<MobEffect> NETHERFLAME =
            MOB_EFFECTS.register("netherflame",
                    () -> new com.lengyang.fulu.effect.NetherflameEffect());

    /** 被困：头顶显示图标（盘丝天牢）。 */
    public static final RegistryObject<MobEffect> WEBBED =
            MOB_EFFECTS.register("webbed",
                    () -> new com.lengyang.fulu.effect.WebbedEffect());

    /** 诡计毒：每秒攻击 4 次（女巫妖丹的毒瓶）。 */
    public static final RegistryObject<MobEffect> WITCH_POISON =
            MOB_EFFECTS.register("witch_poison",
                    () -> new com.lengyang.fulu.effect.WitchPoisonEffect());

    public static final RegistryObject<MobEffect> FLAME_BODY =
            MOB_EFFECTS.register("flame_body", () -> new com.lengyang.fulu.effect.EssenceBuffEffect(0xFF9A2E));


    /** 内讧：纯标记，不掉血。给站在阵法里的怪物挂上。 */
    public static final RegistryObject<MobEffect> INFIGHTING =
            MOB_EFFECTS.register("infighting", InfightingEffect::new);

    /**
     * 反重力：纯标记，阵法内无限时长。升空/按住移动在每刻事件、倒立在客户端渲染事件。
     *
     * <p>和 infighting 一样是"效果当标记、事件干活"的分工，理由见
     * {@link AntiGravityEffect}。
     */
    public static final RegistryObject<MobEffect> ANTIGRAVITY =
            MOB_EFFECTS.register("antigravity", AntiGravityEffect::new);

    /**
     * 抗性提升：玩家减伤，支持 I ~ VIII 级，VII/VIII 额外 80% 反弹。
     *
     * <p>数值表和反弹比例在 {@link ResistanceUpEffect}；真正改伤害的是
     * {@code ResistanceUpHandler}（监听受伤事件）。
     */
    public static final RegistryObject<MobEffect> RESISTANCE_UP =
            MOB_EFFECTS.register("resistance_up", ResistanceUpEffect::new);

    /** 嫁祸标记：站在嫁祸阵里的生物（怪物和玩家）身上的记号。 */
    public static final RegistryObject<MobEffect> SHIFT_BLAME =
            MOB_EFFECTS.register("shift_blame", ShiftBlameMarkEffect::new);

    /** 招魂标记：站在招魂阵里的怪物身上的记号。 */
    public static final RegistryObject<MobEffect> SUMMON =
            MOB_EFFECTS.register("summon", SummonMarkEffect::new);

    /** 滑地标记：站在滑地阵里的怪物身上的记号。 */
    public static final RegistryObject<MobEffect> SLIPPERY =
            MOB_EFFECTS.register("slippery", SlipperyMarkEffect::new);

    /** 无明标记：站在无明阵里的怪物身上的记号。 */
    public static final RegistryObject<MobEffect> BLIND =
            MOB_EFFECTS.register("blind", BlindMarkEffect::new);

    /** 无明「恐惧」：血量低时惊慌逃跑。 */
    public static final RegistryObject<MobEffect> FEAR =
            MOB_EFFECTS.register("fear", FearEffect::new);

    private ModMobEffects() {
    }

    /** 由模组主文件 FuLu 调用。 */
    public static void register(IEventBus modBus) {
        MOB_EFFECTS.register(modBus);
    }
}
