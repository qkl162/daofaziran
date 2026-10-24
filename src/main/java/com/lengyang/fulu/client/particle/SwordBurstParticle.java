package com.lengyang.fulu.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 命中爆发粒子：飞剑打中怪物时炸开的小星星。
 *
 * <p>贴图是一颗小光点（中间白、边缘透明，往金色渐），见 {@code tools/gen_sword_burst.py}。
 * 和剑光粒子的分工：**剑光是青白色的细长光带**（拖尾），
 * **这个是金白色的小星星**（命中那一下），颜色上有对比。
 *
 * <p>三个数值决定了"爆开"的感觉：
 * <ul>
 *   <li><b>初速大、friction 小</b>：一开始冲得快，然后迅速慢下来 —— 先快后慢；</li>
 *   <li><b>生命 10~15 刻</b>：够短，炸一下就没了；</li>
 *   <li><b>一边缩小一边透明</b>：不是"啪"地消失，而是收着尾消失。</li>
 * </ul>
 */
public class SwordBurstParticle extends TextureSheetParticle {

    /** 生命：10~15 刻（用户要求）。 */
    private static final int MIN_LIFE = 10;
    private static final int MAX_LIFE = 15;

    /** 起手颜色（白）和收尾颜色（金）—— 粒子自己会从白渐变到金。 */
    private static final float START_R = 1.0F;
    private static final float START_G = 0.98F;
    private static final float START_B = 0.90F;
    private static final float END_R = 1.0F;
    private static final float END_G = 0.72F;
    private static final float END_B = 0.25F;

    protected SwordBurstParticle(ClientLevel level, double x, double y, double z,
                                 double vx, double vy, double vz) {
        super(level, x, y, z);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;

        this.lifetime = MIN_LIFE + this.random.nextInt(MAX_LIFE - MIN_LIFE + 1);
        this.quadSize = 0.16F + this.random.nextFloat() * 0.10F;
        this.gravity = 0.0F;
        this.friction = 0.78F;      // 阻力大 = 先快后慢
        this.hasPhysics = false;

        this.setColor(START_R, START_G, START_B);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        float life = (float) this.age / (float) this.lifetime;
        // 缩小 + 变透明（同时进行，收着尾消失）
        this.quadSize *= 0.90F;
        this.setAlpha(Math.max(0.0F, 1.0F - life * life));
        // 白 -> 金
        this.setColor(
                START_R + (END_R - START_R) * life,
                START_G + (END_G - START_G) * life,
                START_B + (END_B - START_B) * life);
    }

    /** 工厂：客户端收到"要放一个 sword_burst"的包时调它。 */
    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            SwordBurstParticle particle =
                    new SwordBurstParticle(level, x, y, z, vx, vy, vz);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
