package com.lengyang.fulu.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 符文粒子的"怎么画"那一半（只在客户端加载）。
 *
 * <p>贴图是一个小小的发光符印，见 {@code tools/gen_rune_texture.py}。
 * 位置完全由服务端算好（飞剑沿剑轴的一圈螺旋），粒子自己**不飞、不飘、不下落** ——
 * 它只是"在这里亮一下然后消失"，所以那一圈符文看起来才是绕着剑转的。
 *
 * <p>做成"亮一下就不动"而不是"飞出去"，是因为符文要连成一条螺旋线：
 * 粒子一旦自己乱动，线就散了。
 */
public class RuneMoteParticle extends TextureSheetParticle {

    /** 淡青白，和剑身的银蓝一致。 */
    private static final float RED = 0.84F;
    private static final float GREEN = 0.95F;
    private static final float BLUE = 1.0F;

    protected RuneMoteParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);

        this.lifetime = 8 + this.random.nextInt(5);      // 0.4 ~ 0.6 秒
        this.quadSize = 0.16F + this.random.nextFloat() * 0.08F;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.xd = this.yd = this.zd = 0.0D;              // 停在原处

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
        // 淡淡的淡入淡出，免得螺旋线上出现硬边
        if (life < 0.2F) {
            this.setAlpha(life / 0.2F);
        } else {
            this.setAlpha(Math.max(0.0F, (1.0F - life) / 0.8F));
        }
    }

    /** 工厂：客户端收到"要放一个 rune_mote"的包时调它。 */
    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            RuneMoteParticle particle = new RuneMoteParticle(level, x, y, z);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
