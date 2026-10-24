package com.lengyang.fulu.combat;

import net.minecraft.world.entity.player.Player;

/**
 * 次数盾 —— **一个刻意留空的接缝**。
 *
 * <p>用户的要求里有一句"减伤后的伤害再走次数盾逻辑"，但**次数盾在模组里还不存在**，
 * 而且没有说清它是什么：一次能挡几下？由什么给（替身符？阵法？）？打完了会怎样？
 * （是消失、还是进入冷却、还是碎掉？）
 *
 * <p>不自己编规则，所以这里只留一个**原样返回**的接缝：现在的行为就是
 * "减伤后的伤害直接生效"。等规则定了，只需要改这一个方法 —— 调用方
 * （{@code ResistanceUpHandler}）不用动。
 *
 * <p>为什么单独一个类而不是在 handler 里留个 TODO：接缝要有**明确的签名**，
 * 以后填的时候不用重新想"入口在哪儿、拿得到什么"。这个方法的参数就是它将来能用的全部信息。
 */
public final class HitCountShield {

    private HitCountShield() {
    }

    /**
     * 让次数盾处理一次（已经减伤过的）伤害，返回真正要扣的血。
     *
     * <p>现在：原样返回（没有次数盾）。
     *
     * @param player 受伤的玩家
     * @param damage 抗性提升减伤之后、准备扣掉的伤害
     * @return 最终扣掉的伤害
     */
    public static float absorb(Player player, float damage) {
        return damage;
    }
}
