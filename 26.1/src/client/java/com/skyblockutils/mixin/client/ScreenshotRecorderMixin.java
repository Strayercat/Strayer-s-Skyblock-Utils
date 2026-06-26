package com.skyblockutils.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.skyblockutils.features.hud.ScreenshotManager;
import net.minecraft.client.Screenshot;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Consumer;

@Mixin(Screenshot.class)
public class ScreenshotRecorderMixin {

    @ModifyArg(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Screenshot;takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V"
            ),
            method = "grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V",
            index = 2
    )
    private static Consumer<NativeImage> captureImage(Consumer<NativeImage> original) {
        String filename = Util.getFilenameFormattedDateTime() + ".png";
        return image -> {
            NativeImage copy = new NativeImage(image.getWidth(), image.getHeight(), false);
            copy.copyFrom(image);
            ScreenshotManager.addScreenshot(copy, filename);
            original.accept(image);
        };
    }
}