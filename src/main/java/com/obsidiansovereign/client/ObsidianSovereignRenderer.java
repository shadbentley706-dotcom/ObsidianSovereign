package com.obsidiansovereign.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.obsidiansovereign.ObsidianSovereignMod;
import com.obsidiansovereign.entity.ObsidianSovereignEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ObsidianSovereignRenderer extends HumanoidMobRenderer<ObsidianSovereignEntity, HumanoidModel<ObsidianSovereignEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ObsidianSovereignMod.MODID, "textures/entity/obsidian_sovereign.png");
    private static final ResourceLocation GLOW =
            new ResourceLocation(ObsidianSovereignMod.MODID, "textures/entity/obsidian_sovereign_glow.png");
    private static final float SCALE = 1.45F;

    public ObsidianSovereignRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F * SCALE);
        this.addLayer(new SovereignCapeLayer(this, ctx.getModelSet()));
        this.addLayer(new GlowLayer<>(this, GLOW));
        this.addLayer(new SovereignForceFieldLayer(this, ctx.getModelSet()));
    }

    @Override
    protected void scale(ObsidianSovereignEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(ObsidianSovereignEntity entity) {
        return TEXTURE;
    }
}
