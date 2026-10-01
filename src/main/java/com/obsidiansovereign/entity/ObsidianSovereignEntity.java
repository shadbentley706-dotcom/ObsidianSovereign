package com.obsidiansovereign.entity;

import com.obsidiansovereign.registry.ModEntities;
import com.obsidiansovereign.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Obsidian Sovereign: a three-phase warlock-king boss.
 *  Phase 1 (100%-66%): arcane bolts, teleport strikes, flight, force fields.
 *  Phase 2 (66%-33%): crystal shield (force field that must be shattered) + Runebound Soldiers.
 *  Phase 3 (33%-0%):  enraged - faster, triple bolts, more flight, divebomb + ground-slam shockwaves.
 * He has no voice: he fights in complete silence.
 */
public class ObsidianSovereignEntity extends Monster implements PowerableMob {
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(ObsidianSovereignEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHIELD =
            SynchedEntityData.defineId(ObsidianSovereignEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FIELD =
            SynchedEntityData.defineId(ObsidianSovereignEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int MAX_SHIELD = 80;
    private static final int MAX_SOLDIERS = 6;

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(true);

    private int boltCooldown = 40;
    private int teleportCooldown = 100;
    private int summonCooldown = 0;
    private int slamCooldown = 60;
    private int slamCharge = 0;
    private int stunTicks = 0;

    private int flyTicks = 0;
    private int flyCooldown = 100;
    private float orbitAngle = 0.0F;
    private int meleeCooldown = 0;
    private boolean diveBomb = false;

    private int fieldTicks = 0;
    private int fieldCooldown = 160;

    public ObsidianSovereignEntity(EntityType<? extends ObsidianSovereignEntity> type, Level level) {
        super(type, level);
        this.xpReward = 150;
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SOVEREIGN_SCEPTER.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F); // the loot table drops the scepter instead
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D) // + the scepter's own damage
                .add(Attributes.ATTACK_KNOCKBACK, 1.5D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    /** Natural spawning: deep underground (below Y=0), rare, and never near another Sovereign. */
    public static boolean checkSovereignSpawnRules(EntityType<ObsidianSovereignEntity> type, ServerLevelAccessor level,
                                                   MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION) {
            if (pos.getY() >= 0 || random.nextInt(30) != 0) return false;
            if (!level.getEntitiesOfClass(ObsidianSovereignEntity.class, new AABB(pos).inflate(96.0D)).isEmpty()) return false;
        }
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }

    // ---------------------------------------------------------------- AI

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true) {
            @Override public boolean canUse() { return !isBusy() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !isBusy() && super.canContinueToUse(); }
        });
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, RuneboundSoldierEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    private boolean isBusy() {
        return stunTicks > 0 || slamCharge > 0 || isFlying();
    }

    public boolean isFlying() {
        return flyTicks > 0;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel server = (ServerLevel) this.level();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        updatePhase(server);
        tickForceField();

        if (stunTicks > 0) {
            stunTicks--;
            if (isFlying()) stopFlying(false);
            this.getNavigation().stop();
            if (stunTicks % 5 == 0) {
                server.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + getBbHeight() + 0.3D, getZ(),
                        6, 0.4D, 0.1D, 0.4D, 0.05D);
            }
            return;
        }

        if (boltCooldown > 0) boltCooldown--;
        if (teleportCooldown > 0) teleportCooldown--;
        if (summonCooldown > 0) summonCooldown--;
        if (slamCooldown > 0) slamCooldown--;
        if (flyCooldown > 0) flyCooldown--;
        if (meleeCooldown > 0) meleeCooldown--;

        // Divebomb landing -> shockwave
        if (diveBomb && this.onGround()) {
            diveBomb = false;
            groundSlam(server);
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            slamCharge = 0;
            if (isFlying()) stopFlying(false);
            return;
        }

        if (slamCharge > 0) {
            slamCharge--;
            this.getNavigation().stop();
            server.sendParticles(ParticleTypes.DRAGON_BREATH, getX(), getY() + 0.2D, getZ(), 8, 1.5D, 0.1D, 1.5D, 0.01D);
            if (slamCharge == 0) groundSlam(server);
            return;
        }

        int phase = getPhase();
        double distSq = this.distanceToSqr(target);

        // ---- Airborne: circle above the target and rain bolts
        if (isFlying()) {
            tickFlight(server, target, phase);
            if (boltCooldown <= 0 && this.hasLineOfSight(target)) {
                castBolts(target, phase);
                boltCooldown = phase == 3 ? 25 : 40;
            }
            return;
        }

        // ---- Take off: when the target is above him, or now and then
        if (flyCooldown <= 0 && (target.getY() - getY() > 3.0D || this.random.nextInt(phase == 3 ? 60 : 140) == 0)) {
            startFlying(server);
            return;
        }

        if (phase == 3 && slamCooldown <= 0 && distSq < 36.0D) {
            slamCharge = 15;
            slamCooldown = 100;
            return;
        }

        if (phase == 2 && summonCooldown <= 0) {
            summonSoldiers(server, target, 2);
            summonCooldown = 300;
        }

        if (teleportCooldown <= 0 && (distSq > 144.0D || this.random.nextInt(60) == 0)) {
            teleportStrike(server, target);
            teleportCooldown = phase == 3 ? 100 : 160;
        } else if (boltCooldown <= 0 && distSq > 9.0D && this.hasLineOfSight(target)) {
            castBolts(target, phase);
            boltCooldown = phase == 3 ? 30 : 55;
        }
    }

    private void updatePhase(ServerLevel server) {
        float pct = this.getHealth() / this.getMaxHealth();
        int wanted = pct > 0.66F ? 1 : (pct > 0.33F ? 2 : 3);
        while (getPhase() < wanted) {
            setPhase(getPhase() + 1);
            onEnterPhase(server, getPhase());
        }
    }

    private void onEnterPhase(ServerLevel server, int phase) {
        if (phase == 2) {
            setShield(MAX_SHIELD);
            this.playSound(SoundEvents.BEACON_ACTIVATE, 3.0F, 0.6F);
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5D, getZ(), 80, 1.0D, 1.5D, 1.0D, 0.1D);
            summonSoldiers(server, this.getTarget(), 3);
            summonCooldown = 300;
        } else if (phase == 3) {
            server.sendParticles(ParticleTypes.DRAGON_BREATH, getX(), getY() + 1.5D, getZ(), 100, 1.2D, 1.5D, 1.2D, 0.05D);
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
            applyEnrage();
            teleportCooldown = 40;
            flyCooldown = 20;
            activateForceField(server);
        }
    }

    private void applyEnrage() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.38D);
    }

    // ---------------------------------------------------------------- Flight

    private void startFlying(ServerLevel server) {
        flyTicks = 100 + this.random.nextInt(60);
        this.setNoGravity(true);
        this.getNavigation().stop();
        this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.6D, 0.0D));
        this.hasImpulse = true;
        orbitAngle = this.random.nextFloat() * Mth.TWO_PI;
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), 40, 0.6D, 0.2D, 0.6D, 0.1D);
        this.playSound(SoundEvents.PHANTOM_FLAP, 2.0F, 0.6F);
    }

    private void tickFlight(ServerLevel server, LivingEntity target, int phase) {
        flyTicks--;
        this.getNavigation().stop();
        this.getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0D);

        orbitAngle += phase == 3 ? 0.06F : 0.04F;
        Vec3 dest = new Vec3(target.getX() + Math.cos(orbitAngle) * 5.0D, target.getY() + 3.5D,
                target.getZ() + Math.sin(orbitAngle) * 5.0D);
        Vec3 diff = dest.subtract(this.position());
        double speed = phase == 3 ? 0.45D : 0.32D;
        Vec3 wanted = diff.lengthSqr() > speed * speed ? diff.normalize().scale(speed) : diff;
        this.setDeltaMovement(this.getDeltaMovement().scale(0.5D).add(wanted.scale(0.5D)));

        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (meleeCooldown <= 0 && this.distanceToSqr(target) < 6.0D) {
            this.swing(InteractionHand.MAIN_HAND);
            this.doHurtTarget(target);
            meleeCooldown = 20;
        }
        if (flyTicks <= 0) stopFlying(phase == 3);
    }

    private void stopFlying(boolean dive) {
        flyTicks = 0;
        this.setNoGravity(false);
        flyCooldown = 200;
        if (dive) {
            diveBomb = true;
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(v.x, -1.2D, v.z);
        }
    }

    // ---------------------------------------------------------------- Force field

    public boolean isFieldActive() {
        return this.entityData.get(DATA_FIELD);
    }

    /** Used by the swirl render layer: visible whenever the field OR the phase-2 crystal shield is up. */
    @Override
    public boolean isPowered() {
        return isFieldActive() || getShield() > 0;
    }

    private void tickForceField() {
        if (fieldTicks > 0 && --fieldTicks == 0) this.entityData.set(DATA_FIELD, false);
        if (fieldCooldown > 0) fieldCooldown--;
    }

    private void activateForceField(ServerLevel server) {
        fieldTicks = 60;
        fieldCooldown = getPhase() == 3 ? 160 : 240;
        this.entityData.set(DATA_FIELD, true);
        this.playSound(SoundEvents.BEACON_POWER_SELECT, 2.0F, 1.4F);

        // Repulsor pulse: blasts everyone nearby away.
        for (int i = 0; i < 32; i++) {
            double a = i / 32.0D * Math.PI * 2.0D;
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX() + Math.cos(a) * 2.5D, getY() + 1.2D,
                    getZ() + Math.sin(a) * 2.5D, 2, 0.0D, 0.3D, 0.0D, 0.05D);
        }
        AABB area = this.getBoundingBox().inflate(5.0D, 2.0D, 5.0D);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != this && e.isAlive() && !(e instanceof RuneboundSoldierEntity))) {
            double dx = e.getX() - getX();
            double dz = e.getZ() - getZ();
            e.hurt(this.damageSources().indirectMagic(this, this), 4.0F);
            e.knockback(1.6D, -dx, -dz);
            e.setDeltaMovement(e.getDeltaMovement().add(0.0D, 0.3D, 0.0D));
            e.hurtMarked = true;
        }
    }

    // ---------------------------------------------------------------- Attacks

    private void castBolts(LivingEntity target, int phase) {
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 2.0F, 0.6F);
        this.swing(InteractionHand.MAIN_HAND);
        int count = phase == 3 ? 3 : 1;
        float damage = phase == 3 ? 10.0F : 8.0F;

        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5D) - this.getEyeY();
        double dz = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double baseAngle = Math.atan2(dz, dx);

        for (int i = 0; i < count; i++) {
            double angle = baseAngle + (i - (count - 1) / 2.0D) * 0.25D;
            double vx = Math.cos(angle) * horizontal;
            double vz = Math.sin(angle) * horizontal;
            ArcaneBoltEntity bolt = new ArcaneBoltEntity(this.level(), this, vx, dy, vz);
            bolt.setPos(getX() + Math.cos(angle) * 0.8D, getEyeY() - 0.3D, getZ() + Math.sin(angle) * 0.8D);
            bolt.setDamage(damage);
            this.level().addFreshEntity(bolt);
        }
    }

    private void teleportStrike(ServerLevel server, LivingEntity target) {
        Vec3 look = target.getLookAngle();
        double tx = target.getX() - look.x * 2.5D;
        double tz = target.getZ() - look.z * 2.5D;
        double ox = getX(), oy = getY(), oz = getZ();

        if (this.randomTeleport(tx, target.getY(), tz, false)) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, ox, oy + 1.5D, oz, 40, 0.5D, 1.0D, 0.5D, 0.05D);
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5D, getZ(), 40, 0.5D, 1.0D, 0.5D, 0.05D);
            server.playSound(null, ox, oy, oz, SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.5F, 0.6F);
            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5F, 0.6F);
            this.getLookControl().setLookAt(target, 180.0F, 180.0F);
            if (this.distanceToSqr(target) < 16.0D) {
                this.swing(InteractionHand.MAIN_HAND);
                this.doHurtTarget(target);
            }
        }
    }

    private void summonSoldiers(ServerLevel server, @Nullable LivingEntity target, int wanted) {
        int existing = server.getEntitiesOfClass(RuneboundSoldierEntity.class, this.getBoundingBox().inflate(24.0D)).size();
        int toSpawn = Math.min(wanted, MAX_SOLDIERS - existing);
        if (toSpawn <= 0) return;

        this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 2.0F, 0.7F);
        for (int i = 0; i < toSpawn; i++) {
            RuneboundSoldierEntity soldier = ModEntities.RUNEBOUND_SOLDIER.get().create(server);
            if (soldier == null) continue;
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double sx = getX() + Math.cos(angle) * 3.0D;
            double sz = getZ() + Math.sin(angle) * 3.0D;
            soldier.moveTo(sx, getY(), sz, this.random.nextFloat() * 360.0F, 0.0F);
            soldier.setLimitedLife(900);
            if (target != null) soldier.setTarget(target);
            server.addFreshEntity(soldier);
            server.sendParticles(ParticleTypes.WITCH, sx, getY() + 1.0D, sz, 20, 0.3D, 0.8D, 0.3D, 0.05D);
        }
    }

    private void groundSlam(ServerLevel server) {
        this.swing(InteractionHand.MAIN_HAND);
        this.playSound(SoundEvents.GENERIC_EXPLODE, 2.0F, 0.7F);
        for (int i = 0; i < 24; i++) {
            double a = i / 24.0D * Math.PI * 2.0D;
            for (double r = 1.5D; r <= 6.0D; r += 1.5D) {
                server.sendParticles(ParticleTypes.DRAGON_BREATH, getX() + Math.cos(a) * r, getY() + 0.2D,
                        getZ() + Math.sin(a) * r, 1, 0.0D, 0.05D, 0.0D, 0.01D);
            }
        }
        server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5D, getZ(), 6, 1.5D, 0.2D, 1.5D, 0.0D);

        AABB area = this.getBoundingBox().inflate(6.0D, 2.0D, 6.0D);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != this && e.isAlive() && !(e instanceof RuneboundSoldierEntity))) {
            double dx = e.getX() - getX();
            double dz = e.getZ() - getZ();
            if (e.hurt(this.damageSources().mobAttack(this), 10.0F)) {
                e.knockback(1.8D, -dx, -dz);
                e.setDeltaMovement(e.getDeltaMovement().add(0.0D, 0.5D, 0.0D));
                e.hurtMarked = true;
            }
        }
    }

    // ---------------------------------------------------------------- Shield / damage

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;

        if (!this.level().isClientSide && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // Force field: blocks everything (arrows bounce off).
            if (isFieldActive()) {
                this.playSound(SoundEvents.AMETHYST_BLOCK_HIT, 1.5F, 1.4F);
                return false;
            }
            // Phase-2 crystal shield: soaks damage until shattered.
            if (getShield() > 0) {
                int remaining = getShield() - Mth.ceil(amount);
                this.playSound(SoundEvents.AMETHYST_BLOCK_HIT, 2.0F, 0.7F + this.random.nextFloat() * 0.3F);
                if (remaining <= 0) shatterShield();
                else setShield(remaining);
                return false;
            }
        }
        if (stunTicks > 0) amount *= 1.5F;
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide && stunTicks <= 0 && fieldCooldown <= 0
                && this.random.nextFloat() < 0.35F) {
            activateForceField((ServerLevel) this.level());
        }
        return result;
    }

    private void shatterShield() {
        setShield(0);
        fieldTicks = 0;
        this.entityData.set(DATA_FIELD, false);
        stunTicks = 100;
        slamCharge = 0;
        this.getNavigation().stop();
        this.playSound(SoundEvents.GLASS_BREAK, 3.0F, 0.5F);
        this.playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 3.0F, 0.6F);
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1.5D, getZ(), 60, 1.0D, 1.5D, 1.0D, 0.2D);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide) {
            for (RuneboundSoldierEntity s : this.level().getEntitiesOfClass(RuneboundSoldierEntity.class,
                    this.getBoundingBox().inflate(32.0D))) {
                s.crumble();
            }
        }
    }

    // ---------------------------------------------------------------- Client visuals

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.WITCH, getRandomX(0.6D), getRandomY(), getRandomZ(0.6D), 0.0D, 0.0D, 0.0D);
            }
            if (this.isNoGravity()) { // flight trail
                for (int i = 0; i < 2; i++) {
                    this.level().addParticle(ParticleTypes.REVERSE_PORTAL, getRandomX(0.4D), getY() - 0.1D,
                            getRandomZ(0.4D), 0.0D, -0.05D, 0.0D);
                }
            }
            if (getPhase() >= 3 && this.random.nextInt(2) == 0) {
                this.level().addParticle(ParticleTypes.DRAGON_BREATH, getRandomX(0.6D), getRandomY(), getRandomZ(0.6D), 0.0D, 0.03D, 0.0D);
            }
        }
    }

    // ---------------------------------------------------------------- Boss bar

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    // ---------------------------------------------------------------- Misc behaviour

    @Override
    public boolean removeWhenFarAway(double distanceSq) { return false; }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof RuneboundSoldierEntity || super.isAlliedTo(other);
    }

    // No voice at all.
    @Override @Nullable protected SoundEvent getAmbientSound() { return null; }
    @Override @Nullable protected SoundEvent getHurtSound(DamageSource source) { return null; }
    @Override @Nullable protected SoundEvent getDeathSound() { return null; }

    // ---------------------------------------------------------------- Data

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, 1);
        this.entityData.define(DATA_SHIELD, 0);
        this.entityData.define(DATA_FIELD, false);
    }

    public int getPhase() { return this.entityData.get(DATA_PHASE); }
    private void setPhase(int phase) { this.entityData.set(DATA_PHASE, phase); }
    public int getShield() { return this.entityData.get(DATA_SHIELD); }
    private void setShield(int shield) { this.entityData.set(DATA_SHIELD, Math.max(0, shield)); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", getPhase());
        tag.putInt("Shield", getShield());
        tag.putInt("StunTicks", stunTicks);
        tag.putInt("FlyTicks", flyTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Phase")) setPhase(Math.max(1, tag.getInt("Phase")));
        setShield(tag.getInt("Shield"));
        stunTicks = tag.getInt("StunTicks");
        flyTicks = tag.getInt("FlyTicks");
        if (flyTicks <= 0) this.setNoGravity(false);
        if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
        if (getPhase() >= 3) {
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
            applyEnrage();
        }
    }
}
