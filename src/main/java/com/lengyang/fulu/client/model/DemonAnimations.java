package com.lengyang.fulu.client.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * 动画名探测：读 {@code animations/formation_demon.animation.json}，把里面所有动画名记下来。
 *
 * <p>为什么需要它：射线这条技能暂时复用 {@code attack}，但以后你加了专属动画（比如
 * {@code cast_ray}），代码里**不需要改**——这里会先探测有没有，有就用专属的，没有就退回
 * {@code attack}。否则 GeckoLib 找不到动画名会报错、而且什么都不播（比用错动画更糟）。
 *
 * <p>只在客户端读资源；服务端调用会直接返回 false。
 */
public final class DemonAnimations {

    private static final ResourceLocation FILE =
            new ResourceLocation("fulu", "animations/formation_demon.animation.json");

    private static final Set<String> NAMES = new HashSet<>();
    private static boolean loaded;

    private DemonAnimations() {
    }

    /** 这个动画名存在吗。 */
    public static boolean has(String name) {
        if (!loaded) {
            load();
        }
        return NAMES.contains(name);
    }

    private static void load() {
        loaded = true;
        try {
            var mc = Minecraft.getInstance();
            if (mc == null) {
                return;
            }
            var res = mc.getResourceManager().getResource(FILE);
            if (res.isEmpty()) {
                return;
            }
            try (var in = res.get().open()) {
                JsonObject root = JsonParser.parseReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                if (root.has("animations")) {
                    for (var key : root.getAsJsonObject("animations").keySet()) {
                        NAMES.add(key);
                    }
                }
            }
        } catch (Exception ignored) {
            // 读不到就当没有，退回默认动画
        }
    }
}
