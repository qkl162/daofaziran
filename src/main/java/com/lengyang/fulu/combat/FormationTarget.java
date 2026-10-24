package com.lengyang.fulu.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;

/**
 * "这只生物算不算阵法要对付的敌人" —— <b>规则只写在这里</b>。
 *
 * <p>为什么要单独抽一个类：阵法扫描（{@code FormationEyeBlockEntity}）和飞剑自己找目标
 * （{@code FlyingSwordEntity}，手动 {@code /summon} 出来的剑用得上）必须用**同一个标准**。
 * 两处各写一遍的话，迟早出现"阵眼认得的怪、飞剑却不打"这种对不上的毛病。
 *
 * <p><b>用 {@link Enemy} 接口，不要用 {@code Monster} 类</b> —— 这是踩过的坑：
 * 僵尸、骷髅、苦力怕、末影人、女巫这些继承 {@code Monster}，但
 * <b>史莱姆、岩浆怪、恶魂、幻翼、疣猪兽</b>只实现 {@code Enemy} 接口。
 * 用 {@code Monster} 筛，上面这几种一个都扫不到，玩家看到的就是"有些怪物无法识别"。
 *
 * <p>唯一的例外是**已驯服**的动物（狼、猫……）：它们也实现了 {@code Enemy}，
 * 但那是玩家的宠物，不该被阵法打。
 */
public final class FormationTarget {

    private FormationTarget() {
    }

    /** 是不是阵法该对付的敌人。 */
    public static boolean isTarget(Entity entity) {
        if (!(entity instanceof Mob mob)) {
            return false;                       // 玩家、盔甲架、掉落物都不算
        }
        if (!(mob instanceof Enemy)) {
            return false;                       // 村民、牛羊猪、铁傀儡都不算
        }
        if (mob instanceof TamableAnimal tameable && tameable.isTame()) {
            return false;                       // 玩家的宠物不打
        }
        return mob.isAlive();
    }

    /**
     * 这只怪能不能被反重力阵翻过来。
     *
     * <p>排除飞行生物（幻翼、恶魂、蜜蜂、蝙蝠……）和 BOSS（末影龙、凋灵）。
     * 用"没有重力 / 飞行类"来判断，比列类名可靠 —— 模组加进来的飞行怪也自动排除。
     *
     * <p>放在这里而不是各自写一份：阵眼挂标记、渲染时翻转、每刻按住移动，
     * 三处必须用同一个标准，不然会出现"翻了但不按住"这种对不上的毛病。
     */
    public static boolean canInvert(Mob mob) {
        if (mob.isNoGravity()) {
            return false;
        }
        if (mob instanceof FlyingMob || mob instanceof FlyingAnimal || mob instanceof Bat) {
            return false;
        }
        return !(mob instanceof EnderDragon) && !(mob instanceof WitherBoss);
    }
}
