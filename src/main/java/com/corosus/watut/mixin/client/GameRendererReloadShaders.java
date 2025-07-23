package com.corosus.watut.mixin.client;

import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.ShaderInstanceBlur;
import com.corosus.watut.WatutMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Mixin(GameRenderer.class)
public abstract class GameRendererReloadShaders {

    @Shadow
    @Final
    private Map<String, ShaderInstance> shaders;

    @Inject(method = "reloadShaders", at = @At("RETURN"))
    private void onLoadShaders(ResourceProvider resourceProvider, CallbackInfo ci) {
        PlayerStatusManagerClient.particle = null;
        PlayerStatusManagerClient.positionTexBlur = null;
        PlayerStatusManagerClient.positionTexBlurHorizontal = null;
        PlayerStatusManagerClient.positionTexBlurVertical = null;

        loadShaderSafe("particle", DefaultVertexFormat.PARTICLE, resourceProvider,
                shader -> PlayerStatusManagerClient.particle = shader, "粒子");
        loadShaderSafe("position_tex_blur", DefaultVertexFormat.POSITION_TEX, resourceProvider,
                shader -> PlayerStatusManagerClient.positionTexBlur = shader, "模糊");
        loadShaderSafe("position_tex_blur_horizontal", DefaultVertexFormat.POSITION_TEX, resourceProvider,
                shader -> PlayerStatusManagerClient.positionTexBlurHorizontal = shader, "水平模糊");
        loadShaderSafe("position_tex_blur_vertical", DefaultVertexFormat.POSITION_TEX, resourceProvider,
                shader -> PlayerStatusManagerClient.positionTexBlurVertical = shader, "垂直模糊");
    }

    /**
     * 安全加载 shader，如果缺失不崩溃，只警告
     */
    private void loadShaderSafe(String name, DefaultVertexFormat format, ResourceProvider resourceProvider,
                               java.util.function.Consumer<ShaderInstanceBlur> setter, String desc) {
        try {
            ShaderInstanceBlur shader = new ShaderInstanceBlur(getResourceFactory(resourceProvider), name, format);
            if (shader != null) {
                setter.accept(shader);
                shaders.put(shader.getName(), shader);
            }
        } catch (IOException e) {
            System.err.println("[Watut] 警告：" + desc + " shader (" + name + ") 缺失，已跳过，不影响游戏启动。");
        } catch (Throwable t) {
            System.err.println("[Watut] 警告：" + desc + " shader (" + name + ") 加载异常：" + t);
        }
    }

    private static ResourceProvider getResourceFactory(ResourceProvider resourceManager) {
        return new ResourceProvider() {
            @Override
            public Optional<Resource> getResource(ResourceLocation resourceLocation) {
                ResourceLocation corrected = ResourceLocation.fromNamespaceAndPath(
                        WatutMod.MODID, resourceLocation.getPath());
                return resourceManager.getResource(corrected);
            }
        };
    }
}
