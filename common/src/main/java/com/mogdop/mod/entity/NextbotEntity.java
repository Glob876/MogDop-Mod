package com.mogdop.mod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class NextbotEntity extends PathAwareEntity {

    private static final TrackedData<String> TEXTURE_NAME = DataTracker.registerData(NextbotEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Float> DAMAGE = DataTracker.registerData(NextbotEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> MOVE_SPEED = DataTracker.registerData(NextbotEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public static final float DEFAULT_DAMAGE = 100.0F;
    public static final float DEFAULT_SPEED = 0.3F;

    public NextbotEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createNextbotAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, DEFAULT_SPEED)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, DEFAULT_DAMAGE)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.targetSelector.add(0, new RevengeGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(TEXTURE_NAME, "");
        builder.add(DAMAGE, DEFAULT_DAMAGE);
        builder.add(MOVE_SPEED, DEFAULT_SPEED);
    }

    public String getTextureName() {
        return this.dataTracker.get(TEXTURE_NAME);
    }

    public void setTextureName(String name) {
        this.dataTracker.set(TEXTURE_NAME, name == null ? "" : name);
    }

    public float getNextbotDamage() {
        Float v = this.dataTracker.get(DAMAGE);
        return v == null ? DEFAULT_DAMAGE : v;
    }

    public void setNextbotDamage(float damage) {
        float d = Math.max(1.0F, Math.min(1000.0F, damage));
        this.dataTracker.set(DAMAGE, d);
        if (this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE) != null) {
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(d);
        }
    }

    public float getNextbotSpeed() {
        Float v = this.dataTracker.get(MOVE_SPEED);
        return v == null ? DEFAULT_SPEED : v;
    }

    public void setNextbotSpeed(float speed) {
        float s = (float) Math.max(0.05, Math.min(1.0, speed));
        this.dataTracker.set(MOVE_SPEED, s);
        if (this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED) != null) {
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(s);
        }
    }

    public void applyPreset(String textureName, float speed, float damage) {
        setTextureName(textureName);
        setNextbotSpeed(speed);
        setNextbotDamage(damage);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.age % 10 == 0) {
            LivingEntity target = this.getTarget();
            if (target instanceof PlayerEntity player && !player.isCreative() && !player.isSpectator()) {
                if (this.distanceTo(player) < 1.8F && player.isAlive()) {
                    this.tryAttack(player);
                }
            }
        }
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("NextbotTexture")) {
            setTextureName(nbt.getString("NextbotTexture"));
        }
        if (nbt.contains("NextbotDamage")) {
            setNextbotDamage(nbt.getFloat("NextbotDamage"));
        }
        if (nbt.contains("NextbotSpeed")) {
            setNextbotSpeed(nbt.getFloat("NextbotSpeed"));
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("NextbotTexture", getTextureName());
        nbt.putFloat("NextbotDamage", getNextbotDamage());
        nbt.putFloat("NextbotSpeed", getNextbotSpeed());
    }
}
