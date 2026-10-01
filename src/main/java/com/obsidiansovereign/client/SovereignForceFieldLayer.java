package com.obsidiansovereign.client;

import com.obsidiansovereign.ObsidianSovereignMod;
import com.obsidiansovereign.entity.ObsidianSovereignEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.resources.ResourceLocation;

/** Swirling purple energy shell (like a charged creeper) shown while a force field or shield is up. */
public class SovereignForceFieldLayer extends EnergySwirlLayer<ObsidianSovereignEntity, HumanoidModel<ObsidianSovereignEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ObsidianSovereignMod.MODID, "textures/entity/sovereign_force_field.png");
    private final HumanoidModel<ObsidianSovereignEntity> model;

    public SovereignForceFieldLayer(RenderLayerParent<ObsidianSovereignEntity, HumanoidModel<ObsidianSovereignEntity>> parent,
                                    EntityModelSet models) {
        super(parent);
        this.model = new HumanoidModel<>(models.bakeLayer(ModModelLayers.SOVEREIGN_FORCE_FIELD));
    }

    @Override protected float xOffset(float tick) { return tick * 0.012F; }
    @Override protected ResourceLocation getTextureLocation() { return TEXTURE; }
    @Override protected EntityModel<ObsidianSovereignEntity> model() { return this.model; }
}
