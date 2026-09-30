package com.skyblockutils.mixin.client;

import com.skyblockutils.StrayersSkyblockUtilsClient;
import com.skyblockutils.render.EggDetector;
import com.skyblockutils.render.SkullOwnerAccess;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkullBlockEntity.class)
public abstract class SkullBlockEntityMixin implements SkullOwnerAccess {
    @Shadow
    private @Nullable ResolvableProfile owner;

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void ssu$checkEgg(ValueInput input, CallbackInfo ci) {
        if (StrayersSkyblockUtilsClient.isOnHypixel) EggDetector.check((SkullBlockEntity) (Object) this);
    }

    @Override
    public void ssu$setOwner(@Nullable ResolvableProfile owner) {
        this.owner = owner;
    }
}
