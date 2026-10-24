package com.lengyang.fulu.registry;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.block.FormationEyeBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块实体类型登记表（教程第三步里"专门登记方块实体类型的文件"）。
 *
 * <p>"方块实体类型"和"方块实体"也是一对多的关系：
 * <pre>
 *   BlockEntityType  登记一次的"种类"（这里是"阵眼的方块实体"）
 *   BlockEntity      世界里每一个阵眼各一份实例
 * </pre>
 *
 * <p>登记时最关键的是 {@code BlockEntityType.Builder.of(...)} 的第二个参数：
 * <b>要把方块传进去</b>，这才是"绑定到阵眼方块上"的那一步。
 * 不传的话，阵眼放下时游戏根本不知道它该配哪个方块实体，
 * {@code newBlockEntity} 会被调用但返回的对象没有任何类型归属，
 * 表现为方块实体"存在但从不 tick"。
 *
 * <p>另外：方块实体类型**不需要**任何 json 资源文件（贴图/模型/语言都在方块那边），
 * 所以这一步没有任何 assets 或 data 文件要写。
 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, FuLu.MOD_ID);

    /**
     * 阵眼的方块实体。名字与方块保持一致（都叫 {@code formation_eye}），
     * 这样以后看日志、看存档一眼就能对上。
     */
    public static final RegistryObject<BlockEntityType<FormationEyeBlockEntity>> FORMATION_EYE =
            BLOCK_ENTITIES.register("formation_eye",
                    () -> BlockEntityType.Builder
                            .of(FormationEyeBlockEntity::new, ModBlocks.FORMATION_EYE.get())
                            // build(null)：不需要 DataFixer（那是原版升级旧存档才要的）
                            .build(null));

    private ModBlockEntities() {
    }

    /** 由模组主文件 FuLu 调用。 */
    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
