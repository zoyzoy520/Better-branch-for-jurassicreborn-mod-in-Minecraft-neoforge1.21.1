package net.example.jrtameall.mixin;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.TabulaModel;
import com.github.alexthe666.citadel.client.model.TabulaModelRenderUtils;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.player.Player;
import net.vit.jurassicreborn.client.render.entity.DinosaurRenderer;
import net.vit.jurassicreborn.common.entities.DinosaurEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.example.jrtameall.RidingSeatCache;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Measures the dinosaur model's true top at render time and caches it per
 * entity. The seat height then uses this measurement instead of static
 * tabula JSON geometry, whose base rotations are crouched/leg-raised poses
 * that do not match what is rendered.
 *
 * Injection point: DinosaurRenderer.scale() RETURN. At that moment the
 * PoseStack already contains the full render transform (entity translate,
 * flip, JR scale/offset), and the only remaining transform before
 * renderToBuffer is the constant translate(0, -1.501, 0), which we subtract
 * by hand. The animator pose is one frame old (setupAnim runs after scale);
 * consecutive-frame error is negligible. Walking the box tree with
 * Matrix4f.transformY gives true world Y of every corner - no unit
 * conversion needed.
 *
 * Measurement runs only while a player is riding, so the per-frame cost is
 * bounded. Cache lives in a concurrent map (see RidingSeatCache) because
 * render (client thread) and getPassengerRidingPosition (client tick) run
 * on different threads.
 */
@Mixin(DinosaurRenderer.class)
public abstract class DinosaurRendererRidingMixin {

    /** Vanilla's constant model sink applied after scale() in LivingEntityRenderer.render. */
    @Unique
    private static final float MODEL_SINK_Y = 1.501F;

    /** Reusable destination for Matrix4f.transform (single-threaded render thread). */
    @Unique
    private static final Vector4f DEST = new Vector4f();

    @Inject(method = "scale(Lnet/vit/jurassicreborn/common/entities/DinosaurEntity;Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
            at = @At("RETURN"))
    private void jr_tame_all$measureAtScale(DinosaurEntity entity, PoseStack pose, float partialTick,
                                            CallbackInfo ci) {
        if (!(entity.getControllingPassenger() instanceof Player)) {
            return;
        }
        // The model field lives on the parent LivingEntityRenderer (protected);
        // access it through the public getModel().
        if (((LivingEntityRenderer<?, ?>) (Object) this).getModel() instanceof TabulaModel tabula) {
            double[] out = {-Double.MAX_VALUE, Double.MAX_VALUE};
            double[] wtTop = {0.0D};
            double[] volSum = {0.0D};
            String[] topName = {"?"};
            float[] topRpY = {0.0F};
            float[] topY2 = {0.0F};
            float[] topOffY = {0.0F};
            float[] rootOffY = {0.0F};
            String[] scaleInfo = {""};
            float[] rootRpY = {0.0F};
            float[] rootMin = {Float.MAX_VALUE};
            float[] rootMax = {-Float.MAX_VALUE};
            String[] rootCube = {""};
            float camY = (float) Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y;
            Matrix4f m = new Matrix4f(pose.last().pose());
            for (AdvancedModelBox root : ((TabulaModelAccessor) tabula).jr_tame_all$getRootBoxes()) {
                jr_tame_all$walk(root, m, out, wtTop, volSum, topName, topRpY, topY2, topOffY, scaleInfo);
                rootRpY[0] = root.rotationPointY;
                rootOffY[0] = root.offsetY;
                for (TabulaModelRenderUtils.ModelBox mb : root.cubeList) {
                    for (int i = 0; i < 8; i++) {
                        float wy = m.transform(mb.posX1 + (i & 1) * (mb.posX2 - mb.posX1),
                                mb.posY1 + ((i >> 1) & 1) * (mb.posY2 - mb.posY1),
                                mb.posZ1 + ((i >> 2) & 1) * (mb.posZ2 - mb.posZ1), 1.0F, DEST).y;
                        rootMin[0] = Math.min(rootMin[0], wy);
                        rootMax[0] = Math.max(rootMax[0], wy);
                    }
                    rootCube[0] = "y1=" + String.format("%.1f", mb.posY1)
                            + " y2=" + String.format("%.1f", mb.posY2)
                            + " z1=" + String.format("%.1f", mb.posZ1)
                            + " z2=" + String.format("%.1f", mb.posZ2)
                            + " x1=" + String.format("%.1f", mb.posX1);
                }
            }
            // transform() output is relative to the camera; add camY, apply
            // the model sink, subtract the entity's feet -> height above feet.
            double relMax = out[0] + camY - MODEL_SINK_Y - entity.getY();
            double relMin = out[1] + camY - MODEL_SINK_Y - entity.getY();
            double relBack = (wtTop[0] / volSum[0]) + camY - MODEL_SINK_Y - entity.getY();
            RidingSeatCache.put(entity.getId(), relMax, relBack);
            if (entity.tickCount % 200 == 0) {
                System.out.println("[JRSEAT] " + entity.getType().builtInRegistryHolder().key().location()
                        + " relMax=" + String.format("%.2f", relMax)
                        + " relMin=" + String.format("%.2f", relMin)
                        + " relBack=" + String.format("%.2f", relBack)
                        + " offY=" + String.format("%.2f", entity.getDinosaur().getOffsetY())
                        + " entityY=" + String.format("%.1f", entity.getY())
                        + " bbH=" + String.format("%.2f", entity.getBbHeight())
                        + " topName=" + topName[0] + " topRpY=" + String.format("%.1f", topRpY[0])
                        + " topY2=" + String.format("%.1f", topY2[0])
                        + " rootRpY=" + String.format("%.1f", rootRpY[0])
                        + " topOffY=" + String.format("%.1f", topOffY[0])
                        + " rootOffY=" + String.format("%.1f", rootOffY[0])
                        + " m00=" + String.format("%.2f", m.m00()) + " m11=" + String.format("%.2f", m.m11())
                        + scaleInfo[0]);
            }
        }
    }

    /**
     * Walk the model part tree (one-frame-old animator pose), replicating
     * citadel's render chain per part:
     *   translate(rotationPoint/16) -> rotateZ -> rotateY -> rotateX -> scale
     * (see AdvancedModelBox.translateAndRotate) and box corners /16 (see
     * ModelBox doRender), all multiplied on top of the root render matrix.
     * Tracks the highest corner (model top, includes raised heads/tails/
     * limbs) and the volume-weighted mean top of all boxes (the back):
     * the torso dominates the weighting, while thin heads/tails/legs
     * contribute almost nothing. Results are camera-relative Y.
     */
    @Unique
    private static void jr_tame_all$walk(AdvancedModelBox box, Matrix4f parent, double[] out,
                                         double[] wtTop, double[] volSum, String[] topName,
                                         float[] topRpY, float[] topY2, float[] topOffY, String[] scaleInfo) {
        Matrix4f local = new Matrix4f(parent);
        local.translate(box.rotationPointX / 16.0F, box.rotationPointY / 16.0F, box.rotationPointZ / 16.0F);
        // rotateAngle fields are radians; citadel applies Z, then Y, then X.
        local.rotateZ(box.rotateAngleZ);
        local.rotateY(box.rotateAngleY);
        local.rotateX(box.rotateAngleX);
        if (box.scaleX != 1.0F || box.scaleY != 1.0F || box.scaleZ != 1.0F) {
            local.scale(box.scaleX, box.scaleY, box.scaleZ);
            if (scaleInfo[0].length() < 200) {
                scaleInfo[0] += "," + (box.boxName == null ? "?" : box.boxName)
                        + "(" + String.format("%.1f", box.scaleX) + "/"
                        + String.format("%.1f", box.scaleY) + "/"
                        + String.format("%.1f", box.scaleZ) + ")";
            }
        }
        for (TabulaModelRenderUtils.ModelBox mb : box.cubeList) {
            float[] xs = {mb.posX1 / 16.0F, mb.posX2 / 16.0F};
            float[] ys = {mb.posY1 / 16.0F, mb.posY2 / 16.0F};
            float[] zs = {mb.posZ1 / 16.0F, mb.posZ2 / 16.0F};
            float boxTop = -Float.MAX_VALUE;
            for (int i = 0; i < 8; i++) {
                float worldY = local.transform(xs[i & 1], ys[(i >> 1) & 1], zs[(i >> 2) & 1], 1.0F, DEST).y;
                if (worldY > out[0]) {
                    out[0] = worldY;
                    topName[0] = box.boxName == null ? "?" : box.boxName;
                    topRpY[0] = box.rotationPointY;
                    topY2[0] = mb.posY2;
                    topOffY[0] = box.offsetY;
                }
                out[1] = Math.min(out[1], worldY);
                if (worldY > boxTop) {
                    boxTop = worldY;
                }
            }
            double vol = (double) (mb.posX2 - mb.posX1) * (mb.posY2 - mb.posY1) * (mb.posZ2 - mb.posZ1);
            wtTop[0] += vol * boxTop;
            volSum[0] += vol;
        }
        for (BasicModelPart child : box.childModels) {
            if (child instanceof AdvancedModelBox advanced) {
                jr_tame_all$walk(advanced, local, out, wtTop, volSum, topName, topRpY, topY2, topOffY, scaleInfo);
            }
        }
    }
}
