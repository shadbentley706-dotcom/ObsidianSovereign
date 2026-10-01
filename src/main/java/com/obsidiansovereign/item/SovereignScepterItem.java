package com.obsidiansovereign.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.obsidiansovereign.entity.ArcaneBoltEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Sovereign's Scepter.
 *  - Tap use:            Arcane Bolt (fast, 14 dmg).
 *  - Hold, then release: Sovereign Beam - charge up to 1.5s for a piercing 48-block beam (up to 50 dmg).
 *  - Sneak + use:        Void Step - blink-dash up to 12 blocks, slashing everything you pass through.
 *  - Melee:              hits like a netherite sword-and-a-half.
 */
public class SovereignScepterItem extends Item {
    private static final int QUICK_CAST_TICKS = 6;
    private static final int MAX_CHARGE_TICKS = 30;
    private static final double BEAM_RANGE = 48.0D;
    private static final double DASH_RANGE = 12.0D;

    private final Multimap<Attribute, AttributeModifier> modifiers;

    public SovereignScepterItem(Properties properties) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 9.0D, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.4D, AttributeModifier.Operation.ADDITION));
        this.modifiers = b.build();
    }

    // ---------------------------------------------------------------- Use

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level instanceof ServerLevel server && voidStep(server, player)) {
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                player.getCooldowns().addCooldown(this, 25);
                player.awardStat(Stats.ITEM_USED.get(this));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.SPEAR; }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        int used = getUseDuration(stack) - remaining;
        float charge = Math.min(1.0F, used / (float) MAX_CHARGE_TICKS);
        if (level.isClientSide) {
            // Energy gathering in front of the caster
            Vec3 c = entity.getEyePosition().add(entity.getLookAngle().scale(1.2D)).add(0.0D, -0.3D, 0.0D);
            RandomSource r = entity.getRandom();
            int n = 1 + (int) (charge * 4);
            for (int i = 0; i < n; i++) {
                level.addParticle(ParticleTypes.PORTAL, c.x, c.y, c.z,
                        (r.nextDouble() - 0.5D) * 3.0D, (r.nextDouble() - 0.5D) * 3.0D, (r.nextDouble() - 0.5D) * 3.0D);
            }
            if (charge >= 1.0F) {
                level.addParticle(ParticleTypes.END_ROD, c.x + (r.nextDouble() - 0.5D) * 0.6D,
                        c.y + (r.nextDouble() - 0.5D) * 0.6D, c.z + (r.nextDouble() - 0.5D) * 0.6D, 0.0D, 0.0D, 0.0D);
            }
        } else if (used == MAX_CHARGE_TICKS) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.8F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        int used = getUseDuration(stack) - timeLeft;
        if (level instanceof ServerLevel server) {
            if (used < QUICK_CAST_TICKS) {
                fireBolt(server, player);
                player.getCooldowns().addCooldown(this, 8);
            } else {
                float charge = Math.min(1.0F, (used - QUICK_CAST_TICKS) / (float) (MAX_CHARGE_TICKS - QUICK_CAST_TICKS));
                fireBeam(server, player, charge);
                player.getCooldowns().addCooldown(this, 20 + (int) (30 * charge));
            }
            InteractionHand hand = player.getUsedItemHand();
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    // ---------------------------------------------------------------- Abilities

    private void fireBolt(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle();
        ArcaneBoltEntity bolt = new ArcaneBoltEntity(level, player, look.x, look.y, look.z);
        bolt.setPos(player.getX() + look.x, player.getEyeY() - 0.1D + look.y, player.getZ() + look.z);
        bolt.setDamage(14.0F);
        level.addFreshEntity(bolt);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.4F);
    }

    private void fireBeam(ServerLevel level, Player player, float charge) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(BEAM_RANGE));
        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 hit = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

        double width = 0.6D + charge * 0.9D;
        float damage = 12.0F + 38.0F * charge;

        // Damage everything the beam passes through
        AABB area = new AABB(start, hit).inflate(width + 1.0D);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area,
                t -> t != player && t.isAlive() && !t.isSpectator() && !player.isAlliedTo(t))) {
            if (e.getBoundingBox().inflate(width).clip(start, hit).isPresent()) {
                if (e.hurt(player.damageSources().indirectMagic(player, player), damage)) {
                    e.knockback(0.8D + charge, -look.x, -look.z);
                    e.hurtMarked = true;
                }
            }
        }

        // Visuals: white-hot core with a twin purple spiral
        Vec3 path = hit.subtract(start);
        double len = path.length();
        Vec3 dir = path.normalize();
        Vec3 right = dir.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-4D) right = new Vec3(1.0D, 0.0D, 0.0D);
        right = right.normalize();
        Vec3 up = right.cross(dir).normalize();
        double radius = 0.25D + charge * 0.45D;

        for (double d = 1.0D; d < len; d += 0.35D) {
            Vec3 p = start.add(dir.scale(d)).add(0.0D, -0.2D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            for (int k = 0; k < 2; k++) {
                double a = d * 1.4D + k * Math.PI;
                Vec3 o = right.scale(Math.cos(a) * radius).add(up.scale(Math.sin(a) * radius));
                level.sendParticles(ParticleTypes.DRAGON_BREATH, p.x + o.x, p.y + o.y, p.z + o.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        if (charge >= 1.0F) {
            for (double d = 2.0D; d < len; d += 3.0D) {
                Vec3 p = start.add(dir.scale(d)).add(0.0D, -0.2D, 0.0D);
                level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, hit.x, hit.y, hit.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        } else {
            level.sendParticles(ParticleTypes.EXPLOSION, hit.x, hit.y, hit.z, 2, 0.3D, 0.3D, 0.3D, 0.0D);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5F, 1.3F - 0.4F * charge);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6F + charge, 1.2F);
    }

    private boolean voidStep(ServerLevel level, Player player) {
        Vec3 start = player.position();
        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, Mth_clamp(look.y, -0.2D, 0.5D), look.z).normalize();
        Vec3 eye = player.getEyePosition();

        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(dir.scale(DASH_RANGE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double dist = hit.getType() == HitResult.Type.MISS ? DASH_RANGE : Math.max(0.0D, hit.getLocation().distanceTo(eye) - 0.8D);
        Vec3 dest = start.add(dir.scale(dist));
        while (dist > 0.5D && !level.noCollision(player, player.getBoundingBox().move(dest.subtract(start)))) {
            dist -= 0.5D;
            dest = start.add(dir.scale(dist));
        }
        if (dist <= 0.5D) return false;

        // Slash everything along the dash path
        AABB sweep = player.getBoundingBox().expandTowards(dest.subtract(start)).inflate(0.8D);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, sweep,
                t -> t != player && t.isAlive() && !t.isSpectator() && !player.isAlliedTo(t))) {
            e.hurt(player.damageSources().indirectMagic(player, player), 10.0F);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY(0.5D), e.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        // Afterimage trail
        for (double d = 0.0D; d <= dist; d += 0.5D) {
            Vec3 p = start.add(dir.scale(d));
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y + 1.0D, p.z, 4, 0.2D, 0.5D, 0.2D, 0.01D);
            level.sendParticles(ParticleTypes.WITCH, p.x, p.y + 1.0D, p.z, 1, 0.2D, 0.4D, 0.2D, 0.0D);
        }

        level.playSound(null, start.x, start.y, start.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.5F);
        player.teleportTo(dest.x, dest.y, dest.z);
        player.resetFallDistance();
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    private static double Mth_clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    // ---------------------------------------------------------------- Weapon stats

    @Override
    @SuppressWarnings("deprecation")
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.modifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // permanent shimmer
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.obsidiansovereign.scepter.tap").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.obsidiansovereign.scepter.hold").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.obsidiansovereign.scepter.sneak").withStyle(ChatFormatting.LIGHT_PURPLE));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
