package com.lengyang.fulu.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 反重力粒子：怪物脚底往上飘的小光点（淡紫）。
 *
 * <p>和符文粒子的区别：符文是**钉在原处**的（位置全由服务端算），
 * 而这个要**自己往上飘** —— 所以它保留一点向上的速度、重力取负，
 * 看起来才像"被托起来"。淡紫是模组里唯一用这个色的地方，一眼能认出来。
 */
public class AntiGravityMoteParticle extends TextureSheetParticle {

    /** 淡紫。 */
    private static final float RED = 0.72F;
    private static final float GREEN = 0.58F;
    private static final float BLUE = 0.98F;

    protected AntiGravityMoteParticle(ClientLevel level, double x, double y, double z,
                                      double vx, double vy, double vz) {
        super(level, x, y, z);
        this.xd = vx * 0.4D;
        this.yd = 0.03D + this.random.nextDouble() * 0.03D;   // 自己往上飘
        this.zd = vz * 0.4D;

        this.lifetime = 14 + this.random.nextInt(10);
        this.quadSize = 0.14F + this.random.nextFloat() * 0.08F;
        this.gravity = -0.006F;                              // 负重力 = 一直往上
        this.friction = 0.96F;
        this.hasPhysics = false;

        this.setColor(RED, GREEN, BLUE);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        float life = (float) this.age / (float) this.lifetime;
        if (life < 0.2F) {
            this.setAlpha(life / 0.2F);
        } else {
            this.setAlpha(Math.max(0.0F, (1.0F - life) / 0.8F));
        }
    }

    /** 工厂：客户端收到"要放一个 anti_gravity"的包时调它。 */
    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            AntiGravityMoteParticle particle =
                    new AntiGravityMoteParticle(level, x, y, z, vx, vy, vz);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
