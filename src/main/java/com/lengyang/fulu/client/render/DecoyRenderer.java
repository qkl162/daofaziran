package com.lengyang.fulu.client.render;

import com.lengyang.fulu.entity.DecoyEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 假身的渲染器：**画一个玩家形状的模型**（Steve 皮肤）。
 *
 * <p>为什么用 {@link HumanoidModel} 而不是 {@code PlayerModel}：
 * 两者 UV 布局完全一致（头 0,0 / 身体 16,16 / 手臂 40,16 / 腿 0,16），
 * 而 {@code HumanoidModel} 的构造签名更稳定，也不需要 {@code AbstractClientPlayer}。
 *
 * <p>皮肤是从原版客户端 jar 里抠出来的 `wide/steve.png`，放在
 * {@code assets/fulu/textures/entity/decoy.png}。
 */
public class DecoyRenderer extends MobRenderer<DecoyEntity, HumanoidModel<DecoyEntity>> {

    /** 模型层 id（在 ModClientSetup 里注册 LayerDefinition）。 */
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("fulu", "decoy"), "main");

    private static final ResourceLocation SKIN =
            new ResourceLocation("fulu", "textures/entity/decoy.png");

    public DecoyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(DecoyEntity entity) {
        return SKIN;
    }
}
