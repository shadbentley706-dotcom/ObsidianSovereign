package com.obsidiansovereign.registry;

import com.obsidiansovereign.ObsidianSovereignMod;
import com.obsidiansovereign.item.ModArmorMaterials;
import com.obsidiansovereign.item.SovereignArmorItem;
import com.obsidiansovereign.item.SovereignScepterItem;
import com.obsidiansovereign.item.SovereignSigilItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ObsidianSovereignMod.MODID);

    public static final RegistryObject<Item> SOVEREIGN_RUNE_SHARD = ITEMS.register("sovereign_rune_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> SOVEREIGN_SCEPTER = ITEMS.register("sovereign_scepter",
            () -> new SovereignScepterItem(new Item.Properties().durability(500).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> SOVEREIGN_SIGIL = ITEMS.register("sovereign_sigil",
            () -> new SovereignSigilItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> SOVEREIGN_HELMET = ITEMS.register("sovereign_helmet",
            () -> new SovereignArmorItem(ModArmorMaterials.SOVEREIGN, ArmorItem.Type.HELMET, armorProps()));
    public static final RegistryObject<Item> SOVEREIGN_CHESTPLATE = ITEMS.register("sovereign_chestplate",
            () -> new SovereignArmorItem(ModArmorMaterials.SOVEREIGN, ArmorItem.Type.CHESTPLATE, armorProps()));
    public static final RegistryObject<Item> SOVEREIGN_LEGGINGS = ITEMS.register("sovereign_leggings",
            () -> new SovereignArmorItem(ModArmorMaterials.SOVEREIGN, ArmorItem.Type.LEGGINGS, armorProps()));
    public static final RegistryObject<Item> SOVEREIGN_BOOTS = ITEMS.register("sovereign_boots",
            () -> new SovereignArmorItem(ModArmorMaterials.SOVEREIGN, ArmorItem.Type.BOOTS, armorProps()));

    public static final RegistryObject<Item> OBSIDIAN_SOVEREIGN_SPAWN_EGG = ITEMS.register("obsidian_sovereign_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.OBSIDIAN_SOVEREIGN, 0x1A1026, 0xA860FF, new Item.Properties()));
    public static final RegistryObject<Item> RUNEBOUND_SOLDIER_SPAWN_EGG = ITEMS.register("runebound_soldier_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.RUNEBOUND_SOLDIER, 0x3A3846, 0x9A5CE6, new Item.Properties()));

    private static Item.Properties armorProps() {
        return new Item.Properties().rarity(Rarity.EPIC).fireResistant();
    }
}
