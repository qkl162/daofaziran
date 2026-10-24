package com.lengyang.fulu.client;

import com.lengyang.fulu.network.CastEssencePacket;
import com.lengyang.fulu.network.ModNetwork;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 炼妖能力只剩两条键位（用户要求：其它全部删掉）：
 *
 * <ul>
 *   <li><b>R —— 选择生物能力</b>：弹出妖丹轮盘，鼠标指哪颗选哪颗；</li>
 *   <li><b>Z —— 释放生物技能</b>：用当前选中的那颗妖丹施法。</li>
 * </ul>
 *
 * <p>旧的 N（选择）/ G（释放）以及它们背后的"生物能力"整套逻辑已删除，
 * 现在只有"妖丹匣 + 轮盘 + 施法"这一条链路。
 */
@Mod.EventBusSubscriber(modid = "fulu", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RefiningKeys {

    /** 轮盘键（R）：选择生物能力。 */
    public static final KeyMapping WHEEL = new KeyMapping(
            "key.fulu.wheel",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.fulu.category");

    /** 换技能键（V）：在多技能妖丹的多个技能之间轮换。 */
    public static final KeyMapping SKILL = new KeyMapping(
            "key.fulu.skill",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.fulu.category");

    /** 施法键（Z）：释放生物技能。 */
    public static final KeyMapping CAST = new KeyMapping(
            "key.fulu.cast",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "key.fulu.category");

    private RefiningKeys() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterKeyMappingsEvent event) {
        event.register(WHEEL);
        event.register(SKILL);
        event.register(CAST);
    }

    /** 键位轮询。 */
    @Mod.EventBusSubscriber(modid = "fulu", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Poller {

        private Poller() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();

            // R：**选择生物能力**（轮盘里已经包含"这颗妖丹的哪个技能"）
            while (WHEEL.consumeClick()) {
                if (mc.player == null || mc.screen != null) {
                    continue;
                }
                if (ClientEssence.findBox().isEmpty()) {
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component
                            .translatable("msg.fulu.wheel.nobox"), true);
                    continue;
                }
                mc.setScreen(new EssenceWheelScreen());
            }

            // V：**选技能**（打开面板自己挑，而不是顺序轮换）
            while (SKILL.consumeClick()) {
                if (mc.player == null || mc.screen != null) {
                    continue;
                }
                var kind = ClientEssence.kindAt(ClientEssence.activeSlot());
                if (kind == null) {
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component
                            .translatable("msg.fulu.skill.none"), true);
                    continue;
                }
                if (kind.skills().length <= 1) {
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component
                            .translatable("msg.fulu.skill.single"), true);
                    continue;
                }
                mc.setScreen(new SkillSelectScreen(ClientEssence.activeSlot(), kind));
            }

            // Z：施法
            while (CAST.consumeClick()) {
                if (mc.player == null || mc.screen != null) {
                    continue;
                }
                int slot = ClientEssence.activeSlot();
                // **按技能**判断冷却（之前只按格子，导致一个技能冷却会挡住整格）
                int cd = ClientEssence.cooldownSeconds(slot, ClientEssence.subSkill());
                if (cd > 0) {
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component
                            .translatable("msg.fulu.cast.cooldown", cd), true);
                    continue;
                }
                // 凋零骷髅 => 挥砍后屏幕边缘冒黑雾（冥气反噬）
                if (ClientEssence.kindAt(slot)
                        == com.lengyang.fulu.refining.EssenceKind.WITHER_SKELETON) {
                    SoulSlashOverlay.trigger();
                }
                ModNetwork.CHANNEL.sendToServer(
                        new CastEssencePacket(slot, ClientEssence.subSkill()));
            }
        }
    }
}
