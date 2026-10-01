package com.obsidiansovereign.client;

import com.obsidiansovereign.ObsidianSovereignMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class ModModelLayers {
    public static final ModelLayerLocation SOVEREIGN_FORCE_FIELD =
            new ModelLayerLocation(new ResourceLocation(ObsidianSovereignMod.MODID, "obsidian_sovereign"), "force_field");
    public static final ModelLayerLocation SOVEREIGN_CAPE =
            new ModelLayerLocation(new ResourceLocation(ObsidianSovereignMod.MODID, "obsidian_sovereign"), "cape");
}
