package com.obsidiansovereign.registry;

import com.obsidiansovereign.ObsidianSovereignMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ObsidianSovereignMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.obsidiansovereign"))
            .icon(() -> new ItemStack(ModItems.SOVEREIGN_EMBLEM.get()))
            .displayItems((params, output) -> {
                output.accept(ModItems.SOVEREIGN_SIGIL.get());
                output.accept(ModItems.SOVEREIGN_SCEPTER.get());
                output.accept(ModItems.SOVEREIGN_RUNE_SHARD.get());
                output.accept(ModItems.SOVEREIGN_HELMET.get());
                output.accept(ModItems.SOVEREIGN_CHESTPLATE.get());
                output.accept(ModItems.SOVEREIGN_LEGGINGS.get());
                output.accept(ModItems.SOVEREIGN_BOOTS.get());
                output.accept(ModItems.OBSIDIAN_SOVEREIGN_SPAWN_EGG.get());
                output.accept(ModItems.RUNEBOUND_SOLDIER_SPAWN_EGG.get());
            })
            .build());
}
