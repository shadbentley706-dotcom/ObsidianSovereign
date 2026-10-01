package com.obsidiansovereign.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Full-bright emissive layer, so eyes and runes glow in the dark (like Enderman eyes). */
public class GlowLayer<T extends Mob, M extends HumanoidModel<T>> extends EyesLayer<T, M> {
    private final RenderType renderType;

    public GlowLayer(RenderLayerParent<T, M> parent, ResourceLocation texture) {
        super(parent);
        this.renderType = RenderType.eyes(texture);
    }

    @Override
    public RenderType renderType() {
        return renderType;
    }
}
