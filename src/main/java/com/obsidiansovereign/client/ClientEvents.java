package com.obsidiansovereign.client;

import com.obsidiansovereign.ObsidianSovereignMod;
import com.obsidiansovereign.registry.ModEntities;
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
}
