package com.mobabsorb;

import com.mobabsorb.capability.MobAbsorbCapability;
import com.mobabsorb.client.ClientEvents;
import com.mobabsorb.client.KeyBindings;
import com.mobabsorb.client.MobAbsorbHudOverlay;
import com.mobabsorb.event.ServerEvents;
import com.mobabsorb.network.ModNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MobAbsorbMod.MOD_ID)
public class MobAbsorbMod {
    public static final String MOD_ID = "mobabsorb";

    public MobAbsorbMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(MobAbsorbCapability::onRegisterCapabilities);

        MinecraftForge.EVENT_BUS.register(new ServerEvents());
        MinecraftForge.EVENT_BUS.register(new MobAbsorbCapability());

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modEventBus.addListener(KeyBindings::onRegisterKeyMappings);
            modEventBus.addListener(MobAbsorbHudOverlay::onRegisterOverlays);
            MinecraftForge.EVENT_BUS.register(new ClientEvents());
        });
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
