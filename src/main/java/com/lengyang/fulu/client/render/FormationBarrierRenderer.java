package com.lengyang.fulu.client.render;

import com.lengyang.fulu.block.FormationEyeBlock;
import com.lengyang.fulu.block.TalismanKind;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

public final class FormationBarrierRenderer {
   private static final double RADIUS = (double)10.0F;
   private static final double RENDER_DISTANCE = (double)64.0F;
   private static final long ANIMATE_MS = 2000L;
   private static final float SURFACE_ALPHA = 0.3F;
   private static final float GRID_ALPHA = 0.42F;
   private static final float RING_ALPHA = 0.88F;
   private static final float RUNE_ALPHA = 0.9F;
   private static final int NEAR_STARS = 24;
   private static final int MID_STARS = 70;
   private static final int FAR_STARS = 120;
   private static final int NEBULAE = 22;
   private static final int CONSTELLATIONS = 4;
   private static final int RUNES = 7;
   private static final int GALAXY_STARS = 150;
   private static final int DENSE_TINY = 4800;
   private static final int DENSE_SMALL = 320;
   private static final int DENSE_BRIGHT = 60;
   private static final double BAND_CENTER = (double)0.5F;
   private static final double BAND_TILT = 0.3;
   private static final int BAND_GLOW = 90;
   private static final int MAX_METEORS = 3;
   private static final double HOLE_DY = (double)8.0F;
   private static final double HOLE_DX = (double)3.0F;
   private static final double HOLE_DIAMETER = (double)2.5F;
   private static final double HOLE_DISC_SECONDS = (double)10.0F;
   private static final int INFALL_STARS = 6;
   private static final int DRAGON_NODES = 24;
   private static final double DRAGON_LENGTH = (double)17.0F;
   private static final double DRAGON_SECONDS_PER_LAP = (double)40.0F;
   private static final int DRAGON_TRAIL = 8;
   private static final int MERIDIANS = 12;
   private static final int PARALLELS = 3;
   private static final int SEGMENTS = 24;
   private static final Map<BlockPos, Anim> ANIMS = new HashMap();
   private static final List<BlockPos> EYES = new ArrayList();
   private static int scanCooldown = 0;
   private static boolean wasInsideAny = false;

   private FormationBarrierRenderer() {
   }

   public static void register() {
      MinecraftForge.EVENT_BUS.register(FormationBarrierRenderer.class);
      // 水墨符的山水阵壁：单独一趟带贴图的绘制，绑在同一批阵眼上
      InkBarrierRenderer.register();
   }

   /** 当前客户端已知的阵眼列表（供 {@code InkBarrierRenderer} 复用，避免重复扫描）。 */
   public static java.util.List<BlockPos> eyesView() {
      return EYES;
   }

   private static float[] colorOf(TalismanKind kind) {
      float[] var10000;
      switch (kind) {
         case CHAOS -> var10000 = new float[]{0.62F, 0.1F, 1.0F};
         case SWORD -> var10000 = new float[]{0.05F, 0.9F, 1.0F};
         case ANTIGRAVITY -> var10000 = new float[]{0.35F, 0.75F, 1.0F};
         case STAND_IN -> var10000 = new float[]{1.0F, 0.78F, 0.1F};
         case SHIFT_BLAME -> var10000 = new float[]{0.1F, 0.85F, 0.35F};
          case SUMMON -> var10000 = new float[]{0.25F, 0.88F, 0.82F};
          case SLIPPERY -> var10000 = new float[]{0.50F, 0.82F, 1.0F};
          case BLIND -> var10000 = new float[]{0.30F, 0.30F, 0.38F};
          case LOOP -> var10000 = new float[]{0.25F, 0.25F, 0.75F};
          case DISTANCE -> var10000 = new float[]{0.75F, 0.50F, 1.0F};
          // 水墨符：先用墨灰压掉原来的暖色，真正的画由 InkBarrierLayer 叠在上面
          case INK -> var10000 = new float[]{0.55F, 0.60F, 0.66F};
         case TIME_SLOW -> var10000 = new float[]{1.0F, 0.843F, 0.0F};   // 淡金 #FFD700
         case REFINING -> var10000 = new float[]{0.627F, 0.125F, 0.251F};  // 暗红紫 #A02040
         default -> var10000 = new float[]{1.0F, 0.94F, 0.7F};
      }

      return var10000;
   }

   private static float[] starColor(int idx) {
      float[] var10000;
      switch (idx) {
         case 0 -> var10000 = new float[]{1.0F, 1.0F, 1.0F};
         case 1 -> var10000 = new float[]{0.72F, 0.87F, 1.0F};
         case 2 -> var10000 = new float[]{0.87F, 0.72F, 1.0F};
         default -> var10000 = new float[]{1.0F, 0.91F, 0.62F};
      }

      return var10000;
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
         Minecraft mc = Minecraft.getInstance();
         ClientLevel level = mc.level;
         if (level != null && mc.player != null) {
            Vec3 cam = event.getCamera().getPosition();
            rescanEyes(level, mc.player.position());
            long now = System.currentTimeMillis();
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(-cam.x, -cam.y, -cam.z);

            for(BlockPos eye : EYES) {
               TalismanKind kind = talismanAt(level, eye);
               Anim anim = (Anim)ANIMS.computeIfAbsent(eye, (k) -> new Anim());
               if (anim.hadTalisman && kind == TalismanKind.EMPTY) {
                  shatter(level, eye);
                  anim.activateMs = 0L;
                  anim.absorbMs = now;
               }

               if (kind != TalismanKind.EMPTY) {
                  anim.lastKind = kind;
               }

               if (!anim.hadTalisman && kind != TalismanKind.EMPTY) {
                  anim.activateMs = now;
               }

               anim.hadTalisman = kind != TalismanKind.EMPTY;
               long dt = Math.max(0L, now - anim.lastMs);
               anim.lastMs = now;
               float step = (float)dt / 2000.0F;
               if (anim.absorbMs != 0L) {
                  anim.target = 1.0F;
                  anim.level = Math.min(1.0F, anim.level + step);
               } else {
                  anim.target = kind == TalismanKind.EMPTY ? 0.0F : 1.0F;
                  if (anim.level < anim.target) {
                     anim.level = Math.min(anim.target, anim.level + step);
                  } else if (anim.level > anim.target) {
                     anim.level = Math.max(anim.target, anim.level - step);
                  }
               }

               if (!(anim.level <= 0.001F)) {
                  if (!anim.built) {
                     buildSky(anim, eye);
                  }

                  playAmbientSounds(level, eye, anim, now, kind);
                  drawSky(level, pose, eye, anim, mc.player.position(), now, kind);
                  drawBarrier(pose, eye, kind, anim, mc.player.position(), hasOverlap(level, eye), now);
               }
            }

            for(Map.Entry<BlockPos, Anim> entry : ANIMS.entrySet()) {
               Anim ga = (Anim)entry.getValue();
               if (entry.getKey() != null && !EYES.contains(entry.getKey()) && ga.absorbMs != 0L && ga.level > 0.001F) {
                  drawBarrier(pose, (BlockPos)entry.getKey(), ga.lastKind, ga, mc.player.position(), false, now);
               }
            }

            boolean insideAny = false;

            for(BlockPos eye : EYES) {
               double cx = (double)eye.getX() + (double)0.5F;
               double cy = (double)eye.getY() + (double)0.5F;
               double cz = (double)eye.getZ() + (double)0.5F;
               if (Math.hypot(mc.player.getX() - cx, mc.player.getZ() - cz) <= (double)10.0F && Math.abs(mc.player.getY() - cy) <= (double)10.0F) {
                  insideAny = true;
                  break;
               }
            }

            if (wasInsideAny && !insideAny) {
               mc.player.displayClientMessage(Component.translatable("msg.fulu.formation.field.left"), true);
            }

            wasInsideAny = insideAny;
            pose.popPose();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            ANIMS.entrySet().removeIf((e) -> ((Anim)e.getValue()).level <= 0.001F && !EYES.contains(e.getKey()));
         } else {
            ANIMS.clear();
            EYES.clear();
         }
      }
   }

   private static void buildSky(Anim anim, BlockPos eye) {
      Random rnd = new Random(eye.asLong() * 31L + 24301L);
      Star[] stars = new Star[214];
      int i = 0;

      for(int n = 0; n < 24; ++n) {
         stars[i++] = makeStar(rnd, FormationBarrierRenderer.Layer.NEAR);
      }

      for(int n = 0; n < 70; ++n) {
         stars[i++] = makeStar(rnd, FormationBarrierRenderer.Layer.MID);
      }

      for(int n = 0; n < 120; ++n) {
         stars[i++] = makeStar(rnd, FormationBarrierRenderer.Layer.FAR);
      }

      int denseCount = 5180;
      DenseStar[] dense = new DenseStar[denseCount];
      int di = 0;

      for(int n = 0; n < denseCount; ++n) {
         DenseStar ds = new DenseStar();
         ds.angle = rnd.nextDouble() * Math.PI * (double)2.0F;
         boolean onBand = rnd.nextDouble() < 0.6;
         if (onBand) {
            double base = (double)0.5F + 0.3 * Math.sin(ds.angle - 0.6);
            ds.height = base + (rnd.nextDouble() - (double)0.5F) * 0.18;
         } else {
            ds.height = 0.04 + rnd.nextDouble() * 0.94;
         }

         ds.height = Math.max(0.02, Math.min(0.99, ds.height));
         ds.brightness = (float)(0.35 + rnd.nextDouble() * 0.65);
         if (n < 60) {
            ds.sizeClass = 2;
            ds.brightness = (float)Math.min((double)1.0F, (double)ds.brightness + (double)0.25F);
         } else if (n < 380) {
            ds.sizeClass = 1;
         } else {
            ds.sizeClass = 0;
         }

         double pick = rnd.nextDouble();
         ds.color = pick < 0.7 ? 0 : (pick < 0.85 ? 1 : (pick < 0.95 ? 2 : 3));
         dense[di++] = ds;
      }

      Nebula[] nebulae = new Nebula[22];

      for(int n = 0; n < 22; ++n) {
         Nebula nb = new Nebula();
         nb.layer = n % 3 == 0 ? FormationBarrierRenderer.Layer.NEAR : (n % 3 == 1 ? FormationBarrierRenderer.Layer.MID : FormationBarrierRenderer.Layer.FAR);
         nb.angle = rnd.nextDouble() * Math.PI * (double)2.0F;
         nb.height = 0.12 + rnd.nextDouble() * 0.8;
         nb.size = 3.4 + rnd.nextDouble() * 4.2;
         nb.radii = new double[8];

         for(int k = 0; k < 8; ++k) {
            nb.radii[k] = 0.55 + rnd.nextDouble() * 0.85;
         }

         nb.color = rnd.nextInt(2);
         nebulae[n] = nb;
      }

      Constellation[] constellations = new Constellation[4];

      for(int n = 0; n < 4; ++n) {
         Constellation cs = new Constellation();
         int count = 4 + rnd.nextInt(3);
         cs.starIndex = new int[count];

         for(int k = 0; k < count; ++k) {
            cs.starIndex[k] = 24 + rnd.nextInt(70);
         }

         cs.color = rnd.nextInt(2);
         cs.phase = rnd.nextDouble() * Math.PI * (double)2.0F;
         constellations[n] = cs;
      }

      Meteor[] meteors = new Meteor[3];

      for(int n = 0; n < 3; ++n) {
         meteors[n] = new Meteor();
      }

      anim.stars = stars;
      anim.dense = dense;
      anim.nebulae = nebulae;
      anim.constellations = constellations;
      anim.meteors = meteors;
      anim.lastMeteorMs = System.currentTimeMillis();
      anim.holePhase = rnd.nextDouble() * Math.PI * (double)2.0F;
      anim.built = true;
   }

   private static Star makeStar(Random rnd, Layer layer) {
      Star s = new Star();
      s.layer = layer;
      s.angle = rnd.nextDouble() * Math.PI * (double)2.0F;
      s.angleFrom = s.angle;
      s.angleTo = s.angle;
      s.height = 0.06 + rnd.nextDouble() * 0.9;
      s.phase = rnd.nextDouble() * Math.PI * (double)2.0F;
      s.period = (double)2.0F + rnd.nextDouble() * (double)3.0F;
      if (layer == FormationBarrierRenderer.Layer.NEAR) {
         s.sizeClass = rnd.nextInt(3) == 0 ? 2 : 1;
      } else if (layer == FormationBarrierRenderer.Layer.MID) {
         s.sizeClass = rnd.nextInt(4) == 0 ? 2 : (rnd.nextInt(2) == 0 ? 1 : 0);
      } else {
         s.sizeClass = rnd.nextInt(6) == 0 ? 1 : 0;
      }

      if (layer == FormationBarrierRenderer.Layer.NEAR) {
         s.color = rnd.nextBoolean() ? 3 : 0;
      } else if (layer == FormationBarrierRenderer.Layer.MID) {
         s.color = rnd.nextInt(4) == 0 ? 2 : 0;
      } else {
         s.color = rnd.nextBoolean() ? 2 : 1;
      }

      s.spin = (rnd.nextDouble() - (double)0.5F) * 0.6;
      return s;
   }

   private static double spinOf(Layer layer, long now) {
      return (double)(now % 240000L) / (double)240000.0F * Math.PI * (double)2.0F * layer.spinMul;
   }

   private static void drawSky(ClientLevel level, PoseStack pose, BlockPos eye, Anim anim, Vec3 playerPos, long now, TalismanKind kind) {
      // 水墨阵：**不要星空、不要粒子**。整面阵壁交给 InkBarrierRenderer 的山水贴图，
      // 这层原有的星点（END_ROD / FIREWORK，近距离会显示成大白方块）会破坏画面。
      if (kind == TalismanKind.INK) {
         return;
      }
      double cx = (double)eye.getX() + (double)0.5F;
      double cy = (double)eye.getY() + (double)0.5F;
      double cz = (double)eye.getZ() + (double)0.5F;
      double dist = Math.hypot(playerPos.x - cx, playerPos.z - cz);
      boolean inside = dist <= (double)10.0F && Math.abs(playerPos.y - cy) <= (double)10.0F;
      float boost = (inside ? 1.18F : 1.0F) * starBoostFor(kind);
      double grow = (double)anim.level;
      if (!(grow <= 0.02)) {
         pose.pushPose();
         pose.translate(cx, cy, cz);
         Tesselator tes = Tesselator.getInstance();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         BufferBuilder tri = tes.getBuilder();
         tri.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
         float haloPulse = 0.018F + 0.014F * (float)((double)0.5F + (double)0.5F * Math.sin((double)now / (double)4000.0F));
         float backdrop = surfaceAlphaFor(kind) < 0.25F ? 0.2F : 0.14F;
         addDome(tri, pose, (double)15.0F * grow, 0.045F, 0.038F, 0.14F, backdrop, 14);
         addDome(tri, pose, 14.2 * grow, 0.1F, 0.08F, 0.22F, haloPulse, 18);
         addHorizonGlow(tri, pose, 13.799999999999999 * grow, 1.0F, 0.45F, 0.55F, 0.055F, false);
         addHorizonGlow(tri, pose, (double)14.0F * grow, 0.1F, 0.14F, 0.4F, 0.075F, true);
         drawBandGlow(tri, pose, now, grow);

         for(Nebula nb : anim.nebulae) {
            double ang = nb.angle + spinOf(nb.layer, now) * (double)0.5F;
            double y = nb.height * (double)10.0F * grow;
            double r = radiusAt(y) * nb.layer.radiusMul;
            float[] col = nb.color == 0 ? new float[]{0.90F, 0.42F, 0.20F}
                    : (nb.color == 1 ? new float[]{0.25F, 0.72F, 0.85F}
                    : new float[]{0.85F, 0.40F, 0.65F});
            addBlob(tri, pose, r, y, ang, nb.size * grow, nb.radii, col, 0.30F);
         }

         drawBlackHoleBody(tri, pose, anim, now, grow, discColorOf(kind));
         BufferUploader.drawWithShader(tri.end());
         RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
         BufferBuilder buf = tes.getBuilder();
         buf.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
         updateMeteors(anim, now);
         updateDrift(anim, now);
         float meteorLight = 0.0F;

         for(Meteor m : anim.meteors) {
            if (m.active) {
               double age = (double)(now - m.startMs) / (double)m.lifeMs;
               if (age >= (double)1.0F) {
                  m.active = false;
               } else {
                  meteorLight = Math.max(meteorLight, (float)((double)1.0F - age) * 0.6F);
                  drawMeteor(buf, pose, m, age, grow);
               }
            }
         }

         float tw = 1.0F + meteorLight;
         drawDenseStars(buf, pose, anim, now, grow, boost * tw, inside);

         for(Star s : anim.stars) {
            if (s.sizeClass != 0) {
               drawStar(buf, pose, s, now, grow, boost * tw, inside);
            }
         }

         for(Constellation cs : anim.constellations) {
            float blink = 0.1F + 0.1F * (float)((double)0.5F + (double)0.5F * Math.sin((double)now / (double)1800.0F + cs.phase));
            float[] col = cs.color == 0 ? new float[]{0.4F, 0.28F, 0.72F} : new float[]{0.26F, 0.38F, 0.7F};

            for(int k = 0; k + 1 < cs.starIndex.length; ++k) {
               curvedLine(buf, pose, anim.stars[cs.starIndex[k]], anim.stars[cs.starIndex[k + 1]], now, grow, col, blink);
            }
         }

         for(Nebula nb : anim.nebulae) {
            double angN = nb.angle + spinOf(nb.layer, now) * (double)0.5F;
            double yN = nb.height * (double)10.0F * grow;
            double rN = radiusAt(yN) * nb.layer.radiusMul;
            float[] var10000;
            switch (nb.color) {
               case 0 -> var10000 = new float[]{1.0F, 0.55F, 0.28F};
               case 1 -> var10000 = new float[]{0.45F, 0.9F, 1.0F};
               default -> var10000 = new float[]{1.0F, 0.6F, 0.85F};
            }

            float[] col = var10000;
            Random nr = new Random((long)(nb.angle * (double)1000000.0F) ^ (long)(nb.height * (double)1000000.0F));

            for(int k = 0; k < 18; ++k) {
               double a0 = angN + (nr.nextDouble() - (double)0.5F) * nb.size * 0.12;
               double a1 = a0 + (nr.nextDouble() - (double)0.5F) * 0.1;
               double y0 = yN + (nr.nextDouble() - (double)0.5F) * nb.size * 0.9;
               double y1 = y0 + (nr.nextDouble() - (double)0.5F) * 0.8;
               double rr0 = rN + (nr.nextDouble() - (double)0.5F) * nb.size * 0.7;
               double rr1 = rr0 + (nr.nextDouble() - (double)0.5F) * 0.9;
               float aa = 0.35F + nr.nextFloat() * 0.45F;
               if (y0 > 0.2 && y1 > 0.2 && y0 < (double)10.0F && y1 < (double)10.0F) {
                  line(buf, pose, rr0, y0, a0, rr1, y1, a1, col[0], col[1], col[2], aa);
               }
            }
         }

         drawGalaxyBand(buf, pose, now, grow, boost);
         drawRunes(buf, pose, now, grow, boost);
         float[] disc = discColorOf(kind);
         drawBlackHoleDisc(buf, pose, anim, now, grow, disc);
         drawDragon(buf, pose, anim, playerPos, now, grow, boost, disc);
         spawnDragonDust(level, eye, anim, now);
         BufferUploader.drawWithShader(buf.end());
         pose.popPose();
      }
   }

   private static void drawStar(BufferBuilder buf, PoseStack pose, Star s, long now, double grow, float boost, boolean inside) {
      double baseAngle = s.angle;
      if (s.shiftStartMs != 0L) {
         double t = Math.min((double)1.0F, (double)(now - s.shiftStartMs) / (double)2500.0F);
         double e = t * t * ((double)3.0F - (double)2.0F * t);
         baseAngle = s.angleFrom + (s.angleTo - s.angleFrom) * e;
         if (t >= (double)1.0F) {
            s.angle = s.angleTo;
            s.angleFrom = s.angleTo;
            s.shiftStartMs = 0L;
         }
      }

      double ang = baseAngle + spinOf(s.layer, now);
      double y = s.height * (double)10.0F * grow;
      double r = radiusAt(y) * s.layer.radiusMul;
      if (!(r <= 0.05)) {
         double wave = (double)0.5F + (double)0.5F * Math.sin((double)now / (s.period * (double)1000.0F / (Math.PI * 2D)) + s.phase);
         float twinkle = (float)(0.4 + 0.6 * wave);
         float[] col = starColor(s.color);
         float alpha = Math.min(1.0F, twinkle * boost);
         if (s.sizeClass == 0) {
            cross(buf, pose, r, y, ang, 0.05, col[0], col[1], col[2], alpha * 0.9F);
         } else {
            double rot = (double)now / (double)6000.0F * s.spin;
            if (s.sizeClass == 1) {
               star4(buf, pose, r, y, ang + rot, 0.2, 0.32, col, alpha);
            } else {
               star4(buf, pose, r, y, ang + rot, 0.28, 0.52, col, alpha);
               cross(buf, pose, r, y, ang + rot, 0.34, Math.min(1.0F, col[0] + 0.2F), Math.min(1.0F, col[1] + 0.2F), Math.min(1.0F, col[2] + 0.2F), alpha * 0.32F);
            }

            if (inside) {
               cross(buf, pose, r, y, ang, 0.1, col[0], col[1], col[2], alpha * 0.45F);
            }

         }
      }
   }

   private static void star4(BufferBuilder buf, PoseStack pose, double r, double y, double ang, double shortLen, double longLen, float[] col, float alpha) {
      double dr = (double)1.0F / Math.max((double)0.5F, r);
      line(buf, pose, r, y - shortLen, ang - shortLen * dr, r, y + shortLen, ang + shortLen * dr, col[0], col[1], col[2], alpha);
      line(buf, pose, r, y - longLen, ang - longLen * dr, r, y + longLen, ang + longLen * dr, Math.min(1.0F, col[0] + 0.15F), Math.min(1.0F, col[1] + 0.15F), Math.min(1.0F, col[2] + 0.15F), alpha);
   }

   private static void curvedLine(BufferBuilder buf, PoseStack pose, Star a, Star b, long now, double grow, float[] col, float alpha) {
      double angA = a.angle + spinOf(a.layer, now);
      double angB = b.angle + spinOf(b.layer, now);
      double yA = a.height * (double)10.0F * grow;
      double yB = b.height * (double)10.0F * grow;
      double rA = radiusAt(yA) * a.layer.radiusMul;
      double rB = radiusAt(yB) * b.layer.radiusMul;
      double angM = (angA + angB) * (double)0.5F;
      double yM = (yA + yB) * (double)0.5F + 0.45;
      double rM = radiusAt(yM) * (a.layer.radiusMul + b.layer.radiusMul) * (double)0.5F + 0.35;
      line(buf, pose, rA, yA, angA, rM, yM, angM, col[0], col[1], col[2], alpha * 0.7F);
      line(buf, pose, rM, yM, angM, rB, yB, angB, col[0], col[1], col[2], alpha * 0.7F);
   }

   private static void drawGalaxyBand(BufferBuilder buf, PoseStack pose, long now, double grow, float boost) {
      double spin = (double)(now % 60000L) / (double)60000.0F * Math.PI * (double)2.0F;
      Random rnd = new Random(45069L);

      for(int i = 0; i < 150; ++i) {
         double t = rnd.nextDouble();
         double ang = spin + t * Math.PI * (double)2.0F;
         double tilt = Math.sin(ang) * (double)1.5F + (rnd.nextDouble() - (double)0.5F) * 0.9;
         double y = ((double)4.5F + tilt) * grow;
         if (!(y < 0.2) && !(y > (double)10.5F)) {
            double r = radiusAt(y) * 1.2;
            double edge = Math.cos(t * Math.PI * (double)2.0F);
            float a = (float)(0.35 + 0.45 * ((double)1.0F - Math.abs(edge))) * boost;
            float[] col = edge > (double)0.0F ? new float[]{0.82F, 0.75F, 1.0F} : new float[]{0.7F, 0.82F, 1.0F};
            cross(buf, pose, r, y, ang, 0.045 + rnd.nextDouble() * 0.03, col[0], col[1], col[2], Math.min(1.0F, a));
         }
      }

   }

   private static void drawRunes(BufferBuilder buf, PoseStack pose, long now, double grow, float boost) {
      for(int i = 0; i < 7; ++i) {
         double t = (double)i / (double)7.0F;
         double ang = (double)now / (double)9000.0F * Math.PI * (double)2.0F + t * Math.PI * (double)2.0F;
         double y = (double)10.0F * (0.18 + 0.72 * t) * grow;
         if (!(y > (double)10.0F)) {
            double r = radiusAt(y) * 1.1;
            boolean gold = i % 2 == 0;
            float rr = 1.0F;
            float gg = gold ? 0.9F : 1.0F;
            float bb = gold ? 0.45F : 0.95F;
            float pa = clamp(0.9F * boost * (0.7F + 0.3F * (float)Math.sin((double)now / (double)700.0F + (double)i)));
            double size = 0.3;
            double d = size / Math.max((double)0.5F, r);
            line(buf, pose, r, y - size, ang, r, y, ang + d, rr, gg, bb, pa);
            line(buf, pose, r, y, ang + d, r, y + size, ang, rr, gg, bb, pa);
            line(buf, pose, r, y + size, ang, r, y, ang - d, rr, gg, bb, pa);
            line(buf, pose, r, y, ang - d, r, y - size, ang, rr, gg, bb, pa);
            cross(buf, pose, r, y, ang + (double)now / (double)3000.0F + (double)i, size * 0.6, rr, gg, bb, pa * 0.8F);
         }
      }

   }

   private static void drawBlackHoleBody(BufferBuilder tri, PoseStack pose, Anim anim, long now, double grow, float[] discColor) {
      if (!(grow <= 0.02)) {
         pose.pushPose();
         pose.translate((double)3.0F, (double)8.0F * grow, (double)0.0F);
         double breathe = (double)1.0F + Math.sin((double)now / (double)2600.0F) * 0.03;
         double radius = (double)1.25F * grow * breathe;
         int rings = 24;
         int segs = 36;

         for(int r = 0; r < rings; ++r) {
            double phi0 = Math.PI * (double)r / (double)rings;
            double phi1 = Math.PI * (double)(r + 1) / (double)rings;
            double y0 = Math.cos(phi0) * radius;
            double y1 = Math.cos(phi1) * radius;
            double rad0 = Math.sin(phi0) * radius;
            double rad1 = Math.sin(phi1) * radius;
            float edge = (float)Math.pow((double)r / (double)rings, 2.2);
            float cr = 0.004F + 0.02F * edge;
            float cg = 0.003F + 0.014F * edge;
            float cb = 0.01F + 0.055F * edge;

            for(int s = 0; s < segs; ++s) {
               double t0 = (Math.PI * 2D) * (double)s / (double)segs;
               double t1 = (Math.PI * 2D) * (double)(s + 1) / (double)segs;
               quadZero(tri, pose, rad0, y0, t0, rad0, y0, t1, rad1, y1, t1, rad1, y1, t0, cr, cg, cb, 0.99F);
            }
         }

         double inner = radius * 0.62;

         for(int r = 0; r < rings / 2; ++r) {
            double phi0 = Math.PI * (double)r / (double)(rings / 2);
            double phi1 = Math.PI * (double)(r + 1) / (double)(rings / 2);
            double y0 = Math.cos(phi0) * inner;
            double y1 = Math.cos(phi1) * inner;
            double rad0 = Math.sin(phi0) * inner;
            double rad1 = Math.sin(phi1) * inner;

            for(int s = 0; s < segs; ++s) {
               double t0 = (Math.PI * 2D) * (double)s / (double)segs;
               double t1 = (Math.PI * 2D) * (double)(s + 1) / (double)segs;
               quadZero(tri, pose, rad0, y0, t0, rad0, y0, t1, rad1, y1, t1, rad1, y1, t0, 0.0F, 0.0F, 0.002F, 0.985F);
            }
         }

         double lens = radius * 1.55;

         for(int r = 0; r < rings; ++r) {
            double phi0 = Math.PI * (double)r / (double)rings;
            double phi1 = Math.PI * (double)(r + 1) / (double)rings;
            double y0 = Math.cos(phi0) * lens;
            double y1 = Math.cos(phi1) * lens;
            double rad0 = Math.sin(phi0) * lens;
            double rad1 = Math.sin(phi1) * lens;

            for(int s = 0; s < segs; ++s) {
               double t0 = (Math.PI * 2D) * (double)s / (double)segs;
               double t1 = (Math.PI * 2D) * (double)(s + 1) / (double)segs;
               float aa = (float)(0.3 * ((double)1.0F - (double)r / (double)rings * 0.6));
               quadZero(tri, pose, rad0, y0, t0, rad0, y0, t1, rad1, y1, t1, rad1, y1, t0, 0.04F, 0.03F, 0.09F, aa);
            }
         }

         pose.popPose();
      }
   }

   private static void drawBlackHoleDisc(BufferBuilder buf, PoseStack pose, Anim anim, long now, double grow, float[] discColor) {
      if (!(grow <= 0.02)) {
         pose.pushPose();
         pose.translate((double)3.0F, (double)8.0F * grow, (double)0.0F);
         double breathe = (double)1.0F + Math.sin((double)now / (double)2600.0F) * 0.03;
         double radius = (double)1.25F * grow * breathe;
         double spin = (double)(now % 10000L) / (double)10000.0F * Math.PI * (double)2.0F;
         double tilt = Math.toRadians((double)22.0F);
         int ringSteps = 48;

         for(int i = 0; i < ringSteps; ++i) {
            double a0 = spin * (double)2.0F + (Math.PI * 2D) * (double)i / (double)ringSteps;
            double a1 = spin * (double)2.0F + (Math.PI * 2D) * (double)(i + 1) / (double)ringSteps;

            for(int k = 0; k < 3; ++k) {
               double rr = radius * (1.04 + (double)k * 0.035);
               float aa = 1.0F - (float)k * 0.3F;
               lineZero(buf, pose, rr, Math.sin(a0) * rr * Math.sin(tilt), a0, rr, Math.sin(a1) * rr * Math.sin(tilt), a1, 1.0F, 0.97F, 0.88F, aa);
            }
         }

         for(int i = 0; i < 24; ++i) {
            double a0 = -spin * (double)0.5F + (Math.PI * 2D) * (double)i / (double)24.0F;
            double a1 = -spin * (double)0.5F + (Math.PI * 2D) * (double)(i + 1) / (double)24.0F;
            double rr = radius * (double)1.75F;
            lineZero(buf, pose, rr, Math.sin(a0) * rr * Math.sin(tilt), a0, rr, Math.sin(a1) * rr * Math.sin(tilt), a1, 0.85F, 0.7F, 1.0F, 0.3F);
         }

         double[] radii = new double[]{radius * 1.3, radius * 1.62, radius * (double)2.0F};
         float[][] cols = new float[][]{{1.0F, 0.93F, 0.8F}, {Math.min(1.0F, discColor[0] + 0.25F), Math.min(1.0F, discColor[1] + 0.1F), 0.35F}, {discColor[0] * 0.7F, discColor[1] * 0.45F, 1.0F}};
         float[] alphas = new float[]{1.0F, 0.95F, 0.75F};
         float fed = anim.absorbMs != 0L && now - anim.absorbMs < 1100L ? 1.0F + 0.6F * (1.0F - (float)(now - anim.absorbMs) / 1100.0F) : 1.0F;

         for(int ring = 0; ring < radii.length; ++ring) {
            int steps = 36;
            double speed = (double)1.0F + (double)ring * 0.28;

            for(int i = 0; i < steps; ++i) {
               double a0 = spin * speed + (Math.PI * 2D) * (double)i / (double)steps;
               double a1 = spin * speed + (Math.PI * 2D) * (double)(i + 1) / (double)steps;
               double rr = radii[ring];
               lineZero(buf, pose, rr, Math.sin(a0) * rr * Math.sin(tilt), a0, rr, Math.sin(a1) * rr * Math.sin(tilt), a1, cols[ring][0], cols[ring][1], cols[ring][2], Math.min(1.0F, alphas[ring] * fed));
            }
         }

         for(int halo = 0; halo < 2; ++halo) {
            double rr = radius * (2.6 + (double)halo * 0.55);
            float aa = 0.18F - (float)halo * 0.07F;
            int steps = 40;

            for(int i = 0; i < steps; ++i) {
               double a0 = -spin * 0.6 + (Math.PI * 2D) * (double)i / (double)steps;
               double a1 = -spin * 0.6 + (Math.PI * 2D) * (double)(i + 1) / (double)steps;
               lineZero(buf, pose, rr, Math.sin(a0) * rr * Math.sin(tilt), a0, rr, Math.sin(a1) * rr * Math.sin(tilt), a1, 0.8F, 0.7F, 1.0F, aa);
            }
         }

         for(int i = 0; i < 6; ++i) {
            double tt = ((double)now / (double)4200.0F + (double)i * 0.37) % (double)1.0F;
            double dist = radius * ((double)3.0F - tt * 2.6);
            double ang = spin * 1.8 + (double)i * 1.9 + tt * (double)5.0F;
            double y = Math.sin(ang) * dist * Math.sin(tilt) * 0.6;
            crossZero(buf, pose, dist, y, ang, 0.05 + tt * 0.04, 1.0F, 0.95F, 0.85F, (float)((double)1.0F - tt) * 0.95F);
         }

         double innerR = radius * 1.14;
         double outerR = radius * 2.6;
         int bands = 14;

         for(int b = 0; b < bands; ++b) {
            double rr = innerR + (outerR - innerR) * ((double)b / (double)(bands - 1));
            float t01 = (float)b / (float)(bands - 1);
            float cr = Math.min(1.0F, 1.0F - t01 * 0.35F + discColor[0] * t01 * 0.35F);
            float cg = Math.min(1.0F, 0.95F - t01 * 0.55F + discColor[1] * t01 * 0.55F);
            float cb = Math.min(1.0F, 0.8F + t01 * 0.2F);
            float aa = (1.0F - t01 * 0.55F) * 0.85F * fed;
            int steps = 44;

            for(int i = 0; i < steps; ++i) {
               double a0 = spin * ((double)1.0F + (double)t01 * (double)0.5F) + (Math.PI * 2D) * (double)i / (double)steps;
               double a1 = spin * ((double)1.0F + (double)t01 * (double)0.5F) + (Math.PI * 2D) * (double)(i + 1) / (double)steps;
               lineZero(buf, pose, rr, Math.sin(a0) * rr * Math.sin(tilt), a0, rr, Math.sin(a1) * rr * Math.sin(tilt), a1, cr, cg, cb, aa);
            }
         }

         pose.popPose();
      }
   }

   private static float[] dragonColor(double t) {
      if (t > 0.88) {
         return new float[]{1.0F, 0.88F, 0.5F};
      } else if (t > 0.7) {
         float k = (float)((0.88 - t) / 0.18);
         return new float[]{1.0F - 0.3F * k, 0.88F - 0.2F * k, 0.5F + 0.25F * k};
      } else if (t > 0.45) {
         return new float[]{0.5F, 0.25F, 0.75F};
      } else {
         return t > 0.18 ? new float[]{0.25F, 0.13F, 0.5F} : new float[]{0.13F, 0.06F, 0.25F};
      }
   }

   private static void drawDragon(BufferBuilder buf, PoseStack pose, Anim anim, Vec3 playerPos, long now, double grow, float boost, float[] tint) {
      if (!(grow <= 0.05) && anim.dragonNodes != null) {
         double t0 = (double)(now % 40000L) / (double)40000.0F;
         boolean resting = t0 > 0.88;
         double restK = resting ? (t0 - 0.88) / 0.12 : (double)0.0F;
         double headAngle = t0 * Math.PI * (double)2.0F;
         if (resting) {
            double headHeight = 0.92;
         } else {
            double headHeight = 0.18 + 0.66 * ((double)0.5F + (double)0.5F * Math.sin(t0 * Math.PI * (double)2.0F));
         }

         int n = 24;
         double[] px = anim.dragonNodes[0];
         double[] py = anim.dragonNodes[1];
         double[] pz = anim.dragonNodes[2];

         for(int i = 0; i < n; ++i) {
            double back = (double)i / (double)n;
            double ang = headAngle - back * 1.35;
            double h;
            if (resting) {
               h = 0.92 - back * 0.32 + Math.sin((double)now / (double)900.0F + (double)i * 0.7) * 0.02;
               h -= restK * 0.05;
            } else {
               h = 0.18 + 0.66 * ((double)0.5F + (double)0.5F * Math.sin((t0 - back * 0.1) * Math.PI * (double)2.0F));
            }

            h += Math.sin((double)now / (double)620.0F - (double)i * 0.55) * 0.035;
            if (back > (double)0.75F) {
               ang += Math.sin((double)now / (double)500.0F + back * (double)6.0F) * 0.1 * (back - (double)0.75F) / (double)0.25F;
            }

            h = Math.max(0.05, Math.min(0.98, h));
            double y = h * (double)10.0F * grow;
            double r = radiusAt(y) * (double)1.25F + (double)2.5F;
            px[i] = r * Math.cos(ang);
            py[i] = y;
            pz[i] = r * Math.sin(ang);
         }

         for(int i = 0; i + 1 < n; ++i) {
            double tt = (double)1.0F - (double)i / (double)n;
            float[] c0 = dragonColor((double)1.0F - (double)i / (double)n);
            float[] c1 = dragonColor((double)1.0F - (double)(i + 1) / (double)n);
            double mx = (px[i] + px[i + 1]) * (double)0.5F;
            double my = (py[i] + py[i + 1]) * (double)0.5F + 0.1 + Math.sin((double)now / (double)500.0F + (double)i) * 0.06;
            double mz = (pz[i] + pz[i + 1]) * (double)0.5F;
            float a = 0.5F * (float)(0.35 + 0.65 * tt);
            lineWorld(buf, pose, px[i], py[i], pz[i], mx, my, mz, c0[0], c0[1], c0[2], a);
            lineWorld(buf, pose, mx, my, mz, px[i + 1], py[i + 1], pz[i + 1], c1[0], c1[1], c1[2], a);
         }

         for(int i = 0; i < n; ++i) {
            double tt = (double)1.0F - (double)i / (double)n;
            float[] c = dragonColor(tt);
            double size = 0.06 + 0.16 * tt;
            float bright = (float)(0.3 + 0.7 * tt) * boost;
            crossWorld(buf, pose, px[i], py[i], pz[i], size, c[0], c[1], c[2], bright);
         }

         double tx = px[n - 1];
         double ty = py[n - 1];
         double tz = pz[n - 1];
         double bx = px[n - 3];
         double bz = pz[n - 3];
         double dirX = tx - bx;
         double dirZ = tz - bz;
         double dlen = Math.max(0.01, Math.hypot(dirX, dirZ));
         dirX /= dlen;
         dirZ /= dlen;
         double perpX = -dirZ;
         double perpZ = dirX;

         for(int k = -1; k <= 1; ++k) {
            double sway = Math.sin((double)now / (double)380.0F + (double)k) * 0.18;
            double ex = tx + dirX * (double)1.5F + perpX * (double)k * 0.55 + sway * perpX;
            double ez = tz + dirZ * (double)1.5F + perpZ * (double)k * 0.55 + sway * perpZ;
            double ey = ty + 0.1 * (double)k + Math.sin((double)now / (double)420.0F + (double)k) * 0.12;
            float[] c = dragonColor(0.05);
            lineWorld(buf, pose, tx, ty, tz, ex, ey, ez, c[0], c[1], c[2], 0.65F);
         }

         double holeX = (double)3.0F;
         double holeY = (double)8.0F * grow;
         double emergeT = (double)1.0F;
         if (anim.activateMs != 0L) {
            emergeT = Math.min((double)1.0F, (double)(now - anim.activateMs) / (double)2000.0F);
         }

         double closeT = (double)0.0F;
         if (anim.target == 0.0F) {
            closeT = (double)1.0F - (double)anim.level;
         }

         double blend = Math.max((double)1.0F - emergeT, closeT);
         if (blend > 0.001) {
            for(int i = 0; i < n; ++i) {
               double k = blend * (0.55 + 0.45 * ((double)1.0F - (double)i / (double)n));
               px[i] = px[i] * ((double)1.0F - k) + holeX * k;
               py[i] = py[i] * ((double)1.0F - k) + holeY * k;
               pz[i] *= (double)1.0F - k;
            }
         }

         double hx = px[0];
         double hy = py[0];
         double hz = pz[0];
         float[] headCol = dragonColor((double)1.0F);
         crossWorld(buf, pose, hx, hy, hz, 0.3, headCol[0], headCol[1], headCol[2], 1.0F);
         crossWorld(buf, pose, hx, hy, hz, 0.62, 1.0F, 0.95F, 0.7F, 0.3F);
         crossWorld(buf, pose, hx, hy, hz, 0.95, 1.0F, 0.9F, 0.55F, 0.16F);
         lineWorld(buf, pose, hx - 0.1, hy + 0.22, hz, hx - 0.34, hy + 0.55, hz + 0.1, 1.0F, 0.92F, 0.62F, 0.9F);
         lineWorld(buf, pose, hx + 0.1, hy + 0.22, hz, hx + 0.34, hy + 0.55, hz + 0.1, 1.0F, 0.92F, 0.62F, 0.9F);
         double dx = playerPos.x - hx;
         double dz = playerPos.z - hz;
         double len = Math.max(0.001, Math.hypot(dx, dz));
         double ex = dx / len;
         double ez = dz / len;
         double side = 0.24;
         crossWorld(buf, pose, hx + ex * 0.22 - ez * side, hy + 0.14, hz + ez * 0.22 + ex * side, 0.1, 1.0F, 1.0F, 1.0F, 1.0F);
         crossWorld(buf, pose, hx + ex * 0.22 + ez * side, hy + 0.14, hz + ez * 0.22 - ex * side, 0.1, 1.0F, 1.0F, 1.0F, 1.0F);
         double toHoleX = holeX - hx;
         double toHoleZ = holeZ(anim);
         double holeDist = Math.hypot(toHoleX, toHoleZ);
         if (holeDist < (double)6.0F) {
            double k = (double)1.0F - holeDist / (double)6.0F;

            for(int i = 1; i < n; ++i) {
               double back = (double)i / (double)n;
               py[i] -= k * back * 1.1;
               px[i] += (holeX - px[i]) * k * back * (double)0.25F;
            }

            anim.holeFlashMs = now;
         }

         if (now - anim.lastBreathMs > 10000L + (long)(Math.random() * (double)10000.0F)) {
            anim.lastBreathMs = now;
            anim.breathMs = now;
         }

         if (anim.breathMs != 0L && now - anim.breathMs < 700L) {
            double mouthX = hx + ex * 0.42;
            double mouthZ = hz + ez * 0.42;

            for(int k = 0; k < 6; ++k) {
               double a = (double)(now % 700L) / (double)700.0F;
               double sp = 0.1 + (double)k * 0.05;
               crossWorld(buf, pose, mouthX + ex * sp * a, hy - 0.02 + Math.sin((double)k) * 0.1 * a, mouthZ + ez * sp * a, 0.05 + 0.05 * ((double)1.0F - a), 1.0F, 0.95F, 0.75F, (float)((double)1.0F - a) * 0.9F);
            }
         }

      }
   }

   private static void updateDrift(Anim anim, long now) {
      if (now - anim.lastShiftMs >= 30000L) {
         anim.lastShiftMs = now;
         ++anim.shiftTicks;
         boolean shiftMid = anim.shiftTicks % 2 == 0;

         for(Star s : anim.stars) {
            boolean doIt = s.layer == FormationBarrierRenderer.Layer.NEAR || shiftMid && s.layer == FormationBarrierRenderer.Layer.MID;
            if (doIt) {
               s.angleFrom = s.angle;
               s.angleTo = s.angle + (Math.random() - (double)0.5F) * 1.6;
               s.shiftStartMs = now;
            }
         }

      }
   }

   private static void updateMeteors(Anim anim, long now) {
      if (now - anim.lastMeteorMs > 5000L + (long)(Math.random() * (double)5000.0F)) {
         anim.lastMeteorMs = now;

         for(Meteor m : anim.meteors) {
            if (!m.active) {
               m.active = true;
               m.startMs = now;
               m.lifeMs = 500L + (long)(Math.random() * (double)500.0F);
               m.angle = Math.random() * Math.PI * (double)2.0F;
               m.height = 0.55 + Math.random() * 0.4;
               m.len = (double)3.0F + Math.random() * (double)2.0F;
               m.color = Math.random() < 0.2 ? 2 : (Math.random() < (double)0.5F ? 0 : 1);
               break;
            }
         }
      }

   }

   private static void drawMeteor(BufferBuilder buf, PoseStack pose, Meteor m, double age, double grow) {
      float[] col = m.color == 0 ? new float[]{1.0F, 1.0F, 1.0F} : (m.color == 1 ? new float[]{0.75F, 0.88F, 1.0F} : new float[]{1.0F, 0.88F, 0.55F});
      double y = m.height * (double)10.0F * grow * ((double)1.0F - age * 0.85);
      double r = radiusAt(y) * 1.3;
      double ang = m.angle + age * 0.35;

      for(int i = 0; i < 4; ++i) {
         double t0 = (double)i / (double)4.0F;
         double t1 = (double)(i + 1) / (double)4.0F;
         float a = 0.9F * (1.0F - (float)age) * (1.0F - (float)t0 * 0.75F);
         line(buf, pose, r, y + m.len * t0, ang - 0.1 * t0, r, y + m.len * t1, ang - 0.1 * t1, col[0], col[1], col[2], a);
      }

   }

   private static void spawnDragonDust(ClientLevel level, BlockPos eye, Anim anim, long now) {
      if (anim.dragonNodes != null) {
         double ex = (double)eye.getX() + (double)0.5F;
         double ey = (double)eye.getY() + (double)0.5F;
         double ez = (double)eye.getZ() + (double)0.5F;
         int n = 24;

         for(int i = 0; i < n; i += 2) {
            double tt = (double)1.0F - (double)i / (double)n;
            float[] c = dragonColor(tt);
            double wx = ex + anim.dragonNodes[0][i];
            double wy = ey + anim.dragonNodes[1][i];
            double wz = ez + anim.dragonNodes[2][i];
            double bx = anim.dragonNodes[0][Math.max(0, i - 2)];
            double bz = anim.dragonNodes[2][Math.max(0, i - 2)];
            double vx = (anim.dragonNodes[0][i] - bx) * 0.08;
            double vz = (anim.dragonNodes[2][i] - bz) * 0.08;
            level.addParticle(ParticleTypes.END_ROD, wx, wy, wz, vx, 0.004, vz);
            if (i == 0) {
               level.addParticle(ParticleTypes.FIREWORK, wx, wy, wz, (double)0.0F, (double)0.0F, (double)0.0F);
            }
         }

      }
   }

   private static float starBoostFor(TalismanKind kind) {
      float var10000;
      switch (kind) {
         case CHAOS:
         case SHIFT_BLAME:
            var10000 = 1.0F;
            break;
         case SWORD:
         case ANTIGRAVITY:
         case STAND_IN:
            var10000 = 1.45F;
            break;
         default:
            var10000 = 1.15F;
      }

      return var10000;
   }

   private static float surfaceAlphaFor(TalismanKind kind) {
      // 时缓符：金色屏障，40% 透明度（用户指定）
      if (kind == TalismanKind.TIME_SLOW) {
         return 0.40F;
      }
      // 炼妖符：暗红紫屏障，40% 透明度
      if (kind == TalismanKind.REFINING) {
         return 0.40F;
      }
      float var10000;
      switch (kind) {
         case CHAOS:
         case SHIFT_BLAME:
            var10000 = 0.34F;
            break;
         case SWORD:
         case ANTIGRAVITY:
         case STAND_IN:
            var10000 = 0.15F;
            break;
         default:
            var10000 = 0.24F;
      }

      return var10000;
   }

   private static double holeZ(Anim anim) {
      return (double)0.0F;
   }

   private static void drawBarrier(PoseStack pose, BlockPos eye, TalismanKind kind, Anim anim, Vec3 playerPos, boolean overlay, long now) {
      double cx = (double)eye.getX() + (double)0.5F;
      double cy = (double)eye.getY() + (double)0.5F;
      double cz = (double)eye.getZ() + (double)0.5F;
      double dist = Math.hypot(playerPos.x - cx, playerPos.z - cz);
      boolean inside = dist <= (double)10.0F && Math.abs(playerPos.y - cy) <= (double)10.0F;
      if (inside && !anim.wasInside) {
         anim.rippleMs = now;
      }

      anim.wasInside = inside;
      float near = (float)Math.max((double)0.0F, (double)1.0F - dist / (double)64.0F);
      float boost = 1.0F + 0.45F * near;
      float breathe = 0.85F + 0.15F * (float)Math.sin((double)now / (double)640.0F);
      float ringPulse = inside ? 0.78F + 0.22F * (float)Math.sin((double)now / (double)220.0F) : 1.0F;
      float overlapMul = overlay ? 1.35F : 1.0F;
      float[] c = colorOf(kind);
      double topY = (double)10.0F * (double)anim.level;
      float absorb = 0.0F;
      if (anim.absorbMs != 0L) {
         absorb = Math.min(1.0F, (float)(now - anim.absorbMs) / 1100.0F);
         if (absorb >= 1.0F) {
            anim.absorbMs = 0L;
            anim.level = 0.0F;
            return;
         }
      }

      float absorbEase = absorb * absorb;
      pose.pushPose();
      pose.translate(cx + (double)3.0F * (double)absorbEase, cy + (double)8.0F * (double)absorbEase, cz);
      float shrink = 1.0F - 0.92F * absorbEase;
      pose.scale(shrink, shrink, shrink);
      Tesselator tes = Tesselator.getInstance();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.depthMask(false);
      RenderSystem.disableCull();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      BufferBuilder buf = tes.getBuilder();
      buf.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
      float a = clamp(surfaceAlphaFor(kind) * boost * breathe * overlapMul * (1.0F - absorb));

      for(int seg = 0; seg < 24; ++seg) {
         double y0 = topY * (double)seg / (double)24.0F;
         double y1 = topY * (double)(seg + 1) / (double)24.0F;
         double r0 = radiusAt(y0);
         double r1 = radiusAt(y1);

         for(int lon = 0; lon < 12; ++lon) {
            double t0 = (Math.PI * 2D) * (double)lon / (double)12.0F;
            double t1 = (Math.PI * 2D) * (double)(lon + 1) / (double)12.0F;
            quad(buf, pose, r0, y0, t0, r0, y0, t1, r1, y1, t1, r1, y1, t0, c[0], c[1], c[2], a);
         }
      }

      BufferUploader.drawWithShader(buf.end());

      // ---- 炼妖符：屏障表面一个缓慢旋转的**六芒星**（用户要求）
      if (kind == TalismanKind.REFINING) {
         BufferBuilder star = tes.getBuilder();
         star.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         double spin = (double)now / 7000.0D;
         double yA = topY * 0.30D;
         double yB = topY * 0.72D;
         double rA = radiusAt(yA) * 1.006D;
         double rB = radiusAt(yB) * 1.006D;
         float sa = clamp(a * 2.1F);
         // 两个三角形叠成六芒星：上三角 + 下三角，各三条边
         for (int tri = 0; tri < 2; ++tri) {
            double base = spin + (tri == 0 ? 0.0D : Math.PI / 3.0D);
            for (int i = 0; i < 3; ++i) {
               double t0 = base + Math.PI * 2.0D * (double)i / 3.0D;
               double t1 = base + Math.PI * 2.0D * (double)(i + 1) / 3.0D;
               // 三个顶点交替在上下两个高度上，形成星形
               boolean up0 = (i % 2 == 0);
               boolean up1 = ((i + 1) % 2 == 0);
               double yy0 = up0 ? yB : yA;
               double yy1 = up1 ? yB : yA;
               double rr0 = up0 ? rB : rA;
               double rr1 = up1 ? rB : rA;
               quad(star, pose, rr0, yy0, t0, rr0, yy0, t0 + 0.012D,
                     rr1, yy1, t1 + 0.012D, rr1, yy1, t1,
                     c[0], c[1], c[2], sa);
            }
         }
         BufferUploader.drawWithShader(star.end());
      }

      // ---- 时缓符：屏障表面一圈**缓慢旋转的钟表刻度**（用户要求）
      if (kind == TalismanKind.TIME_SLOW) {
         BufferBuilder dial = tes.getBuilder();
         dial.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         double rot = (double)now / 9000.0D;          // 约 9 秒转一圈
         double yA = topY * 0.42D;
         double yB = topY * 0.52D;
         double rA = radiusAt(yA) * 1.006D;
         double rB = radiusAt(yB) * 1.006D;
         float tickAlpha = clamp(a * 1.9F);
         for (int i = 0; i < 12; ++i) {
            double t = rot + Math.PI * 2.0D * (double)i / 12.0D;
            double w = (i % 3 == 0) ? 0.022D : 0.010D;   // 每 3 格一个长刻度
            quad(dial, pose, rA, yA, t - w, rA, yA, t + w, rB, yB, t + w, rB, yB, t - w,
                  c[0], c[1], c[2], tickAlpha);
         }
         // 一圈细环，把刻度串起来
         for (int i = 0; i < 48; ++i) {
            double t0 = Math.PI * 2.0D * (double)i / 48.0D;
            double t1 = Math.PI * 2.0D * (double)(i + 1) / 48.0D;
            double r = radiusAt(yA) * 1.004D;
            line(dial, pose, r, yA, t0, r, yA, t1, c[0], c[1], c[2], clamp(a * 1.2F));
         }
         BufferUploader.drawWithShader(dial.end());
      }
      RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
      buf = tes.getBuilder();
      buf.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
      float gc0 = Math.min(1.0F, c[0] + 0.1F);
      float gc1 = Math.min(1.0F, c[1] + 0.1F);
      float gc2 = Math.min(1.0F, c[2] + 0.14F);
      float ga = clamp(0.42F * boost * breathe * (1.0F - absorb));

      for(int lon = 0; lon < 12; ++lon) {
         double t = (Math.PI * 2D) * (double)lon / (double)12.0F;

         for(int s = 0; s < 24; ++s) {
            double y0 = topY * (double)s / (double)24.0F;
            double y1 = topY * (double)(s + 1) / (double)24.0F;
            line(buf, pose, radiusAt(y0), y0, t, radiusAt(y1), y1, t, gc0, gc1, gc2, ga);
         }
      }

      for(int p = 1; p <= 3; ++p) {
         double y = (double)10.0F * (double)p / (double)4.0F * (double)anim.level;
         if (!(y > topY)) {
            double r = radiusAt(y);

            for(int lon = 0; lon < 12; ++lon) {
               double t0 = (Math.PI * 2D) * (double)lon / (double)12.0F;
               double t1 = (Math.PI * 2D) * (double)(lon + 1) / (double)12.0F;
               line(buf, pose, r, y, t0, r, y, t1, gc0, gc1, gc2, ga);
            }
         }
      }

      // 滑地阵：地面结一层薄冰（纯视觉覆盖，不改变方块）
      if (kind == TalismanKind.SLIPPERY) {
         double iceY = 0.03D;
         int gridN = 20;
         float iceA = 0.30F * anim.level * (1.0F - absorb);
         double half = RADIUS * anim.level * 0.98D;
         for (int g = 0; g <= gridN; g++) {
            double off = -half + 2.0D * half * ((double) g / gridN);
            lineWorld(buf, pose, -half, iceY, off, half, iceY, off, 0.72F, 0.90F, 1.0F, iceA);
            lineWorld(buf, pose, off, iceY, -half, off, iceY, half, 0.72F, 0.90F, 1.0F, iceA);
         }
      }

      float ra = clamp(0.88F * boost * ringPulse * (1.0F - absorb));

      for(double dy : new double[]{0.02, 0.16}) {
         double r = radiusAt(dy);
         int steps = 48;

         for(int lon = 0; lon < steps; ++lon) {
            double t0 = (Math.PI * 2D) * (double)lon / (double)steps;
            double t1 = (Math.PI * 2D) * (double)(lon + 1) / (double)steps;
            line(buf, pose, r, dy, t0, r, dy, t1, Math.min(1.0F, c[0] + 0.25F), Math.min(1.0F, c[1] + 0.25F), Math.min(1.0F, c[2] + 0.3F), ra);
         }
      }

      if (anim.rippleMs != 0L) {
         double age = (double)(now - anim.rippleMs) / (double)1000.0F;
         if (age >= (double)1.0F) {
            anim.rippleMs = 0L;
         } else {
            float wa = clamp((float)((double)1.0F - age));

            for(int band = 0; band < 3; ++band) {
               double r = (double)10.0F * (0.8 + 0.45 * age) + (double)band * 0.28;
               double y = 0.05 + (double)band * 0.12;
               float ba = wa * (1.0F - (float)band * 0.28F);
               int steps = 96;

               for(int lon = 0; lon < steps; ++lon) {
                  double t0 = (Math.PI * 2D) * (double)lon / (double)steps;
                  double t1 = (Math.PI * 2D) * (double)(lon + 1) / (double)steps;
                  line(buf, pose, r, y, t0, r, y, t1, 1.0F, 1.0F, 0.9F, ba);
               }
            }
         }
      }

      BufferUploader.drawWithShader(buf.end());
      pose.popPose();
   }

   private static void drawDenseStars(BufferBuilder buf, PoseStack pose, Anim anim, long now, double grow, float boost, boolean inside) {
      if (anim.dense != null) {
         double spin = (double)(now % 240000L) / (double)240000.0F * Math.PI * (double)2.0F * 0.35;

         for(DenseStar ds : anim.dense) {
            double ang = ds.angle + spin;
            double y = ds.height * (double)10.0F * grow;
            if (!(y <= 0.05) && !(y > 10.2)) {
               double r = radiusAt(y) * 1.2;
               float[] col = starColor(ds.color);
               float a = Math.min(1.0F, ds.brightness * boost * (inside ? 1.12F : 1.0F));
               if (ds.sizeClass == 0) {
                  line(buf, pose, r, y - 0.015, ang, r, y + 0.015, ang, col[0], col[1], col[2], a);
               } else if (ds.sizeClass == 1) {
                  cross(buf, pose, r, y, ang, 0.06, col[0], col[1], col[2], a);
               } else {
                  star4(buf, pose, r, y, ang + (double)now / (double)9000.0F, 0.16, 0.3, col, a);
                  cross(buf, pose, r, y, ang, 0.22, col[0], col[1], col[2], a * 0.35F);
               }
            }
         }

      }
   }

   private static void drawBandGlow(BufferBuilder tri, PoseStack pose, long now, double grow) {
      if (!(grow <= 0.02)) {
         for(int i = 0; i < 90; ++i) {
            double t = (double)i / (double)90.0F;
            double ang = t * Math.PI * (double)2.0F + (double)now / (double)200000.0F;
            double center = (double)0.5F + 0.3 * Math.sin(ang - 0.6);
            double y0 = (center - 0.1) * (double)10.0F * grow;
            double y1 = (center + 0.1) * (double)10.0F * grow;
            if (!(y1 <= 0.1) && !(y0 >= (double)10.0F)) {
               y0 = Math.max(0.1, y0);
               y1 = Math.min((double)10.0F, y1);
               double a1 = ang + 0.1117010721276371;
               double r0 = radiusAt(y0) * 1.26;
               double r1 = radiusAt(y1) * 1.26;
               float alpha = (float)(0.1 * (0.35 + 0.65 * ((double)1.0F - Math.abs(Math.cos(t * Math.PI)))));
               vTri(tri, pose, r0, y0, ang, 0.35F, 0.3F, 0.7F, alpha);
               vTri(tri, pose, r1, y1, ang, 0.45F, 0.38F, 0.85F, alpha);
               vTri(tri, pose, r0, y0, a1, 0.35F, 0.3F, 0.7F, alpha);
               vTri(tri, pose, r1, y1, ang, 0.45F, 0.38F, 0.85F, alpha);
               vTri(tri, pose, r1, y1, a1, 0.45F, 0.38F, 0.85F, alpha);
               vTri(tri, pose, r0, y0, a1, 0.35F, 0.3F, 0.7F, alpha);
            }
         }

      }
   }

   private static void addHorizonGlow(BufferBuilder tri, PoseStack pose, double radius, float red, float green, float blue, float alpha, boolean top) {
      int rings = 5;
      if (top) {
         boolean var10000 = false;
      } else {
         int var29 = rings - 1;
      }

      for(int seg = 0; seg < rings; ++seg) {
         int idx = top ? seg : rings - 1 - seg;
         double y0 = radius * (double)idx / (double)rings;
         double y1 = radius * (double)(idx + 1) / (double)rings;
         double depth = top ? (double)idx / (double)rings : (double)(rings - idx) / (double)rings;
         if (!(depth > 0.45)) {
            double r0 = Math.sqrt(Math.max((double)0.0F, radius * radius - y0 * y0));
            double r1 = Math.sqrt(Math.max((double)0.0F, radius * radius - y1 * y1));
            float a = alpha * (float)((double)1.0F - depth / 0.45);

            for(int lon = 0; lon < 12; ++lon) {
               double t0 = (Math.PI * 2D) * (double)lon / (double)12.0F;
               double t1 = (Math.PI * 2D) * (double)(lon + 1) / (double)12.0F;
               vTri(tri, pose, r0, y0, t0, red, green, blue, a);
               vTri(tri, pose, r1, y1, t1, red, green, blue, a);
               vTri(tri, pose, r0, y0, t1, red, green, blue, a);
            }
         }
      }

   }

   private static void addDome(BufferBuilder buf, PoseStack pose, double radius, float red, float green, float blue, float alpha, int rings) {
      for(int seg = 0; seg < rings; ++seg) {
         double y0 = radius * (double)seg / (double)rings;
         double y1 = radius * (double)(seg + 1) / (double)rings;
         double r0 = Math.sqrt(Math.max((double)0.0F, radius * radius - y0 * y0));
         double r1 = Math.sqrt(Math.max((double)0.0F, radius * radius - y1 * y1));

         for(int lon = 0; lon < 12; ++lon) {
            double t0 = (Math.PI * 2D) * (double)lon / (double)12.0F;
            double t1 = (Math.PI * 2D) * (double)(lon + 1) / (double)12.0F;
            vTri(buf, pose, r0, y0, t0, red, green, blue, alpha);
            vTri(buf, pose, r1, y1, t1, red, green, blue, alpha);
            vTri(buf, pose, r0, y0, t1, red, green, blue, alpha);
         }
      }

   }

   private static void addBlob(BufferBuilder buf, PoseStack pose, double r, double y, double ang, double size, double[] radii, float[] col, float alpha) {
      int n = radii.length;

      for(int i = 0; i < n; ++i) {
         int j = (i + 1) % n;
         double a0 = ang + (Math.PI * 2D) * (double)i / (double)n;
         double a1 = ang + (Math.PI * 2D) * (double)j / (double)n;
         double s0 = size * radii[i] * (double)0.5F;
         double s1 = size * radii[j] * (double)0.5F;
         vTri(buf, pose, r, y, a0, col[0], col[1], col[2], alpha * 0.15F);
         vTri(buf, pose, r + s0, y + (radii[i] - 0.9) * size * 0.4, a0, col[0], col[1], col[2], alpha);
         vTri(buf, pose, r + s1, y + (radii[j] - 0.9) * size * 0.4, a1, col[0], col[1], col[2], alpha);
      }

   }

   private static void vTri(BufferBuilder buf, PoseStack pose, double r, double y, double angle, float red, float green, float blue, float alpha) {
      buf.vertex(pose.last().pose(), (float)(r * Math.cos(angle)), (float)y, (float)(r * Math.sin(angle))).color(red, green, blue, alpha).endVertex();
   }

   private static float clamp(float v) {
      return Math.max(0.0F, Math.min(1.0F, v));
   }

   private static double radiusAt(double y) {
      double r2 = (double)100.0F - y * y;
      return r2 <= (double)0.0F ? (double)0.0F : Math.sqrt(r2);
   }

   private static void quad(BufferBuilder buf, PoseStack pose, double r0, double y0, double t0, double r1, double y1, double t1, double r2, double y2, double t2, double r3, double y3, double t3, float red, float green, float blue, float alpha) {
      vTri(buf, pose, r0, y0, t0, red, green, blue, alpha);
      vTri(buf, pose, r1, y1, t1, red, green, blue, alpha);
      vTri(buf, pose, r2, y2, t2, red, green, blue, alpha);
      vTri(buf, pose, r0, y0, t0, red, green, blue, alpha);
      vTri(buf, pose, r2, y2, t2, red, green, blue, alpha);
      vTri(buf, pose, r3, y3, t3, red, green, blue, alpha);
   }

   private static void line(BufferBuilder buf, PoseStack pose, double ra, double ya, double ta, double rb, double yb, double tb, float red, float green, float blue, float alpha) {
      Matrix4f m = pose.last().pose();
      buf.vertex(m, (float)(ra * Math.cos(ta)), (float)ya, (float)(ra * Math.sin(ta))).color(red, green, blue, alpha).normal(0.0F, 1.0F, 0.0F).endVertex();
      buf.vertex(m, (float)(rb * Math.cos(tb)), (float)yb, (float)(rb * Math.sin(tb))).color(red, green, blue, alpha).normal(0.0F, 1.0F, 0.0F).endVertex();
   }

   private static void cross(BufferBuilder buf, PoseStack pose, double r, double y, double angle, double size, float red, float green, float blue, float alpha) {
      line(buf, pose, r, y - size, angle, r, y + size, angle, red, green, blue, alpha);
      double d = size / Math.max((double)0.5F, r);
      line(buf, pose, r, y, angle - d, r, y, angle + d, red, green, blue, alpha);
   }

   private static float[] discColorOf(TalismanKind kind) {
      float[] var10000;
      switch (kind) {
         case CHAOS -> var10000 = new float[]{0.72F, 0.3F, 1.0F};
         case SWORD -> var10000 = new float[]{0.25F, 0.9F, 0.95F};
         case ANTIGRAVITY -> var10000 = new float[]{0.45F, 0.75F, 1.0F};
         case STAND_IN -> var10000 = new float[]{1.0F, 0.72F, 0.15F};
         case SHIFT_BLAME -> var10000 = new float[]{0.15F, 0.9F, 0.4F};
          case SUMMON -> var10000 = new float[]{0.3F, 0.95F, 0.9F};
          case SLIPPERY -> var10000 = new float[]{0.65F, 0.88F, 1.0F};
          case BLIND -> var10000 = new float[]{0.40F, 0.40F, 0.52F};
          case LOOP -> var10000 = new float[]{0.45F, 0.40F, 0.95F};
          case DISTANCE -> var10000 = new float[]{0.85F, 0.65F, 1.0F};
         default -> var10000 = new float[]{1.0F, 0.55F, 0.25F};
      }

      return var10000;
   }

   private static void vTriWorld(BufferBuilder buf, PoseStack pose, double x, double y, double z, float red, float green, float blue, float alpha) {
      buf.vertex(pose.last().pose(), (float)x, (float)y, (float)z).color(red, green, blue, alpha).endVertex();
   }

   private static void lineWorld(BufferBuilder buf, PoseStack pose, double x1, double y1, double z1, double x2, double y2, double z2, float red, float green, float blue, float alpha) {
      Matrix4f m = pose.last().pose();
      buf.vertex(m, (float)x1, (float)y1, (float)z1).color(red, green, blue, alpha).normal(0.0F, 1.0F, 0.0F).endVertex();
      buf.vertex(m, (float)x2, (float)y2, (float)z2).color(red, green, blue, alpha).normal(0.0F, 1.0F, 0.0F).endVertex();
   }

   private static void crossWorld(BufferBuilder buf, PoseStack pose, double x, double y, double z, double size, float red, float green, float blue, float alpha) {
      lineWorld(buf, pose, x - size, y, z, x + size, y, z, red, green, blue, alpha);
      lineWorld(buf, pose, x, y - size, z, x, y + size, z, red, green, blue, alpha);
      lineWorld(buf, pose, x, y, z - size, x, y, z + size, red, green, blue, alpha);
   }

   private static void quadZero(BufferBuilder buf, PoseStack pose, double r0, double y0, double t0, double r1, double y1, double t1, double r2, double y2, double t2, double r3, double y3, double t3, float red, float green, float blue, float alpha) {
      vTri(buf, pose, r0, y0, t0, red, green, blue, alpha);
      vTri(buf, pose, r1, y1, t1, red, green, blue, alpha);
      vTri(buf, pose, r2, y2, t2, red, green, blue, alpha);
      vTri(buf, pose, r0, y0, t0, red, green, blue, alpha);
      vTri(buf, pose, r2, y2, t2, red, green, blue, alpha);
      vTri(buf, pose, r3, y3, t3, red, green, blue, alpha);
   }

   private static void crossZero(BufferBuilder buf, PoseStack pose, double r, double y, double ang, double size, float red, float green, float blue, float alpha) {
      double dr = size / Math.max((double)0.5F, r);
      line(buf, pose, r, y - size, ang, r, y + size, ang, red, green, blue, alpha);
      line(buf, pose, r, y, ang - dr, r, y, ang + dr, red, green, blue, alpha);
      line(buf, pose, r - size, y, ang, r + size, y, ang, red, green, blue, alpha);
   }

   private static void lineZero(BufferBuilder buf, PoseStack pose, double ra, double ya, double ta, double rb, double yb, double tb, float red, float green, float blue, float alpha) {
      line(buf, pose, ra, ya, ta, rb, yb, tb, red, green, blue, alpha);
   }

   private static void playAmbientSounds(ClientLevel level, BlockPos eye, Anim anim, long now, TalismanKind kind) {
      if (anim.hadTalisman) {
         double cx = (double)eye.getX() + (double)0.5F;
         double cy = (double)eye.getY() + (double)0.5F;
         double cz = (double)eye.getZ() + (double)0.5F;
         if (now - anim.lastRumbleMs > 45000L) {
            anim.lastRumbleMs = now;
            level.playLocalSound(cx + (double)3.0F, cy + (double)8.0F, cz, SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 1.6F, 0.55F, false);
         }

         if (now - anim.lastGrowlMs > 30000L + (long)(Math.random() * (double)30000.0F)) {
            anim.lastGrowlMs = now;
            level.playLocalSound(cx, cy + (double)9.0F, cz, SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.AMBIENT, 0.9F, 0.6F, false);
         }

      }
   }

   private static void rescanEyes(ClientLevel level, Vec3 playerPos) {
      if (--scanCooldown <= 0) {
         scanCooldown = 10;
         List<BlockPos> previous = new ArrayList(EYES);
         EYES.clear();
         int radiusChunks = (int)Math.ceil((double)4.0F);
         int cx = (int)playerPos.x >> 4;
         int cz = (int)playerPos.z >> 4;

         for(int dx = -radiusChunks; dx <= radiusChunks; ++dx) {
            for(int dz = -radiusChunks; dz <= radiusChunks; ++dz) {
               LevelChunk chunk = level.getChunkSource().getChunk(cx + dx, cz + dz, false);
               if (chunk != null) {
                  for(BlockPos pos : chunk.getBlockEntities().keySet()) {
                     BlockState state = level.getBlockState(pos);
                     if (state.getBlock() instanceof FormationEyeBlock && pos.distToCenterSqr(playerPos) <= (double)4096.0F) {
                        EYES.add(pos.immutable());
                     }
                  }
               }
            }
         }

         for(BlockPos old : previous) {
            if (!EYES.contains(old)) {
               Anim anim = (Anim)ANIMS.get(old);
               if (anim != null && anim.level > 0.05F) {
                  shatter(level, old);
                  anim.absorbMs = System.currentTimeMillis();
               }
            }
         }

      }
   }

   private static TalismanKind talismanAt(ClientLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return !(state.getBlock() instanceof FormationEyeBlock) ? TalismanKind.EMPTY : (TalismanKind)state.getValue(FormationEyeBlock.TALISMAN);
   }

   private static boolean hasOverlap(ClientLevel level, BlockPos eye) {
      for(BlockPos other : EYES) {
         if (!other.equals(eye) && talismanAt(level, other) != TalismanKind.EMPTY && other.distSqr(eye) <= (double)400.0F) {
            return true;
         }
      }

      return false;
   }

   private static void shatter(ClientLevel level, BlockPos eye) {
      double x = (double)eye.getX() + (double)0.5F;
      double y = (double)eye.getY() + (double)0.5F;
      double z = (double)eye.getZ() + (double)0.5F;

      for(int i = 0; i < 40; ++i) {
         double a = level.random.nextDouble() * Math.PI * (double)2.0F;
         double r = (double)10.0F * (0.4 + 0.6 * level.random.nextDouble());
         level.addParticle(ParticleTypes.END_ROD, x + Math.cos(a) * r, y + (double)2.0F * level.random.nextDouble(), z + Math.sin(a) * r, (level.random.nextDouble() - (double)0.5F) * 0.3, -0.05, (level.random.nextDouble() - (double)0.5F) * 0.3);
      }

      for(int i = 0; i < 30; ++i) {
         double a = level.random.nextDouble() * Math.PI * (double)2.0F;
         double sp = (double)0.5F + level.random.nextDouble() * 1.2;
         level.addParticle(ParticleTypes.FIREWORK, x, y + (double)7.0F, z, Math.cos(a) * sp * 0.35, -0.35 - level.random.nextDouble() * 0.4, Math.sin(a) * sp * 0.35);
      }

      level.playLocalSound(x, y, z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 0.9F, false);
      level.playLocalSound(x, y, z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 1.5F, false);
   }

   private static enum Layer {
      NEAR(1.06, (double)1.0F),
      MID(1.16, 0.62),
      FAR(1.3, 0.34);

      final double radiusMul;
      final double spinMul;

      private Layer(double radiusMul, double spinMul) {
         this.radiusMul = radiusMul;
         this.spinMul = spinMul;
      }

      // $FF: synthetic method
      private static Layer[] $values() {
         return new Layer[]{NEAR, MID, FAR};
      }
   }

   private static final class Star {
      Layer layer;
      double angle;
      double angleFrom;
      double angleTo;
      long shiftStartMs;
      double height;
      double phase;
      double period;
      int sizeClass;
      int color;
      double spin;
   }

   private static final class DenseStar {
      double angle;
      double height;
      float brightness;
      int sizeClass;
      int color;
   }

   private static final class Nebula {
      Layer layer;
      double angle;
      double height;
      double size;
      double[] radii;
      int color;
   }

   private static final class Constellation {
      int[] starIndex;
      int color;
      double phase;
   }

   private static final class Meteor {
      long startMs;
      long lifeMs;
      double angle;
      double height;
      double len;
      int color;
      boolean active;
   }

   private static final class Anim {
      float level;
      float target;
      long lastMs = System.currentTimeMillis();
      boolean hadTalisman;
      boolean wasInside;
      long rippleMs;
      boolean built;
      Star[] stars;
      DenseStar[] dense;
      Nebula[] nebulae;
      Constellation[] constellations;
      Meteor[] meteors;
      long lastMeteorMs;
      long lastRumbleMs;
      long lastGrowlMs;
      double[][] dragonNodes = new double[3][24];
      long lastBreathMs;
      long breathMs;
      long holeOrbitMs;
      long holeFlashMs;
      long activateMs;
      long absorbMs;
      TalismanKind lastKind;
      boolean dragonAroundHole;
      long dragonAroundStartMs;
      long lastShiftMs;
      int shiftTicks;
      double holePhase;

      private Anim() {
         this.lastKind = TalismanKind.EMPTY;
         this.lastShiftMs = System.currentTimeMillis();
      }
   }
}
