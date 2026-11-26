package com.mobabsorb.power;

import com.mobabsorb.capability.MobAbsorbCapability;
import com.mobabsorb.capability.MobAbsorbData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class MobPowerManager {

    public static void absorbMobStats(LivingEntity mob, MobAbsorbData data) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        data.setAbsorbedMobType(key);

        float maxHp = mob.getMaxHealth();
        float atk = mob.getAttribute(Attributes.ATTACK_DAMAGE) != null
                ? (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE)
                : 2.0f;
        float spd = mob.getAttribute(Attributes.MOVEMENT_SPEED) != null
                ? (float) mob.getAttributeValue(Attributes.MOVEMENT_SPEED)
                : 0.25f;

        data.setAbsorbedMaxHealth(maxHp);
        data.setAbsorbedAttackDamage(atk);
        data.setAbsorbedMoveSpeed(spd);
        data.setAbsorbedFireImmune(mob.fireImmune());
        data.setAbsorbedAquatic(mob.canBreatheUnderwater());
        data.setAbsorbedFlying(mob instanceof FlyingAnimal || mob.isNoGravity());
        data.setAbilityCooldown(0);
    }

    public static void tickPassivePowers(ServerPlayer player, MobAbsorbData data) {
        if (data.getAbilityCooldown() > 0) {
            data.setAbilityCooldown(data.getAbilityCooldown() - 1);
            if (data.getAbilityCooldown() % 10 == 0 || data.getAbilityCooldown() == 0) {
                MobAbsorbCapability.syncToAll(player);
            }
        }

        ResourceLocation mobType = data.getAbsorbedMobType();
        if (mobType == null) return;

        // Spider wall climbing (every tick)
        String path = mobType.getPath();
        if (path.equals("spider") || path.equals("cave_spider")) {
            if (player.horizontalCollision && !player.onGround()) {
                Vec3 delta = player.getDeltaMovement();
                player.setDeltaMovement(delta.x, 0.22D, delta.z);
                player.hurtMarked = true;
                player.fallDistance = 0.0f;
            }
        }

        // Refresh passive effects once per second
        if (player.tickCount % 20 != 0) return;

        switch (path) {
            case "creeper" -> {
                give(player, MobEffects.MOVEMENT_SPEED, 0);
                give(player, MobEffects.DAMAGE_RESISTANCE, 0);
            }
            case "enderman" -> {
                give(player, MobEffects.NIGHT_VISION, 0, 260);
                give(player, MobEffects.MOVEMENT_SPEED, 1);
            }
            case "blaze", "magma_cube", "strider" -> {
                give(player, MobEffects.FIRE_RESISTANCE, 0);
                give(player, MobEffects.SLOW_FALLING, 0);
            }
            case "ghast" -> {
                give(player, MobEffects.FIRE_RESISTANCE, 0);
                give(player, MobEffects.SLOW_FALLING, 0);
            }
            case "spider", "cave_spider" -> {
                give(player, MobEffects.JUMP, 1);
                give(player, MobEffects.NIGHT_VISION, 0, 260);
            }
            case "zombie", "husk", "zombie_villager", "zombified_piglin" -> {
                give(player, MobEffects.DAMAGE_BOOST, 0);
                give(player, MobEffects.DAMAGE_RESISTANCE, 0);
            }
            case "drowned" -> {
                give(player, MobEffects.WATER_BREATHING, 0);
                give(player, MobEffects.DOLPHINS_GRACE, 0);
                give(player, MobEffects.DAMAGE_BOOST, 0);
            }
            case "iron_golem" -> {
                give(player, MobEffects.DAMAGE_BOOST, 1);
                give(player, MobEffects.DAMAGE_RESISTANCE, 1);
                give(player, MobEffects.HEALTH_BOOST, 1);
            }
            case "warden" -> {
                give(player, MobEffects.DAMAGE_BOOST, 2);
                give(player, MobEffects.DAMAGE_RESISTANCE, 1);
                give(player, MobEffects.HEALTH_BOOST, 2);
            }
            case "wither" -> {
                give(player, MobEffects.REGENERATION, 1);
                give(player, MobEffects.FIRE_RESISTANCE, 0);
                give(player, MobEffects.DAMAGE_RESISTANCE, 1);
                player.removeEffect(MobEffects.WITHER);
            }
            case "ender_dragon" -> {
                give(player, MobEffects.SLOW_FALLING, 0);
                give(player, MobEffects.DAMAGE_BOOST, 2);
                give(player, MobEffects.DAMAGE_RESISTANCE, 1);
                give(player, MobEffects.HEALTH_BOOST, 2);
            }
            case "slime" -> {
                give(player, MobEffects.JUMP, 2);
            }
            case "phantom", "bat", "allay", "parrot", "bee", "chicken" -> {
                give(player, MobEffects.SLOW_FALLING, 0);
                if (path.equals("phantom") || path.equals("bat")) {
                    give(player, MobEffects.NIGHT_VISION, 0, 260);
                }
            }
            case "guardian", "elder_guardian", "dolphin", "squid", "glow_squid", "axolotl", "turtle", "cod", "salmon", "pufferfish", "tropical_fish" -> {
                give(player, MobEffects.WATER_BREATHING, 0);
                give(player, MobEffects.DOLPHINS_GRACE, 0);
                if (path.equals("turtle") || path.equals("elder_guardian")) {
                    give(player, MobEffects.DAMAGE_RESISTANCE, 1);
                }
            }
            case "wolf", "fox", "cat", "ocelot", "horse", "donkey", "mule", "rabbit" -> {
                give(player, MobEffects.MOVEMENT_SPEED, 1);
                if (path.equals("rabbit") || path.equals("horse")) {
                    give(player, MobEffects.JUMP, 2);
                }
            }
            case "shulker" -> {
                give(player, MobEffects.DAMAGE_RESISTANCE, 1);
            }
            case "witch" -> {
                give(player, MobEffects.REGENERATION, 0);
                player.removeEffect(MobEffects.POISON);
            }
            case "ravager", "hoglin", "zoglin", "piglin_brute" -> {
                give(player, MobEffects.DAMAGE_BOOST, 1);
                give(player, MobEffects.DAMAGE_RESISTANCE, 0);
            }
            case "villager", "wandering_trader" -> {
                give(player, MobEffects.HERO_OF_THE_VILLAGE, 0);
                give(player, MobEffects.LUCK, 0);
            }
            case "cow", "mooshroom", "pig", "sheep" -> {
                give(player, MobEffects.REGENERATION, 0);
                give(player, MobEffects.SATURATION, 0, 40);
            }
            default -> applyUniversalPassives(player, data);
        }
    }

    private static void applyUniversalPassives(ServerPlayer player, MobAbsorbData data) {
        if (data.getAbsorbedMaxHealth() >= 60.0f) {
            give(player, MobEffects.HEALTH_BOOST, 1);
            give(player, MobEffects.DAMAGE_RESISTANCE, 0);
        } else if (data.getAbsorbedMaxHealth() >= 30.0f) {
            give(player, MobEffects.HEALTH_BOOST, 0);
        }

        if (data.getAbsorbedAttackDamage() >= 8.0f) {
            give(player, MobEffects.DAMAGE_BOOST, 1);
        } else if (data.getAbsorbedAttackDamage() >= 4.0f) {
            give(player, MobEffects.DAMAGE_BOOST, 0);
        }

        if (data.getAbsorbedMoveSpeed() >= 0.30f) {
            give(player, MobEffects.MOVEMENT_SPEED, 0);
        }
        if (data.isAbsorbedFireImmune()) {
            give(player, MobEffects.FIRE_RESISTANCE, 0);
        }
        if (data.isAbsorbedAquatic()) {
            give(player, MobEffects.WATER_BREATHING, 0);
            give(player, MobEffects.DOLPHINS_GRACE, 0);
        }
        if (data.isAbsorbedFlying()) {
            give(player, MobEffects.SLOW_FALLING, 0);
        }
    }

    public static void onMeleeHit(ServerPlayer player, LivingEntity target, MobAbsorbData data) {
        ResourceLocation mobType = data.getAbsorbedMobType();
        if (mobType == null) return;

        switch (mobType.getPath()) {
            case "cave_spider", "bee", "pufferfish" ->
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            case "wither_skeleton", "wither" ->
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            case "stray" ->
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            case "husk" ->
                    target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 1));
            case "blaze", "magma_cube" ->
                    target.setSecondsOnFire(5);
            case "iron_golem", "ravager", "hoglin" -> {
                Vec3 push = target.position().subtract(player.position()).normalize();
                target.setDeltaMovement(push.x * 1.1, 0.55, push.z * 1.1);
                target.hurtMarked = true;
            }
            case "shulker" ->
                    target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 0));
        }
    }

    public static boolean shouldCancelFallDamage(MobAbsorbData data) {
        ResourceLocation mobType = data.getAbsorbedMobType();
        if (mobType == null) return false;
        String path = mobType.getPath();
        return path.equals("slime") || path.equals("magma_cube") || path.equals("cat")
                || path.equals("ocelot") || path.equals("chicken") || path.equals("phantom")
                || path.equals("bat") || path.equals("bee") || path.equals("allay")
                || path.equals("parrot") || data.isAbsorbedFlying();
    }

    public static void activatePower(ServerPlayer player, MobAbsorbData data) {
        ResourceLocation mobType = data.getAbsorbedMobType();
        if (mobType == null) {
            player.displayClientMessage(Component.translatable("message.mobabsorb.no_power"), true);
            return;
        }
        if (data.getAbilityCooldown() > 0) {
            int secs = (int) Math.ceil(data.getAbilityCooldown() / 20.0);
            player.displayClientMessage(Component.translatable("message.mobabsorb.cooldown", secs), true);
            return;
        }

        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        String path = mobType.getPath();
        int cooldownTicks = 100; // default 5s

        switch (path) {
            case "creeper" -> {
                // Controlled explosion that does not damage the player
                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
                AABB area = player.getBoundingBox().inflate(5.0);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
                    target.hurt(level.damageSources().playerAttack(player), 14.0f);
                    Vec3 kb = target.position().subtract(player.position()).normalize();
                    target.setDeltaMovement(kb.x * 1.2, 0.5, kb.z * 1.2);
                    target.hurtMarked = true;
                }
                cooldownTicks = 140;
            }
            case "enderman" -> {
                Vec3 start = player.getEyePosition();
                Vec3 end = start.add(look.scale(16.0));
                HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                Vec3 dest = hit.getLocation().subtract(look.scale(0.8));
                level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.4, 0.8, 0.4, 0.1);
                player.teleportTo(dest.x, dest.y, dest.z);
                level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                level.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y + 1.0, dest.z, 25, 0.4, 0.8, 0.4, 0.1);
                cooldownTicks = 80;
            }
            case "blaze", "magma_cube" -> {
                SmallFireball fireball = new SmallFireball(level, player, look.x, look.y, look.z);
                fireball.setPos(player.getX() + look.x, player.getEyeY() - 0.1, player.getZ() + look.z);
                level.addFreshEntity(fireball);
                level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                cooldownTicks = 40;
            }
            case "ghast" -> {
                LargeFireball fireball = new LargeFireball(level, player, look.x, look.y, look.z, 2);
                fireball.setPos(player.getX() + look.x * 1.5, player.getEyeY() - 0.2, player.getZ() + look.z * 1.5);
                level.addFreshEntity(fireball);
                level.playSound(null, player.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                cooldownTicks = 100;
            }
            case "warden" -> {
                // Sonic Boom beam
                level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 1.0f);
                Vec3 origin = player.getEyePosition();
                for (int i = 1; i <= 16; i++) {
                    Vec3 p = origin.add(look.scale(i));
                    level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    AABB box = new AABB(p.x - 1.2, p.y - 1.2, p.z - 1.2, p.x + 1.2, p.y + 1.2, p.z + 1.2);
                    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)) {
                        target.hurt(level.damageSources().sonicBoom(player), 16.0f);
                        target.setDeltaMovement(look.x * 1.4, 0.4, look.z * 1.4);
                        target.hurtMarked = true;
                    }
                }
                cooldownTicks = 160;
            }
            case "wither" -> {
                WitherSkull skull = new WitherSkull(level, player, look.x, look.y, look.z);
                skull.setPos(player.getX() + look.x * 1.2, player.getEyeY() - 0.1, player.getZ() + look.z * 1.2);
                level.addFreshEntity(skull);
                level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                cooldownTicks = 60;
            }
            case "ender_dragon" -> {
                DragonFireball fireball = new DragonFireball(level, player, look.x, look.y, look.z);
                fireball.setPos(player.getX() + look.x * 1.5, player.getEyeY() - 0.1, player.getZ() + look.z * 1.5);
                level.addFreshEntity(fireball);
                level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                cooldownTicks = 120;
            }
            case "skeleton", "stray", "pillager" -> {
                Arrow arrow = new Arrow(level, player);
                arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 3.0f, 0.5f);
                arrow.setBaseDamage(4.0);
                if (path.equals("stray")) {
                    arrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
                }
                level.addFreshEntity(arrow);
                level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                cooldownTicks = 30;
            }
            case "evoker" -> {
                for (int i = 1; i <= 7; i++) {
                    double x = player.getX() + look.x * (i * 1.3);
                    double z = player.getZ() + look.z * (i * 1.3);
                    EvokerFangs fangs = new EvokerFangs(level, x, player.getY(), z, (float) Math.toRadians(player.getYRot()), i * 2, player);
                    level.addFreshEntity(fangs);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.0f, 1.0f);
                cooldownTicks = 100;
            }
            case "shulker" -> {
                List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(18.0), e -> e != player);
                if (!nearby.isEmpty()) {
                    LivingEntity target = nearby.get(0);
                    ShulkerBullet bullet = new ShulkerBullet(level, player, target, player.getDirection().getAxis());
                    level.addFreshEntity(bullet);
                    level.playSound(null, player.blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                    cooldownTicks = 60;
                } else {
                    cooldownTicks = 20;
                }
            }
            case "cow", "mooshroom" -> {
                player.removeAllEffects();
                player.heal(4.0f);
                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0f, 1.0f);
                cooldownTicks = 100;
            }
            case "squid", "glow_squid" -> {
                level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), 40, 1.5, 0.8, 1.5, 0.05);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0), e -> e != player)) {
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                }
                level.playSound(null, player.blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 1.0f, 1.0f);
                cooldownTicks = 100;
            }
            case "phantom", "bat", "bee", "allay", "parrot", "chicken" -> {
                player.setDeltaMovement(look.x * 1.4, 0.85, look.z * 1.4);
                player.hurtMarked = true;
                level.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0f, 1.1f);
                level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 15, 0.4, 0.2, 0.4, 0.05);
                cooldownTicks = 70;
            }
            case "witch" -> {
                player.heal(6.0f);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
                level.playSound(null, player.blockPosition(), SoundEvents.WITCH_DRINK, SoundSource.PLAYERS, 1.0f, 1.0f);
                cooldownTicks = 120;
            }
            default -> {
                // Universal Essence Burst for any other vanilla or modded mob!
                float dmg = Math.max(5.0f, Math.min(18.0f, data.getAbsorbedAttackDamage() * 1.5f));
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 30, 1.5, 0.6, 1.5, 0.08);
                level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 1.0f);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5.0), e -> e != player)) {
                    target.hurt(level.damageSources().magic(), dmg);
                    Vec3 kb = target.position().subtract(player.position()).normalize();
                    target.setDeltaMovement(kb.x * 0.9, 0.35, kb.z * 0.9);
                    target.hurtMarked = true;
                }
                cooldownTicks = 100;
            }
        }

        data.setAbilityCooldown(cooldownTicks);
        MobAbsorbCapability.syncToAll(player);
    }

    private static void give(ServerPlayer player, net.minecraft.world.effect.MobEffect effect, int amp) {
        give(player, effect, amp, 60);
    }

    private static void give(ServerPlayer player, net.minecraft.world.effect.MobEffect effect, int amp, int duration) {
        player.addEffect(new MobEffectInstance(effect, duration, amp, true, false, true));
    }
}
