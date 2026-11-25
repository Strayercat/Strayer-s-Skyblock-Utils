package com.skyblockutils.mixin.client;

import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ModelManager.class)
public interface ModelManagerAccessor {
    @Accessor("atlasManager")
    AtlasManager getAtlasManager();

    @Accessor("bakedItemStackModels")
    Map<Identifier, ItemModel> getBakedItemStackModels();
}
