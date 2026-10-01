package com.obsidiansovereign.event;

import com.obsidiansovereign.ObsidianSovereignMod;
import com.obsidiansovereign.entity.ObsidianSovereignEntity;
import com.obsidiansovereign.entity.RuneboundSoldierEntity;
import com.obsidiansovereign.registry.ModEntities;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ObsidianSovereignMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonModEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.OBSIDIAN_SOVEREIGN.get(), ObsidianSovereignEntity.createAttributes().build());
        event.put(ModEntities.RUNEBOUND_SOLDIER.get(), RuneboundSoldierEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.OBSIDIAN_SOVEREIGN.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ObsidianSovereignEntity::checkSovereignSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
