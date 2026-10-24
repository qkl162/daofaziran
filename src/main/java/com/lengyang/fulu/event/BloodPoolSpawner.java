package com.lengyang.fulu.event;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.entity.BloodPoolEntity;
import com.lengyang.fulu.registry.ModEntities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 动物被**玩家**击败 → 在它倒下的地方留一滩 {@link BloodPoolEntity 血泊}。
 *
 * <p>用户要求："每次击败动物会在原地留下一个实体，拿玻璃瓶右键就能获得兽血"，
 * 并在被问到"是否只算玩家击杀"时回答**要** —— 所以现在只认玩家击杀。
 *
 * <p>两条判断：
 * <ol>
 *   <li><b>只对 {@link Animal}（动物）</b>：牛、猪、羊、鸡、兔子、马、狐狸、熊猫……
 *       村民不是 Animal（不会有血泊）；鱿鱼也不是（它没有）；</li>
 *   <li><b>必须是玩家打死的</b>：用 {@code getKiller()} 判断，它认的是
 *       "最后伤害来源是玩家" —— 所以玩家用**弓箭**射死也算 ✓，
 *       掉进岩浆烧死、被别的怪咬死、{@code /kill} 都不算 ✗。</li>
 * </ol>
 *
 * <p>驯服的动物（狗、猫）也算 Animal，玩家打死自家狗也会留一滩 —— 这一点没排除。
 *
 * <p><b>代价</b>：专用服务器没有玩家，所以"玩家击杀"这条**没法无头验证** ——
 * {@code -BloodPoolTest} 只能证明反方向（不是玩家打的就不留）。正方向要你手上试。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID)
public final class BloodPoolSpawner {

    private BloodPoolSpawner() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide()) {
            return;                                      // 只在服务端生成
        }
        if (!(dead instanceof Animal)) {
            return;                                      // 只对动物
        }
        // 击杀者：1.20.1 没有 getKiller()，官方映射里是 getKillCredit()（查过 mappings 才敢写）。
        // 它把"玩家射出的箭"算成射手，所以远程击杀也算；/kill、岩浆、别的怪都没有击杀者。
        LivingEntity credit = dead.getKillCredit();
        if (!(credit instanceof Player)) {
            return;                                      // 必须是被玩家打死的
        }
        if (!(dead.level() instanceof ServerLevel server)) {
            return;
        }

        BloodPoolEntity pool = new BloodPoolEntity(ModEntities.BLOOD_POOL.get(), server);
        // 用死亡位置；不额外抬高 —— 血泊有自己的重力，会自己落到地面
        pool.moveTo(dead.getX(), dead.getY(), dead.getZ(), 0.0F, 0.0F);
        server.addFreshEntity(pool);

        if (DEBUG_LOG) {
            FuLu.LOGGER.info("[blood] pool left at {} {} {} for {} (killed by {})",
                    String.format("%.1f", dead.getX()),
                    String.format("%.1f", dead.getY()),
                    String.format("%.1f", dead.getZ()),
                    dead.getType().getDescriptionId(),
                    credit.getName().getString());
        }
    }

    /** 临时打开看日志用（默认关）。 */
    private static final boolean DEBUG_LOG = false;
}
