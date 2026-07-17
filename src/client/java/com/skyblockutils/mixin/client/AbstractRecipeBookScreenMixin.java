package com.skyblockutils.mixin.client;

import com.skyblockutils.StrayersSkyblockUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractRecipeBookScreen.class)
public class AbstractRecipeBookScreenMixin {

    @Redirect(
            at = @At(
                    value = "NEW",
                    target = "(IIIILnet/minecraft/client/gui/components/WidgetSprites;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/ImageButton;"
            ),
            method = "initButton"
    )
    private ImageButton skyblockutils$redirectRecipeButton(
            int x, int y, int width, int height, WidgetSprites sprites, Button.OnPress onPress
    ) {
        Button.OnPress customOnPress = button -> {
            if (StrayersSkyblockUtilsClient.isInSkyblock) {
                Minecraft client = Minecraft.getInstance();
                if (client.getConnection() == null || client.gui.screen() == null) return;

                client.gui.screen().clearFocus();
                client.getConnection().sendCommand("recipebook");
                return;
            }

            onPress.onPress(button);
        };

        return new ImageButton(x, y, width, height, sprites, customOnPress);
    }
}