package com.lengyang.fulu.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 灵气粒子的"怎么画"那一半（只在客户端加载）。
 *
 * <p>继承 {@link TextureSheetParticle}：贴图集里的一张小图，被渲染成一个永远面向
 * 玩家的四边形。它自己已经处理了重力、阻力、生命、淡出这些常规逻辑，
 * 我们只需要在构造函数里改几个数值。
 *
 * <p>三个"看起来廉价的粒子"的常见成因，这里都避开了：
 * <ul>
 *   <li>突然出现、突然消失 → 在 {@link #tick()} 里做淡入淡出；</li>
 *   <li>大小一模一样 → {@code quadSize} 加随机；</li>
 *   <li>往下掉（灵气该往上飘）→ {@code gravity} 取负值。</li>
 * </ul>
 */
public class SpiritMoteParticle extends TextureSheetParticle {

    /** 金色。贴图本身是白灰的，颜色靠这里乘上去，换色不用重画贴图。 */
    private static final float RED = 0.98F;
    private static final float GREEN = 0.84F;
    private static final float BLUE = 0.46F;

    protected SpiritMoteParticle(ClientLevel level, double x, double y, double z,
                                 double vx, double vy, double vz) {
        super(level, x, y, z);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;

        this.lifetime = 24 + this.random.nextInt(16);       // 1.2 ~ 2 秒
        this.quadSize = 0.10F + this.random.nextFloat() * 0.06F;
        this.gravity = -0.012F;                             // 负重力 = 往上飘
        this.friction = 0.94F;
        this.hasPhysics = false;                            // 不被方块挡住

        this.setColor(RED, GREEN, BLUE);
    }

    @Override
    public ParticleRenderType getRenderType() {
        // 半透明：贴图的 alpha 通道要生效，不能用默认的 TERRAIN_SHEET
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        float life = (float) this.age / (float) this.lifetime;
        if (life < 0.25F) {
            this.setAlpha(life / 0.25F);                    // 淡入
        } else if (life > 0.6F) {
            this.setAlpha((1.0F - life) / 0.4F);            // 淡出
        } else {
            this.setAlpha(1.0F);
        }
    }

    /**
     * 工厂：游戏收到"要放一个 spirit_mote"的包时调它。
     *
     * <p>{@code SpriteSet} 是贴图集里属于这个粒子的那几张图（由
     * {@code assets/fulu/particles/spirit_mote.json} 决定），
     * {@code pickSprite} 随机挑一张；只有一张时就是那一张。
     */
    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            SpiritMoteParticle particle = new SpiritMoteParticle(level, x, y, z, vx, vy, vz);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
