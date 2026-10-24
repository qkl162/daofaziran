package com.lengyang.fulu.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * 飞剑的**几何**：由 {@code tools/bbmodel_to_java.py} 从
 * {@code ref/mohei_xuanjian.bbmodel}（Blockbench / 基岩格式）生成，请勿手改。
 *
 * <p>转换时处理了两处格式差异：
 * <ol>
 *   <li>基岩格式用**逐面 UV**，Java 实体模型只支持**盒式展开** ——
 *       贴图是重新排版过的（每个方块的六个面搬进盒式展开的槽位），
 *       换贴图必须重跑转换脚本；</li>
 *   <li>基岩模型 y 向上、站在 y=0；Java 模型 y 向下 —— 所有 y 取负。</li>
 * </ol>
 *
 * <p><b>骨骼</b>（按用户规范重组，和 bbmodel 里的分组不完全一样）：
 * <pre>
 *   blade  剑身（含剑尖）—— **父骨骼**，转它整把剑跟着转
 *    ├─ guard  护手
 *    └─ hilt   剑柄（含末端）
 * </pre>
 * 动画写在 {@link FlyingSwordModel} 里 —— 这个文件只描述形状。
 */
public final class FlyingSwordShape {

    private FlyingSwordShape() {
    }

    /** 形状定义（贴图 128x128）。 */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition blade = root.addOrReplaceChild("blade",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5f, -26f, -0.5f, 1, 8, 1)
                        .texOffs(4, 0).addBox(-2.5f, -26f, -1f, 2, 8, 2)
                        .texOffs(12, 0).addBox(-0.5f, -26f, -1.5f, 1, 8, 3)
                        .texOffs(20, 0).addBox(0.5f, -26f, -1f, 2, 8, 2)
                        .texOffs(28, 0).addBox(2.5f, -26f, -0.5f, 1, 8, 1)
                        .texOffs(32, 0).addBox(-3.5f, -34f, -0.5f, 1, 8, 1)
                        .texOffs(36, 0).addBox(-2.5f, -34f, -1f, 2, 8, 2)
                        .texOffs(44, 0).addBox(-0.5f, -34f, -1.5f, 1, 8, 3)
                        .texOffs(52, 0).addBox(0.5f, -34f, -1f, 2, 8, 2)
                        .texOffs(60, 0).addBox(2.5f, -34f, -0.5f, 1, 8, 1)
                        .texOffs(64, 0).addBox(-3.5f, -42f, -0.5f, 1, 8, 1)
                        .texOffs(68, 0).addBox(-2.5f, -42f, -1f, 2, 8, 2)
                        .texOffs(76, 0).addBox(-0.5f, -42f, -1.5f, 1, 8, 3)
                        .texOffs(84, 0).addBox(0.5f, -42f, -1f, 2, 8, 2)
                        .texOffs(92, 0).addBox(2.5f, -42f, -0.5f, 1, 8, 1)
                        .texOffs(96, 0).addBox(-3f, -43f, -1.5f, 6, 1, 3)
                        .texOffs(0, 11).addBox(-2.5f, -44f, -1.5f, 5, 1, 3)
                        .texOffs(16, 11).addBox(-2f, -45f, -1f, 4, 1, 2)
                        .texOffs(28, 11).addBox(-1.5f, -46f, -1f, 3, 1, 2)
                        .texOffs(38, 11).addBox(-1f, -47f, -0.5f, 2, 1, 1)
                        .texOffs(44, 11).addBox(-0.5f, -48f, -0.5f, 1, 1, 1)
                        .mirror(),
                PartPose.ZERO);

        PartDefinition guard = blade.addOrReplaceChild("guard",
                CubeListBuilder.create()
                        .texOffs(48, 11).addBox(-1.5f, -18f, -2f, 3, 4, 4)
                        .texOffs(62, 11).addBox(-4.5f, -17f, -1f, 9, 3, 2)
                        .mirror(),
                PartPose.ZERO);

        PartDefinition hilt = blade.addOrReplaceChild("hilt",
                CubeListBuilder.create()
                        .texOffs(84, 11).addBox(-1.5f, -15f, -1.5f, 3, 11, 3)
                        .texOffs(96, 11).addBox(-2f, -4f, -2f, 4, 4, 4)
                        .mirror(),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 128);
    }
}
