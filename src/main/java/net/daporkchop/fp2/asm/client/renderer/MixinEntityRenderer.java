/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.fp2.asm.client.renderer;

import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.client.gl.MatrixHelper;
import net.daporkchop.fp2.strategy.common.IFarContext;
import net.daporkchop.fp2.strategy.common.IFarRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author DaPorkchop_
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    private float farPlaneDistance;

    @Inject(method = "Lnet/minecraft/client/renderer/EntityRenderer;renderWorldPass(IFJ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V",
                    ordinal = 5,
                    shift = At.Shift.BEFORE))
    private void renderWorldPass_postRenderBelowClouds(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        IFarRenderer renderer = ((IFarContext) this.mc.world).renderer();
        if (renderer != null) {
            this.mc.profiler.endStartSection("fp2_render");
            renderer.render(partialTicks, this.mc.world, this.mc);
        }
    }

    //use a projection with infinite zFar

    @Redirect(method = "Lnet/minecraft/client/renderer/EntityRenderer;renderCloudsCheck(Lnet/minecraft/client/renderer/RenderGlobal;FIDDD)V",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V"))
    private void renderCloudsCheck_dontUseGluPerspective(float fov, float aspect, float zNear, float zFar) {
        MatrixHelper.infiniteZFar(fov, aspect, zNear);
    }

    @Redirect(method = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand(FI)V",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V"))
    private void renderHand_dontUseGluPerspective(float fov, float aspect, float zNear, float zFar) {
        MatrixHelper.infiniteZFar(fov, aspect, zNear);
    }

    @Redirect(method = "Lnet/minecraft/client/renderer/EntityRenderer;renderWorldPass(IFJ)V",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V"))
    private void renderWorldPass_dontUseGluPerspective(float fov, float aspect, float zNear, float zFar) {
        MatrixHelper.infiniteZFar(fov, aspect, zNear);
    }

    @Redirect(method = "Lnet/minecraft/client/renderer/EntityRenderer;setupCameraTransform(FI)V",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V"))
    private void setupCameraTransform_dontUseGluPerspective(float fov, float aspect, float zNear, float zFar) {
        MatrixHelper.infiniteZFar(fov, aspect, zNear);
    }

    //set farPlaneDistance to the value in config

    @Redirect(method = "Lnet/minecraft/client/renderer/EntityRenderer;setupCameraTransform(FI)V",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/EntityRenderer;farPlaneDistance:F",
                    opcode = Opcodes.PUTFIELD))
    private void setupCameraTransform_increaseFarPlaneDistance(EntityRenderer renderer, float farPlaneDistance) {
        if (((IFarContext) this.mc.world).isInitialized()) {
            //farPlaneDistance = FP2Config.renderDistance;
            farPlaneDistance = FP2Config.levelCutoffDistance << FP2Config.maxLevels >> 1;
            //TODO: i need a better system for computing this
        }
        this.farPlaneDistance = farPlaneDistance;
    }
}
