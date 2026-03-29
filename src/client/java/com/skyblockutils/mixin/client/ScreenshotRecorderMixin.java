package com.skyblockutils.mixin.client;

import com.skyblockutils.features.hud.ScreenshotManager;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Consumer;

@Mixin(ScreenshotRecorder.class)
public class ScreenshotRecorderMixin {
    @ModifyArg(
            method = "saveScreenshot(Ljava/io/File;Ljava/lang/String;Lnet/minecraft/client/gl/Framebuffer;ILjava/util/function/Consumer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/ScreenshotRecorder;takeScreenshot(Lnet/minecraft/client/gl/Framebuffer;ILjava/util/function/Consumer;)V"),
            index = 2
    )
    private static Consumer<NativeImage> captureImage(Consumer<NativeImage> original) {
        String filename = net.minecraft.util.Util.getFormattedCurrentTime() + ".png";
        return image -> {
            ScreenshotManager.addScreenshot(image, filename);
            original.accept(image);
        };
    }
}
