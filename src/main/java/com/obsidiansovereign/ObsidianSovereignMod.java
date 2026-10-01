package com.obsidiansovereign;

import com.obsidiansovereign.registry.ModCreativeTabs;
import com.obsidiansovereign.registry.ModEntities;
import com.obsidiansovereign.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ObsidianSovereignMod.MODID)
public class ObsidianSovereignMod {
    public static final String MODID = "obsidiansovereign";

    public ObsidianSovereignMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        // Event handlers in the event/ and client/ packages register themselves via @Mod.EventBusSubscriber.
    }
}
