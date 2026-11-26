package com.mobabsorb.network;

import com.mobabsorb.capability.MobAbsorbCapability;
import com.mobabsorb.capability.MobAbsorbData;
import com.mobabsorb.event.ServerEvents;
import com.mobabsorb.power.MobPowerManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ConsumeStatePacket {
    public static final int ACTION_SET_MODE = 0;
    public static final int ACTION_RELEASE = 1;
    public static final int ACTION_USE_POWER = 2;

    private final int actionType;
    private final int value;

    public ConsumeStatePacket(int actionType, int value) {
        this.actionType = actionType;
        this.value = value;
    }

    public static void encode(ConsumeStatePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.actionType);
        buf.writeInt(msg.value);
    }

    public static ConsumeStatePacket decode(FriendlyByteBuf buf) {
        return new ConsumeStatePacket(buf.readInt(), buf.readInt());
    }

    public static void handle(ConsumeStatePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            player.getCapability(MobAbsorbCapability.INSTANCE).ifPresent(data -> {
                if (msg.actionType == ACTION_SET_MODE) {
                    if (data.isHoldingMob() && data.getGrabCooldown() <= 0) {
                        int mode = msg.value;
                        if (mode == MobAbsorbData.MODE_NONE || mode == MobAbsorbData.MODE_EAT || mode == MobAbsorbData.MODE_SWAMP_ABSORB) {
                            data.setConsumeMode(mode);
                            MobAbsorbCapability.syncToAll(player);
                        }
                    }
                } else if (msg.actionType == ACTION_RELEASE) {
                    ServerEvents.releaseHeldMob(player, data, true);
                } else if (msg.actionType == ACTION_USE_POWER) {
                    MobPowerManager.activatePower(player, data);
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
