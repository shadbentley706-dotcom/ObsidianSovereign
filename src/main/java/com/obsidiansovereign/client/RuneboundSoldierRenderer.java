package com.obsidiansovereign.client;

import com.obsidiansovereign.ObsidianSovereignMod;
import com.obsidiansovereign.entity.RuneboundSoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RuneboundSoldierRenderer extends HumanoidMobRenderer<RuneboundSoldierEntity, HumanoidModel<RuneboundSoldierEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ObsidianSovereignMod.MODID, "textures/entity/runebound_soldier.png");
    private static final ResourceLocation GLOW =
            new ResourceLocation(ObsidianSovereignMod.MODID, "textures/entity/runebound_soldier_glow.png");

    public RuneboundSoldierRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        this.addLayer(new GlowLayer<>(this, GLOW));
    }

    @Override
    public ResourceLocation getTextureLocation(RuneboundSoldierEntity entity) {
        return TEXTURE;
    }
}
