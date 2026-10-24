package com.lengyang.fulu.registry;

import com.lengyang.fulu.FuLu;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 粒子登记表。
 *
 * <p>粒子和方块/物品不一样，它要登记<b>两半</b>，缺一半就是"服务端发了、客户端不画"：
 * <ol>
 *   <li><b>服务端</b>：这里登记的 {@link ParticleType}（一个 id，加一个网络编码方式）。
 *       服务端只认这个 id，它负责把"在哪儿放什么粒子"发给玩家。</li>
 *   <li><b>客户端</b>：一个"看到 id 之后怎么画"的 provider，
 *       在 {@code RegisterParticleProvidersEvent} 里挂上（见
 *       {@link com.lengyang.fulu.client.ModClientSetup}）。</li>
 * </ol>
 *
 * <p>这里用 {@link SimpleParticleType}：不带任何额外参数、只靠一张贴图的粒子。
 * 它自带的网络编码方式就是"什么都不传"，所以不需要我们自己写 Codec。
 *
 * <p>{@code new SimpleParticleType(false)} 里的 {@code false} 是
 * {@code overrideLimiter} —— 表示这个粒子**受**客户端的粒子数量上限约束
 * （设成 true 就是"永远不裁剪"，只有极少数原版粒子这么干，别乱开）。
 */
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, FuLu.MOD_ID);

    /** 灵气：金色的小光点，阵眼每 2 秒往外飘一把。 */
    public static final RegistryObject<SimpleParticleType> SPIRIT_MOTE =
            PARTICLE_TYPES.register("spirit_mote", () -> new SimpleParticleType(false));

    /**
     * 命中爆发：飞剑打中怪物时炸开的一圈小星星（中间白、边缘金）。
     */
    public static final RegistryObject<SimpleParticleType> SWORD_BURST =
            PARTICLE_TYPES.register("sword_burst", () -> new SimpleParticleType(false));

    /**
     * 符文：绕着飞剑从剑尾转到剑锋的一圈符印。
     *
     * <p>位置由飞剑实体每刻算好（一圈螺旋），客户端只负责画一颗粒子。
     */
    public static final RegistryObject<SimpleParticleType> RUNE_MOTE =
            PARTICLE_TYPES.register("rune_mote", () -> new SimpleParticleType(false));

    /**
     * 反重力：怪物脚底往上飘的淡紫光点。
     *
     * <p>和符文粒子的区别：符文钉在原处（位置全由服务端算），这个自己往上飘。
     */
    public static final RegistryObject<SimpleParticleType> ANTI_GRAVITY =
            PARTICLE_TYPES.register("anti_gravity", () -> new SimpleParticleType(false));

    private ModParticles() {
    }

    public static void register(IEventBus modBus) {
        PARTICLE_TYPES.register(modBus);
    }
}
