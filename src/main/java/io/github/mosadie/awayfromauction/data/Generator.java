package io.github.mosadie.awayfromauction.data;

import io.github.mosadie.awayfromauction.AwayFromAuction;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

@Mod.EventBusSubscriber(modid = AwayFromAuction.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Generator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        if (event.includeServer())
            registerServerProviders(event.getGenerator());

        if (event.includeClient())
            registerClientProviders(event.getGenerator());
    }

    private static void registerServerProviders(DataGenerator generator) {
        //Client side mod, so no server stuff
    }

    private static void registerClientProviders(DataGenerator generator) {
        generator.addProvider(new GeneratorLanguage(generator));
    }
}