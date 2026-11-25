package com.skyblockutils.mixin.client;

import com.skyblockutils.features.inventory.InventoryButtons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "me.owdding.catharsis.features.gui.modifications.elements.GuiSpriteElement", remap = false)
public class CatharsisSpriteElementMixin {
    @Unique
    private static Field ssu$x, ssu$y, ssu$width, ssu$height;
    @Unique
    private static Method ssu$calculate;
    @Unique
    private static boolean ssu$failed;

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void ssu$hideOverButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, ScreenRectangle bounds, CallbackInfo ci) {
        if (ssu$failed || !InventoryButtons.active() || !(Minecraft.getInstance().gui.screen() instanceof InventoryScreen)) return;
        try {
            if (ssu$calculate == null) {
                Class<?> type = this.getClass();
                ssu$x = ssu$field(type, "x");
                ssu$y = ssu$field(type, "y");
                ssu$width = ssu$field(type, "width");
                ssu$height = ssu$field(type, "height");
                ssu$calculate = ssu$x.getType().getMethod("calculate", int.class, int.class);
            }
            int x = (int) ssu$calculate.invoke(ssu$x.get(this), bounds.left(), bounds.width());
            int y = (int) ssu$calculate.invoke(ssu$y.get(this), bounds.top(), bounds.height());
            Integer width = (Integer) ssu$width.get(this);
            Integer height = (Integer) ssu$height.get(this);
            int w = width == null ? bounds.width() : width;
            int h = height == null ? bounds.height() : height;
            if (InventoryButtons.coversMostly(bounds.left(), bounds.top(), x, y, w, h)) ci.cancel();
        } catch (ReflectiveOperationException | RuntimeException e) {
            ssu$failed = true;
        }
    }

    @Unique
    private static Field ssu$field(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
