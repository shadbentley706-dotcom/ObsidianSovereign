package com.obsidiansovereign.client;

import com.obsidiansovereign.ObsidianSovereignMod;
import com.obsidiansovereign.registry.ModEntities;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ObsidianSovereignMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.OBSIDIAN_SOVEREIGN.get(), ObsidianSovereignRenderer::new);
        event.registerEntityRenderer(ModEntities.RUNEBOUND_SOLDIER.get(), RuneboundSoldierRenderer::new);
        event.registerEntityRenderer(ModEntities.ARCANE_BOLT.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.25F, true));
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.SOVEREIGN_FORCE_FIELD,
                () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.0F), 0.0F), 64, 64));
        event.registerLayerDefinition(ModModelLayers.SOVEREIGN_CAPE, SovereignCapeLayer::createCapeLayer);
    }
}
