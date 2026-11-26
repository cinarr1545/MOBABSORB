package com.mobabsorb.event;

import com.mobabsorb.capability.MobAbsorbCapability;
import com.mobabsorb.capability.MobAbsorbData;
import com.mobabsorb.power.MobPowerManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ServerEvents {

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        if (target instanceof Player) return;

        // Must be sneaking with empty main hand
        if (!player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) return;

        player.getCapability(MobAbsorbCapability.INSTANCE).ifPresent(data -> {
            if (data.isHoldingMob()) return;

            // Pick up / hug the mob!
            data.setHeldEntityId(target.getId());
            data.setConsumeMode(MobAbsorbData.MODE_NONE);
            data.setConsumeProgress(0);
            data.setGrabCooldown(10); // short grace period so initial click doesn't instantly absorb

            if (target instanceof Mob mob) {
                mob.setNoAi(true);
            }
            target.setNoGravity(true);
            target.noPhysics = true;

            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0f, 1.1f);
            player.displayClientMessage(Component.translatable("message.mobabsorb.grabbed", target.getDisplayName()), true);

            MobAbsorbCapability.syncToAll(player);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        });
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        player.getCapability(MobAbsorbCapability.INSTANCE).ifPresent(data -> {
            if (data.getGrabCooldown() > 0) {
                data.setGrabCooldown(data.getGrabCooldown() - 1);
            }

            // Passive mob powers
            MobPowerManager.tickPassivePowers(player, data);

            // Handle held mob
            if (!data.isHoldingMob()) return;

            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(data.getHeldEntityId());
            if (!(entity instanceof LivingEntity heldMob) || !heldMob.isAlive()) {
                data.setHeldEntityId(-1);
                MobAbsorbCapability.syncToAll(player);
                return;
            }

            // Position the held mob in front of the player (hugged, eating, or sinking into body like a swamp)
            updateHeldMobPosition(player, heldMob, data);

            int mode = data.getConsumeMode();
            if (mode == MobAbsorbData.MODE_EAT) {
                int progress = data.getConsumeProgress() + 1;
                data.setConsumeProgress(progress);

                // Eating animation: crunch sounds & particles
                if (progress % 5 == 0) {
                    level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.9f, 0.85f + level.random.nextFloat() * 0.3f);
                    Vec3 mouth = player.getEyePosition().add(player.getLookAngle().scale(0.45)).subtract(0, 0.15, 0);
                    level.sendParticles(ParticleTypes.CRIT, mouth.x, mouth.y, mouth.z, 6, 0.18, 0.18, 0.18, 0.05);
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, mouth.x, mouth.y, mouth.z, 2, 0.1, 0.1, 0.1, 0.01);
                }

                if (progress >= MobAbsorbData.EAT_DURATION) {
                    finishConsume(player, heldMob, data, false);
                } else if (progress % 2 == 0) {
                    MobAbsorbCapability.syncToAll(player);
                }
            } else if (mode == MobAbsorbData.MODE_SWAMP_ABSORB) {
                int progress = data.getConsumeProgress() + 1;
                data.setConsumeProgress(progress);

                // Swamp-like body absorption: viscous sinking sounds & swirling dark/bubble/soul particles
                if (progress % 6 == 0) {
                    level.playSound(null, player.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.65f, 0.55f + (data.getConsumeRatio() * 0.3f));
                    level.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 0.5f, 0.7f);
                }
                if (progress % 2 == 0) {
                    Vec3 chest = player.position().add(0, player.getBbHeight() * 0.55, 0);
                    Vec3 mobCenter = heldMob.position().add(0, heldMob.getBbHeight() * 0.4, 0);
                    // Particles pulled from mob into player's chest like quicksand/swamp
                    level.sendParticles(ParticleTypes.SQUID_INK, mobCenter.x, mobCenter.y, mobCenter.z, 4, 0.25, 0.25, 0.25, 0.02);
                    level.sendParticles(ParticleTypes.SOUL, chest.x, chest.y, chest.z, 2, 0.25, 0.35, 0.25, 0.03);
                    level.sendParticles(ParticleTypes.BUBBLE_POP, chest.x, chest.y, chest.z, 3, 0.3, 0.3, 0.3, 0.01);
                }

                if (progress >= MobAbsorbData.SWAMP_ABSORB_DURATION) {
                    finishConsume(player, heldMob, data, true);
                } else if (progress % 2 == 0) {
                    MobAbsorbCapability.syncToAll(player);
                }
            }
        });
    }

    private static void updateHeldMobPosition(ServerPlayer player, LivingEntity heldMob, MobAbsorbData data) {
        Vec3 look = player.getLookAngle();
        Vec3 horizLook = new Vec3(look.x, 0, look.z);
        if (horizLook.lengthSqr() < 1.0E-4) {
            horizLook = new Vec3(0, 0, 1);
        } else {
            horizLook = horizLook.normalize();
        }

        double forwardDist = 0.85;
        double yOffset = player.getBbHeight() * 0.42;

        if (data.getConsumeMode() == MobAbsorbData.MODE_EAT) {
            // Pull closer to the player's mouth and bob up and down while chewing
            float ratio = data.getConsumeRatio();
            double bob = Math.sin(data.getConsumeProgress() * 1.4) * 0.08;
            forwardDist = 0.75 - (ratio * 0.35);
            yOffset = player.getBbHeight() * 0.62 + bob;
        } else if (data.getConsumeMode() == MobAbsorbData.MODE_SWAMP_ABSORB) {
            // Gradually sink straight INTO the player's chest like a swamp pulling it in!
            float ratio = data.getConsumeRatio();
            forwardDist = 0.75 * (1.0 - ratio); // goes from 0.75 down to 0.0 (inside player's torso)
            yOffset = player.getBbHeight() * (0.45 - ratio * 0.10);
        }

        double targetX = player.getX() + horizLook.x * forwardDist;
        double targetY = player.getY() + yOffset - (heldMob.getBbHeight() * 0.25);
        double targetZ = player.getZ() + horizLook.z * forwardDist;

        heldMob.setDeltaMovement(Vec3.ZERO);
        heldMob.fallDistance = 0.0f;
        heldMob.teleportTo(targetX, targetY, targetZ);
        heldMob.setYRot(player.getYRot() + 180.0f);
        heldMob.setYHeadRot(player.getYRot() + 180.0f);
    }

    private static void finishConsume(ServerPlayer player, LivingEntity heldMob, MobAbsorbData data, boolean swampAbsorb) {
        ServerLevel level = player.serverLevel();
        Component mobName = heldMob.getDisplayName();

        // Grant powers from this mob
        MobPowerManager.absorbMobStats(heldMob, data);

        // Remove the mob
        heldMob.discard();
        data.setHeldEntityId(-1);

        if (swampAbsorb) {
            level.playSound(null, player.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1.2f, 0.8f);
            level.playSound(null, player.blockPosition(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 1.0f, 0.6f);
            level.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.4, 0.6, 0.4, 0.08);
            level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.35, 0.5, 0.35, 0.04);
            player.heal(6.0f);
            player.displayClientMessage(Component.translatable("message.mobabsorb.absorbed", mobName), false);
        } else {
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0f, 0.9f);
            level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.5, player.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
            player.getFoodData().eat(8, 0.8f);
            player.heal(4.0f);
            player.displayClientMessage(Component.translatable("message.mobabsorb.eaten", mobName), false);
        }

        MobAbsorbCapability.syncToAll(player);
    }

    public static void releaseHeldMob(ServerPlayer player, MobAbsorbData data, boolean notify) {
        if (!data.isHoldingMob()) return;
        Entity entity = player.serverLevel().getEntity(data.getHeldEntityId());
        if (entity instanceof LivingEntity heldMob) {
            if (heldMob instanceof Mob mob) {
                mob.setNoAi(false);
            }
            heldMob.setNoGravity(false);
            heldMob.noPhysics = false;
            Vec3 push = player.getLookAngle().scale(0.4);
            heldMob.setDeltaMovement(push.x, 0.15, push.z);
        }
        data.setHeldEntityId(-1);
        MobAbsorbCapability.syncToAll(player);
        if (notify) {
            player.displayClientMessage(Component.translatable("message.mobabsorb.released"), true);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(MobAbsorbCapability.INSTANCE).ifPresent(data -> releaseHeldMob(player, data, false));
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(MobAbsorbCapability.INSTANCE).ifPresent(data -> releaseHeldMob(player, data, false));
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof LivingEntity target) {
            player.getCapability(MobAbsorbCapability.INSTANCE).ifPresent(data ->
                MobPowerManager.onMeleeHit(player, target, data)
            );
        }
    }

    @SubscribeEvent
    public void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(MobAbsorbCapability.INSTANCE).ifPresent(data -> {
                if (MobPowerManager.shouldCancelFallDamage(data)) {
                    event.setCanceled(true);
                }
            });
        }
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        // Guardian/Elder Guardian thorns-like damage reflection
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().getEntity() instanceof LivingEntity attacker) {
            player.getCapability(MobAbsorbCapability.INSTANCE).ifPresent(data -> {
                if (data.getAbsorbedMobType() != null) {
                    String path = data.getAbsorbedMobType().getPath();
                    if (path.equals("guardian") || path.equals("elder_guardian") || path.equals("pufferfish")) {
                        attacker.hurt(player.serverLevel().damageSources().thorns(player), 3.0f);
                    }
                }
            });
        }
    }
}
