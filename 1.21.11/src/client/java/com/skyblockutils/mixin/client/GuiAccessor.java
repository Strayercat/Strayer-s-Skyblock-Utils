package com.skyblockutils.mixin.client;

import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InGameHud.class)
public interface GuiAccessor {
    @Accessor("titleFadeInTicks")
    void setTitleFadeInTime(int time);

    @Accessor("titleStayTicks")
    void setTitleStayTime(int time);

    @Accessor("titleFadeOutTicks")
    void setTitleFadeOutTime(int time);
}