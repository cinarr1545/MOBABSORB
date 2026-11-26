package com.mobabsorb.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class MobAbsorbData {
    public static final int MODE_NONE = 0;
    public static final int MODE_EAT = 1;
    public static final int MODE_SWAMP_ABSORB = 2;

    public static final int EAT_DURATION = 40;          // 2.0 seconds
    public static final int SWAMP_ABSORB_DURATION = 60; // 3.0 seconds

    private int heldEntityId = -1;
    private int consumeMode = MODE_NONE;
    private int consumeProgress = 0;
    private int grabCooldown = 0;

    @Nullable
    private ResourceLocation absorbedMobType = null;
    private float absorbedMaxHealth = 20.0f;
    private float absorbedAttackDamage = 2.0f;
    private float absorbedMoveSpeed = 0.25f;
    private boolean absorbedFireImmune = false;
    private boolean absorbedAquatic = false;
    private boolean absorbedFlying = false;
    private int abilityCooldown = 0;

    public int getHeldEntityId() {
        return heldEntityId;
    }

    public void setHeldEntityId(int heldEntityId) {
        this.heldEntityId = heldEntityId;
        if (heldEntityId == -1) {
            this.consumeMode = MODE_NONE;
            this.consumeProgress = 0;
        }
    }

    public boolean isHoldingMob() {
        return heldEntityId != -1;
    }

    public int getConsumeMode() {
        return consumeMode;
    }

    public void setConsumeMode(int consumeMode) {
        if (this.consumeMode != consumeMode) {
            this.consumeMode = consumeMode;
            this.consumeProgress = 0;
        }
    }

    public int getConsumeProgress() {
        return consumeProgress;
    }

    public void setConsumeProgress(int consumeProgress) {
        this.consumeProgress = consumeProgress;
    }

    public int getMaxConsumeDuration() {
        return consumeMode == MODE_SWAMP_ABSORB ? SWAMP_ABSORB_DURATION : EAT_DURATION;
    }

    public float getConsumeRatio() {
        int max = getMaxConsumeDuration();
        return max <= 0 ? 0f : Math.min(1.0f, (float) consumeProgress / (float) max);
    }

    public int getGrabCooldown() {
        return grabCooldown;
    }

    public void setGrabCooldown(int grabCooldown) {
        this.grabCooldown = grabCooldown;
    }

    @Nullable
    public ResourceLocation getAbsorbedMobType() {
        return absorbedMobType;
    }

    public void setAbsorbedMobType(@Nullable ResourceLocation absorbedMobType) {
        this.absorbedMobType = absorbedMobType;
    }

    public float getAbsorbedMaxHealth() {
        return absorbedMaxHealth;
    }

    public void setAbsorbedMaxHealth(float absorbedMaxHealth) {
        this.absorbedMaxHealth = absorbedMaxHealth;
    }

    public float getAbsorbedAttackDamage() {
        return absorbedAttackDamage;
    }

    public void setAbsorbedAttackDamage(float absorbedAttackDamage) {
        this.absorbedAttackDamage = absorbedAttackDamage;
    }

    public float getAbsorbedMoveSpeed() {
        return absorbedMoveSpeed;
    }

    public void setAbsorbedMoveSpeed(float absorbedMoveSpeed) {
        this.absorbedMoveSpeed = absorbedMoveSpeed;
    }

    public boolean isAbsorbedFireImmune() {
        return absorbedFireImmune;
    }

    public void setAbsorbedFireImmune(boolean absorbedFireImmune) {
        this.absorbedFireImmune = absorbedFireImmune;
    }

    public boolean isAbsorbedAquatic() {
        return absorbedAquatic;
    }

    public void setAbsorbedAquatic(boolean absorbedAquatic) {
        this.absorbedAquatic = absorbedAquatic;
    }

    public boolean isAbsorbedFlying() {
        return absorbedFlying;
    }

    public void setAbsorbedFlying(boolean absorbedFlying) {
        this.absorbedFlying = absorbedFlying;
    }

    public int getAbilityCooldown() {
        return abilityCooldown;
    }

    public void setAbilityCooldown(int abilityCooldown) {
        this.abilityCooldown = Math.max(0, abilityCooldown);
    }

    public void copyFrom(MobAbsorbData other) {
        this.absorbedMobType = other.absorbedMobType;
        this.absorbedMaxHealth = other.absorbedMaxHealth;
        this.absorbedAttackDamage = other.absorbedAttackDamage;
        this.absorbedMoveSpeed = other.absorbedMoveSpeed;
        this.absorbedFireImmune = other.absorbedFireImmune;
        this.absorbedAquatic = other.absorbedAquatic;
        this.absorbedFlying = other.absorbedFlying;
        this.abilityCooldown = other.abilityCooldown;
    }

    public CompoundTag saveNBT() {
        CompoundTag tag = new CompoundTag();
        if (absorbedMobType != null) {
            tag.putString("AbsorbedMobType", absorbedMobType.toString());
        }
        tag.putFloat("AbsorbedMaxHealth", absorbedMaxHealth);
        tag.putFloat("AbsorbedAttackDamage", absorbedAttackDamage);
        tag.putFloat("AbsorbedMoveSpeed", absorbedMoveSpeed);
        tag.putBoolean("AbsorbedFireImmune", absorbedFireImmune);
        tag.putBoolean("AbsorbedAquatic", absorbedAquatic);
        tag.putBoolean("AbsorbedFlying", absorbedFlying);
        tag.putInt("AbilityCooldown", abilityCooldown);
        return tag;
    }

    public void loadNBT(CompoundTag tag) {
        if (tag.contains("AbsorbedMobType")) {
            this.absorbedMobType = ResourceLocation.tryParse(tag.getString("AbsorbedMobType"));
        } else {
            this.absorbedMobType = null;
        }
        this.absorbedMaxHealth = tag.contains("AbsorbedMaxHealth") ? tag.getFloat("AbsorbedMaxHealth") : 20.0f;
        this.absorbedAttackDamage = tag.contains("AbsorbedAttackDamage") ? tag.getFloat("AbsorbedAttackDamage") : 2.0f;
        this.absorbedMoveSpeed = tag.contains("AbsorbedMoveSpeed") ? tag.getFloat("AbsorbedMoveSpeed") : 0.25f;
        this.absorbedFireImmune = tag.getBoolean("AbsorbedFireImmune");
        this.absorbedAquatic = tag.getBoolean("AbsorbedAquatic");
        this.absorbedFlying = tag.getBoolean("AbsorbedFlying");
        this.abilityCooldown = tag.getInt("AbilityCooldown");
    }
}
