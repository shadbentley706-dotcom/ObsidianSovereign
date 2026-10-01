package com.obsidiansovereign.registry;

import com.obsidiansovereign.ObsidianSovereignMod;
import com.obsidiansovereign.entity.ArcaneBoltEntity;
import com.obsidiansovereign.entity.ObsidianSovereignEntity;
import com.obsidiansovereign.entity.RuneboundSoldierEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ObsidianSovereignMod.MODID);

    public static final RegistryObject<EntityType<ObsidianSovereignEntity>> OBSIDIAN_SOVEREIGN =
            ENTITIES.register("obsidian_sovereign", () -> EntityType.Builder
                    .<ObsidianSovereignEntity>of(ObsidianSovereignEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.85F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("obsidian_sovereign"));

    public static final RegistryObject<EntityType<RuneboundSoldierEntity>> RUNEBOUND_SOLDIER =
            ENTITIES.register("runebound_soldier", () -> EntityType.Builder
                    .<RuneboundSoldierEntity>of(RuneboundSoldierEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("runebound_soldier"));

    public static final RegistryObject<EntityType<ArcaneBoltEntity>> ARCANE_BOLT =
            ENTITIES.register("arcane_bolt", () -> EntityType.Builder
                    .<ArcaneBoltEntity>of(ArcaneBoltEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("arcane_bolt"));
}
