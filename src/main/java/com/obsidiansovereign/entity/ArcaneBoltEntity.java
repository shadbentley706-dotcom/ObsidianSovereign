package com.obsidiansovereign.entity;

import com.obsidiansovereign.registry.ModEntities;
import com.obsidiansovereign.registry.ModItems;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Violet arcane projectile. Doesn't burn or explode; deals magic damage and briefly slows.
 * Like a ghast fireball, a player can punch it back at the caster!
 */
public class ArcaneBoltEntity extends Fireball {
    private float damage = 8.0F;

    public ArcaneBoltEntity(EntityType<? extends ArcaneBoltEntity> type, Level level) {
        super(type, level);
    }

    public ArcaneBoltEntity(Level level, LivingEntity shooter, double dx, double dy, double dz) {
        super(ModEntities.ARCANE_BOLT.get(), shooter, dx, dy, dz, level);
        Vec3 dir = new Vec3(dx, dy, dz).normalize();
        this.setDeltaMovement(dir.scale(0.6D)); // start fast instead of slowly accelerating
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.SOVEREIGN_RUNE_SHARD.get());
    }

    @Override protected boolean shouldBurn() { return false; }
    @Override protected ParticleOptions getTrailParticle() { return ParticleTypes.WITCH; }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > 100) this.discard();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity owner = this.getOwner();
        if (owner instanceof ObsidianSovereignEntity && entity instanceof RuneboundSoldierEntity) return false;
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) return;
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        boolean hurt = target.hurt(this.damageSources().indirectMagic(this, owner), damage);
        if (hurt) {
            if (owner instanceof LivingEntity livingOwner) this.doEnchantDamageEffects(livingOwner, target);
            if (target instanceof LivingEntity livingTarget) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.WITCH, getX(), getY(), getZ(), 15, 0.3D, 0.3D, 0.3D, 0.05D);
            this.playSound(SoundEvents.AMETHYST_BLOCK_BREAK, 1.0F, 1.2F);
            this.discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("BoltDamage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BoltDamage")) damage = tag.getFloat("BoltDamage");
    }
}
