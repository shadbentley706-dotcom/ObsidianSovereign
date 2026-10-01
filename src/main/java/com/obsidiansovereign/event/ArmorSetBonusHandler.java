package com.obsidiansovereign.event;

import com.obsidiansovereign.ObsidianSovereignMod;
import com.obsidiansovereign.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Full Sovereign set bonus:
 *  - Passive: Resistance I + Fire Resistance.
 *  - Rune Ward: when a hit would drop you below 50% health, the hit is halved and you gain
 *    Absorption III for 10s. 60 second cooldown.
 */
@Mod.EventBusSubscriber(modid = ObsidianSovereignMod.MODID)
public class ArmorSetBonusHandler {
    private static final String WARD_READY_KEY = "obsidiansovereign_ward_ready_at";
    private static final int WARD_COOLDOWN_TICKS = 1200;

    public static boolean hasFullSet(LivingEntity e) {
        return e.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SOVEREIGN_HELMET.get())
                && e.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.SOVEREIGN_CHESTPLATE.get())
                && e.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.SOVEREIGN_LEGGINGS.get())
                && e.getItemBySlot(EquipmentSlot.FEET).is(ModItems.SOVEREIGN_BOOTS.get());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide || player.tickCount % 40 != 0 || !hasFullSet(player)) return;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false, true));
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        if (!hasFullSet(player)) return;
        if (player.getHealth() - event.getAmount() > player.getMaxHealth() * 0.5F) return;

        long now = player.level().getGameTime();
        CompoundTag data = player.getPersistentData();
        if (now < data.getLong(WARD_READY_KEY)) return;
        data.putLong(WARD_READY_KEY, now + WARD_COOLDOWN_TICKS);

        event.setAmount(event.getAmount() * 0.5F);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 2));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0F, 0.6F);
        if (player.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(),
                    50, 0.6D, 1.0D, 0.6D, 0.1D);
        }
        player.displayClientMessage(Component.translatable("message.obsidiansovereign.rune_ward")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }
}
