package com.mobabsorb.client;

import com.mobabsorb.capability.MobAbsorbCapability;
import com.mobabsorb.capability.MobAbsorbData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.registries.ForgeRegistries;

public class MobAbsorbHudOverlay {

    public static final IGuiOverlay OVERLAY = (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        player.getCapability(MobAbsorbCapability.INSTANCE).ifPresent(data -> {
            // 1. Show active absorbed mob power in top-left corner
            ResourceLocation mobTypeKey = data.getAbsorbedMobType();
            if (mobTypeKey != null) {
                EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(mobTypeKey);
                Component mobName = type != null ? type.getDescription() : Component.literal(mobTypeKey.getPath());
                Component powerText = Component.translatable("hud.mobabsorb.active_power", mobName);

                int cd = data.getAbilityCooldown();
                Component statusText = cd <= 0
                        ? Component.translatable("hud.mobabsorb.ready")
                        : Component.translatable("hud.mobabsorb.cd", (int) Math.ceil(cd / 20.0));

                int boxWidth = Math.max(mc.font.width(powerText), mc.font.width(statusText)) + 14;
                guiGraphics.fill(8, 8, 8 + boxWidth, 36, 0xAA101418);
                guiGraphics.drawString(mc.font, powerText, 14, 13, 0xFFE6F4EA);
                guiGraphics.drawString(mc.font, statusText, 14, 24, cd <= 0 ? 0xFF55FF55 : 0xFFFFAA00);
            }

            // 2. Show held mob & eating / swamp-absorbing progress bar in center-bottom
            if (data.isHoldingMob() && mc.level != null) {
                Entity held = mc.level.getEntity(data.getHeldEntityId());
                if (held != null) {
                    int centerX = screenWidth / 2;
                    int barY = screenHeight - 62;

                    Component label;
                    int barColor;
                    if (data.getConsumeMode() == MobAbsorbData.MODE_EAT) {
                        label = Component.translatable("hud.mobabsorb.eating");
                        barColor = 0xFFE87A24; // Warm orange for eating
                    } else if (data.getConsumeMode() == MobAbsorbData.MODE_SWAMP_ABSORB) {
                        label = Component.translatable("hud.mobabsorb.absorbing");
                        barColor = 0xFF2B8A6E; // Deep swamp teal-green for swamp absorption
                    } else {
                        label = Component.translatable("hud.mobabsorb.holding", held.getDisplayName());
                        barColor = 0xFFAAAAAA;
                    }

                    int textW = mc.font.width(label);
                    guiGraphics.drawString(mc.font, label, centerX - textW / 2, barY - 12, 0xFFFFFFFF);

                    if (data.getConsumeMode() != MobAbsorbData.MODE_NONE) {
                        int barW = 120;
                        int barH = 7;
                        int left = centerX - barW / 2;
                        guiGraphics.fill(left - 1, barY - 1, left + barW + 1, barY + barH + 1, 0xCC000000);
                        int fillW = (int) (barW * data.getConsumeRatio());
                        guiGraphics.fill(left, barY, left + fillW, barY + barH, barColor);
                    }
                }
            }
        });
    };

    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("mob_absorb_hud", OVERLAY);
    }
}
