package com.mobabsorb.network;

import com.mobabsorb.capability.MobAbsorbCapability;
import com.mobabsorb.capability.MobAbsorbData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class SyncMobAbsorbPacket {
    private final int playerId;
    private final int heldEntityId;
    private final int consumeMode;
    private final int consumeProgress;
    @Nullable
    private final ResourceLocation absorbedMobType;
    private final int abilityCooldown;

    public SyncMobAbsorbPacket(int playerId, MobAbsorbData data) {
        this.playerId = playerId;
        this.heldEntityId = data.getHeldEntityId();
        this.consumeMode = data.getConsumeMode();
        this.consumeProgress = data.getConsumeProgress();
        this.absorbedMobType = data.getAbsorbedMobType();
        this.abilityCooldown = data.getAbilityCooldown();
    }

    private SyncMobAbsorbPacket(int playerId, int heldEntityId, int consumeMode, int consumeProgress,
                                @Nullable ResourceLocation absorbedMobType, int abilityCooldown) {
        this.playerId = playerId;
        this.heldEntityId = heldEntityId;
        this.consumeMode = consumeMode;
        this.consumeProgress = consumeProgress;
        this.absorbedMobType = absorbedMobType;
        this.abilityCooldown = abilityCooldown;
    }

    public static void encode(SyncMobAbsorbPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.playerId);
        buf.writeInt(msg.heldEntityId);
        buf.writeInt(msg.consumeMode);
        buf.writeInt(msg.consumeProgress);
        buf.writeBoolean(msg.absorbedMobType != null);
        if (msg.absorbedMobType != null) {
            buf.writeResourceLocation(msg.absorbedMobType);
        }
        buf.writeInt(msg.abilityCooldown);
    }

    public static SyncMobAbsorbPacket decode(FriendlyByteBuf buf) {
        int playerId = buf.readInt();
        int heldEntityId = buf.readInt();
        int consumeMode = buf.readInt();
        int consumeProgress = buf.readInt();
        ResourceLocation mobType = buf.readBoolean() ? buf.readResourceLocation() : null;
        int cooldown = buf.readInt();
        return new SyncMobAbsorbPacket(playerId, heldEntityId, consumeMode, consumeProgress, mobType, cooldown);
    }

    public static void handle(SyncMobAbsorbPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg)));
        ctx.setPacketHandled(true);
    }

    private static void handleClient(SyncMobAbsorbPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity entity = mc.level.getEntity(msg.playerId);
        if (entity instanceof Player player) {
            player.getCapability(MobAbsorbCapability.INSTANCE).ifPresent(data -> {
                data.setHeldEntityId(msg.heldEntityId);
                data.setConsumeMode(msg.consumeMode);
                data.setConsumeProgress(msg.consumeProgress);
                data.setAbsorbedMobType(msg.absorbedMobType);
                data.setAbilityCooldown(msg.abilityCooldown);
            });
        }
    }
}
