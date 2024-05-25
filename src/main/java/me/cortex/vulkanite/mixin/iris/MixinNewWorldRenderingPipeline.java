package me.cortex.vulkanite.mixin.iris;

import me.cortex.vulkanite.client.Vulkanite;
import me.cortex.vulkanite.client.rendering.VulkanPipeline;
import me.cortex.vulkanite.compat.IGetRaytracingSource;
import me.cortex.vulkanite.compat.IRenderTargetVkGetter;
import me.cortex.vulkanite.compat.RaytracingShaderSet;
import me.cortex.vulkanite.lib.base.VContext;
import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.irisshaders.iris.pipeline.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.shaderpack.ProgramSet;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IrisRenderingPipeline.class, remap = false)
public class MixinIrisRenderingPipeline {
    @Shadow @Final private RenderTargets renderTargets;
    @Unique private RaytracingShaderSet[] rtShaderPasses = null;
    @Unique private VContext ctx;
    @Unique private VulkanPipeline pipeline;


    @Inject(method = "<init>", at = @At("TAIL"))
    private void injectRTShader(ProgramSet set, CallbackInfo ci) {
        ctx = Vulkanite.INSTANCE.getCtx();
        var passes = ((IGetRaytracingSource)set).getRaytracingSource();
        if (passes != null) {
            rtShaderPasses = new RaytracingShaderSet[passes.length];
            for (int i = 0; i < passes.length; i++) {
                rtShaderPasses[i] = new RaytracingShaderSet(ctx, passes[i]);
            }
            pipeline = new VulkanPipeline(ctx, Vulkanite.INSTANCE.getAccelerationManager(), rtShaderPasses);
        }
    }

    @Inject(method = "renderShadows", at = @At("TAIL"))
    private void renderShadows(LevelRendererAccessor par1, Camera par2, CallbackInfo ci) {
        pipeline.renderPostShadows(((IRenderTargetVkGetter)renderTargets.get(0)).getMain(), par2);
    }

    @Inject(method = "destroyShaders", at = @At("TAIL"))
    private void destory(CallbackInfo ci) {
        if (rtShaderPasses != null) {
            ctx.cmd.waitQueueIdle(0);
            for (var pass : rtShaderPasses) {
                pass.delete();
            }
            pipeline.destory();
            rtShaderPasses = null;
            pipeline = null;
        }
    }
}
