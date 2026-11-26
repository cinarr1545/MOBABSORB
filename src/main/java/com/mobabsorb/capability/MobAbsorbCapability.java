package com.mobabsorb.capability;

import com.mobabsorb.MobAbsorbMod;
import com.mobabsorb.network.ModNetwork;
import com.mobabsorb.network.SyncMobAbsorbPacket;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MobAbsorbCapability {
    public static final Capability<MobAbsorbData> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = new ResourceLocation(MobAbsorbMod.MOD_ID, "mob_absorb_data");

    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(MobAbsorbData.class);
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            Provider provider = new Provider();
            event.addCapability(ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(INSTANCE).ifPresent(oldData ->
            event.getEntity().getCapability(INSTANCE).ifPresent(newData -> newData.copyFrom(oldData))
        );
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToAll(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToAll(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToAll(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer targetPlayer && event.getEntity() instanceof ServerPlayer tracker) {
            targetPlayer.getCapability(INSTANCE).ifPresent(data ->
                ModNetwork.sendToPlayer(new SyncMobAbsorbPacket(targetPlayer.getId(), data), tracker)
            );
        }
    }

    public static void syncToAll(ServerPlayer player) {
        player.getCapability(INSTANCE).ifPresent(data ->
            ModNetwork.sendToTrackingAndSelf(new SyncMobAbsorbPacket(player.getId(), data), player)
        );
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final MobAbsorbData data = new MobAbsorbData();
        private final LazyOptional<MobAbsorbData> optional = LazyOptional.of(() -> data);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return cap == INSTANCE ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.saveNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.loadNBT(nbt);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }
}
