package com.lengyang.fulu.entity;

import com.lengyang.fulu.registry.ModItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * 血泊：**动物被击败后留在原地的那一滩**。
 *
 * <p>用户要求："每次击败动物会在原地留下一个实体，拿玻璃瓶右键就能获得兽血"。
 *
 * <p>设计上的三个决定（都写在这里，免得以后忘）：
 * <ol>
 *   <li><b>不是生物</b>：它是 {@link Entity} 而不是 {@code Mob} —— 没有 AI、不会被刷怪上限管、
 *       也不会自己走掉，就是地上的一滩；</li>
 *   <li><b>会掉到地面</b>：不关重力，所以哪怕动物死在半空中，血泊也会落到地上；</li>
 *   <li><b>不挡路</b>：{@code canBeCollidedWith} 返回 false（玩家能踩过去），
 *       但 {@code isPickable} 是 true（右键射线打得到它）。这两个是两回事，
 *       混了就会"能右键但走不过去"或者反过来。</li>
 * </ol>
 *
 * <p>右键拿玻璃瓶：扣掉瓶子、给一瓶兽血、然后**血泊消失**（血被接走了）。
 * 一滩只能接一瓶 —— 想改成能接多瓶，说一声。
 */
public class BloodPoolEntity extends Entity {

    /** 玻璃瓶右键后能得到的东西。 */
    private static final int BLOOD_PER_BOTTLE = 1;

    public BloodPoolEntity(EntityType<? extends BloodPoolEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;        // 不参与"站在方块上"的判定
    }

    @Override
    protected void defineSynchedData() {
        // 没有需要同步的状态：它长什么样是固定的
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // 没有额外状态要存
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // 同上
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        // Forge 要求的样板：不写客户端就收不到这个实体
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    /** 能被右键射线打到（但没有碰撞体积，见类注释）。 */
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;                        // 打它没有意义
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /**
     * 玻璃瓶右键 → 一瓶兽血。
     *
     * <p>手里不是玻璃瓶就 {@link InteractionResult#PASS}：**什么都不做**，把机会让给别的逻辑
     * （否则拿着方块右键血泊会放不下方块，那种小毛病最难查）。
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.GLASS_BOTTLE)) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide()) {
            // 客户端只负责摆手；真正的扣瓶/给血在服务端，不然两端各扣一次
            return InteractionResult.SUCCESS;
        }

        held.shrink(1);
        ItemStack blood = new ItemStack(ModItems.BEAST_BLOOD.get(), BLOOD_PER_BOTTLE);
        if (!player.getInventory().add(blood)) {
            player.drop(blood, false);       // 背包装不下，掉在脚下
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.BOTTLE_FILL,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        this.discard();                      // 血被接走了，血泊消失
        return InteractionResult.SUCCESS;
    }

    /** 不需要位置更新（它永远不动），省带宽。 */
    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 64.0D * 64.0D;
    }
}
