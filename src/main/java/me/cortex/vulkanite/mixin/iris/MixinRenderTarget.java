package me.cortex.vulkanite.mixin.iris;

import me.cortex.vulkanite.client.Vulkanite;
import me.cortex.vulkanite.compat.IRenderTargetVkGetter;
import me.cortex.vulkanite.lib.memory.VGImage;
import net.irisshaders.iris.gl.texture.InternalTextureFormat;
import net.irisshaders.iris.gl.texture.PixelFormat;
import net.irisshaders.iris.rendertarget.RenderTarget;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.vulkan.VK10.*;

@Mixin(value = RenderTarget.class, remap = false)
public abstract class MixinRenderTarget implements IRenderTargetVkGetter {
    @Shadow @Final private PixelFormat format;
    @Shadow @Final private InternalTextureFormat internalFormat;

    @Shadow protected abstract void setupTexture(int i, int i1, int i2, boolean b);

    @Unique private VGImage vgMainTexture;
    @Unique private VGImage vgAltTexture;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_genTextures([I)V"))
    private void redirectGen(int[] textures) {

    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/irisshaders/iris/rendertarget/RenderTarget;setupTexture(IIIZ)V", ordinal = 0))
    private void redirectMain(RenderTarget instance, int id, int width, int height, boolean allowsLinear) {
        setupTextures(width, height, allowsLinear);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/irisshaders/iris/rendertarget/RenderTarget;setupTexture(IIIZ)V", ordinal = 1))
    private void redirectAlt(RenderTarget instance, int id, int width, int height, boolean allowsLinear) {}

    @Redirect(method = "setupTexture", at = @At(value = "INVOKE",target = "Lnet/irisshaders/iris/rendertarget/RenderTarget;resizeTexture(III)V"))
    private void redirectResize(RenderTarget instance, int t, int w, int h) {}

    @Overwrite
    public int getMainTexture() {
        return vgMainTexture.glId;
    }

    @Overwrite
    public int getAltTexture() {
        return vgAltTexture.glId;
    }

    @Overwrite
    public void resize(int width, int height) {
        glFinish();
        //TODO: block the gpu fully before deleting and resizing the textures
        vgMainTexture.free();
        vgAltTexture.free();

        setupTextures(width, height, !internalFormat.getPixelFormat().isInteger());
    }

    private void setupTextures(int width, int height, boolean allowsLinear) {
        var ctx = Vulkanite.INSTANCE.getCtx();
        int glfmt = internalFormat.getGlFormat();
        glfmt = (glfmt==GL_RGBA)?GL_RGBA8:glfmt;
        int vkfmt = gl2vkFormat(glfmt);
        vgMainTexture = ctx.memory.createSharedImage(width, height, 1, vkfmt, glfmt, VK_IMAGE_USAGE_STORAGE_BIT , VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
        setupTexture(getMainTexture(), width, height, allowsLinear);

        vgAltTexture = ctx.memory.createSharedImage(width, height, 1, vkfmt, glfmt, VK_IMAGE_USAGE_STORAGE_BIT , VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
        setupTexture(getAltTexture(), width, height, allowsLinear);
    }


    private int gl2vkFormat(int gl) {
        return switch (gl) {
            case GL_R11F_G11F_B10F -> VK_FORMAT_B10G11R11_UFLOAT_PACK32;
            case GL_RGBA16 -> VK_FORMAT_R16G16B16A16_UNORM;
            case GL_RGB32F, GL_RGBA32F -> VK_FORMAT_R32G32B32A32_SFLOAT;
            case GL_RGB8, GL_RGBA8 -> VK_FORMAT_R8G8B8A8_UNORM;
            case GL_R16F -> VK_FORMAT_R16_SFLOAT;
            default -> {throw new IllegalArgumentException("Unknown gl2vk type: "+internalFormat+" -> "+internalFormat.getGlFormat());}
        };
    }

    @Redirect(method = "destroy", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_deleteTextures([I)V"))
    private void redirectResize(int[] textures) {
        glFinish();
        //TODO: block the gpu fully before deleting and resizing the textures
        vgMainTexture.free();
        vgAltTexture.free();
    }

    public VGImage getMain() {
        return vgMainTexture;
    }

    public VGImage getAlt() {
        return vgAltTexture;
    }
}
