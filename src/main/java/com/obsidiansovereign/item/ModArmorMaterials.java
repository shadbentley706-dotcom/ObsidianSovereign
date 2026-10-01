package com.obsidiansovereign.item;

import com.obsidiansovereign.registry.ModItems;
import net.minecraft.Util;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.function.Supplier;

public enum ModArmorMaterials implements ArmorMaterial {
    // Slightly above netherite: tougher, more durable, a bit of knockback resistance.
    SOVEREIGN("obsidiansovereign:sovereign", 40,
            Util.make(new EnumMap<ArmorItem.Type, Integer>(ArmorItem.Type.class), m -> {
                m.put(ArmorItem.Type.BOOTS, 3);
                m.put(ArmorItem.Type.LEGGINGS, 6);
                m.put(ArmorItem.Type.CHESTPLATE, 8);
                m.put(ArmorItem.Type.HELMET, 3);
            }),
            20, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.5F, 0.15F,
            () -> Ingredient.of(ModItems.SOVEREIGN_RUNE_SHARD.get()));

    private static final EnumMap<ArmorItem.Type, Integer> BASE_DURABILITY =
            Util.make(new EnumMap<ArmorItem.Type, Integer>(ArmorItem.Type.class), m -> {
                m.put(ArmorItem.Type.BOOTS, 13);
                m.put(ArmorItem.Type.LEGGINGS, 15);
                m.put(ArmorItem.Type.CHESTPLATE, 16);
                m.put(ArmorItem.Type.HELMET, 11);
            });

    private final String name;
    private final int durabilityMultiplier;
    private final EnumMap<ArmorItem.Type, Integer> defense;
    private final int enchantability;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repair;

    ModArmorMaterials(String name, int durabilityMultiplier, EnumMap<ArmorItem.Type, Integer> defense,
                      int enchantability, SoundEvent equipSound, float toughness, float knockbackResistance,
                      Supplier<Ingredient> repair) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defense = defense;
        this.enchantability = enchantability;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repair = repair;
    }

    @Override public int getDurabilityForType(ArmorItem.Type type) { return BASE_DURABILITY.get(type) * durabilityMultiplier; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return defense.get(type); }
    @Override public int getEnchantmentValue() { return enchantability; }
    @Override public SoundEvent getEquipSound() { return equipSound; }
    @Override public Ingredient getRepairIngredient() { return repair.get(); }
    @Override public String getName() { return name; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockbackResistance; }
}
