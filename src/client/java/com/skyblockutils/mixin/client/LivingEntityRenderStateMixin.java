package com.skyblockutils.mixin.client;

import com.skyblockutils.utils.SSURenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements SSURenderState {
    @Unique
    private boolean ssu$bigHead;

    @Override
    public boolean ssu$isBigHead() {
        return ssu$bigHead;
    }

    @Override
    public void ssu$setBigHead(boolean bigHead) {
        ssu$bigHead = bigHead;
    }
}
