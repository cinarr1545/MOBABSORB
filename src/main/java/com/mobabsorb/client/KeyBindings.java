package com.mobabsorb.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final String CATEGORY = "key.categories.mobabsorb";

    public static final KeyMapping USE_POWER = new KeyMapping(
            "key.mobabsorb.use_power",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    public static final KeyMapping SWAMP_ABSORB = new KeyMapping(
            "key.mobabsorb.swamp_absorb",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    public static final KeyMapping RELEASE_MOB = new KeyMapping(
            "key.mobabsorb.release_mob",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(USE_POWER);
        event.register(SWAMP_ABSORB);
        event.register(RELEASE_MOB);
    }
}
