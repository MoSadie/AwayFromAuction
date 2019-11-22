package io.github.mosadie.awayfromauction;

import java.nio.file.Path;
import java.util.NoSuchElementException;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod.EventBusSubscriber
public class Config {

    public static final String CATEGORY_GENERAL = "general";
    public static final String CATEGORY_HYPIXEL = "hypixel";

    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

    public static ForgeConfigSpec CLIENT_CONFIG;

    public static ForgeConfigSpec.IntValue GENERAL_REFRESH_DELAY;
    public static ForgeConfigSpec.ConfigValue<String> HYPIXEL_API_KEY;

    static {
        CLIENT_BUILDER.comment("General Settings").push(CATEGORY_GENERAL);
        GENERAL_REFRESH_DELAY = CLIENT_BUILDER.comment("Delay in seconds between refreshing data from Hypixel's API").defineInRange("refreshDelay", 60, 1, Integer.MAX_VALUE);
        CLIENT_BUILDER.pop();
        CLIENT_BUILDER.comment("Hypixel API Settings").push(CATEGORY_HYPIXEL);
        HYPIXEL_API_KEY = CLIENT_BUILDER.comment("Hypixel API Key").define("apiKey", "");
        CLIENT_BUILDER.pop();

        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }

    public static void loadConfig(ForgeConfigSpec spec, Path path) {

        final CommentedFileConfig configData = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build();

        configData.load();
        spec.setConfig(configData);
    }

    @SubscribeEvent
    public static void onLoad(final ModConfig.Loading configEvent) {
        try {
            ((AwayFromAuction) ModList.get().getModContainerById(AwayFromAuction.MOD_ID).get().getMod()).refreshHypixelApi();
        } catch(NoSuchElementException e) {
            // Cry?
        }
    }

    @SubscribeEvent
    public static void onReload(final ModConfig.ConfigReloading configEvent) {
        try {
            ((AwayFromAuction) ModList.get().getModContainerById(AwayFromAuction.MOD_ID).get().getMod()).refreshHypixelApi();
        } catch(NoSuchElementException e) {
            // Cry?
        }
    }
}