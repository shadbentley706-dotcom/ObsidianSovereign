package com.obsidiansovereign.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.obsidiansovereign.ObsidianSovereignMod;
import com.obsidiansovereign.entity.ObsidianSovereignEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** A royal cape that swings with movement and billows hard while he flies. */
public class SovereignCapeLayer extends RenderLayer<ObsidianSovereignEntity, HumanoidModel<ObsidianSovereignEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ObsidianSovereignMod.MODID, "textures/entity/sovereign_cape.png");
    private final ModelPart cape;

    public SovereignCapeLayer(RenderLayerParent<ObsidianSovereignEntity, HumanoidModel<ObsidianSovereignEntity>> parent,
                              EntityModelSet models) {
        super(parent);
        this.cape = models.bakeLayer(ModModelLayers.SOVEREIGN_CAPE).getChild("cape");
    }

    public static LayerDefinition createCapeLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5.0F, 0.0F, -1.0F, 10.0F, 16.0F, 1.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, ObsidianSovereignEntity e,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (e.isInvisible()) return;

        // "Lag" = opposite of movement, like the vanilla player cape
        double lagX = e.xo - e.getX();
        double lagY = e.yo - e.getY();
        double lagZ = e.zo - e.getZ();
        float bodyYaw = Mth.rotLerp(partialTick, e.yBodyRotO, e.yBodyRot);
        double sin = Mth.sin(bodyYaw * ((float) Math.PI / 180F));
        double cos = -Mth.cos(bodyYaw * ((float) Math.PI / 180F));

        float lift = Mth.clamp((float) (lagY * 10.0D), -6.0F, 32.0F);
        float swing = Mth.clamp((float) ((lagX * sin + lagZ * cos) * 100.0D), 0.0F, 150.0F);
        float side = Mth.clamp((float) ((lagX * cos - lagZ * sin) * 100.0D), -20.0F, 20.0F);

        boolean flying = e.isNoGravity();
        float flutter = Mth.sin(ageInTicks * (flying ? 0.35F : 0.12F)) * (flying ? 8.0F : 3.0F);
        if (flying) swing = Math.max(swing, 35.0F);

        pose.pushPose();
        pose.translate(0.0F, 0.0F, 0.125F);
        pose.mulPose(Axis.XP.rotationDegrees(6.0F + swing / 2.0F + lift + flutter));
        pose.mulPose(Axis.ZP.rotationDegrees(side / 2.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - side / 2.0F));
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.cape.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
