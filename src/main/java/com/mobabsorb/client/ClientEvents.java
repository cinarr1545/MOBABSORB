package com.mobabsorb.client;

import com.mobabsorb.capability.MobAbsorbCapability;
import com.mobabsorb.capability.MobAbsorbData;
import com.mobabsorb.network.ConsumeStatePacket;
import com.mobabsorb.network.ModNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientEvents {
    private int lastSentMode = MobAbsorbData.MODE_NONE;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null) return;

        // Active power keybind (R)
        while (KeyBindings.USE_POWER.consumeClick()) {
            ModNetwork.sendToServer(new ConsumeStatePacket(ConsumeStatePacket.ACTION_USE_POWER, 0));
        }

        // Release held mob keybind (G)
        while (KeyBindings.RELEASE_MOB.consumeClick()) {
            ModNetwork.sendToServer(new ConsumeStatePacket(ConsumeStatePacket.ACTION_RELEASE, 0));
        }

        player.getCapability(MobAbsorbCapability.INSTANCE).ifPresent(data -> {
            if (!data.isHoldingMob()) {
                lastSentMode = MobAbsorbData.MODE_NONE;
                return;
            }

            boolean rightClickDown = mc.options.keyUse.isDown();
            boolean shiftDown = player.isShiftKeyDown();
            boolean swampKeyDown = KeyBindings.SWAMP_ABSORB.isDown();

            int desiredMode = MobAbsorbData.MODE_NONE;
            if (swampKeyDown || (rightClickDown && shiftDown)) {
                // Mode 2: Swamp-like body absorption (Shift+Right-Click or V)
                desiredMode = MobAbsorbData.MODE_SWAMP_ABSORB;
            } else if (rightClickDown) {
                // Mode 1: Animated eating (Right-Click hold)
                desiredMode = MobAbsorbData.MODE_EAT;
            }

            if (desiredMode != lastSentMode) {
                lastSentMode = desiredMode;
                ModNetwork.sendToServer(new ConsumeStatePacket(ConsumeStatePacket.ACTION_SET_MODE, desiredMode));
            }
        });
    }

    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        LivingEntity rendered = event.getEntity();
        // Check if any player in the level is currently holding this entity
        for (Player player : mc.level.players()) {
            MobAbsorbData data = player.getCapability(MobAbsorbCapability.INSTANCE).orElse(null);
            if (data == null || data.getHeldEntityId() != rendered.getId()) continue;

            PoseStack poseStack = event.getPoseStack();
            float ratio = data.getConsumeRatio();

            if (data.getConsumeMode() == MobAbsorbData.MODE_EAT) {
                // Eating animation: mob shrinks slightly as bites are taken and shakes/tilts dynamically
                float time = (rendered.tickCount + event.getPartialTick()) * 1.6f;
                float shakeX = (float) Math.sin(time * 2.5f) * 0.06f;
                float shakeY = (float) Math.cos(time * 3.0f) * 0.05f;
                float scale = Math.max(0.25f, 0.72f - (ratio * 0.45f));

                poseStack.translate(shakeX, shakeY, 0.0);
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(time) * 14.0f));
                poseStack.scale(scale, scale, scale);
            } else if (data.getConsumeMode() == MobAbsorbData.MODE_SWAMP_ABSORB) {
                // Swamp absorption animation: mob gets pulled inward, squished and swallowed into the player's body!
                float wobble = (float) Math.sin((rendered.tickCount + event.getPartialTick()) * 0.8f) * 0.04f;
                float shrink = Math.max(0.05f, 0.75f * (1.0f - ratio * 0.92f));
                float stretchZ = Math.max(0.05f, shrink * (1.0f - ratio * 0.35f));

                poseStack.translate(wobble, -ratio * 0.25, 0.0);
                poseStack.mulPose(Axis.YP.rotationDegrees(ratio * 180.0f));
                poseStack.scale(shrink, shrink * (1.0f + ratio * 0.15f), stretchZ);
            } else {
                // Hugged / carried state: slightly scaled down so it fits nicely in player's arms
                poseStack.scale(0.72f, 0.72f, 0.72f);
            }
            break;
        }
    }
}
