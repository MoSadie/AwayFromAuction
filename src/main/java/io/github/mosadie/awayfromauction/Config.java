package io.github.mosadie.awayfromauction;

import net.minecraftforge.common.config.Configuration;

public class Config {
    
    public static final String CATEGORY_GENERAL = "general";
    public static final String CATEGORY_HYPIXEL = "hypixel";
    
    public static int GENERAL_REFRESH_DELAY = 60;
    public static String HYPIXEL_API_KEY = "";

    public static void readConfig() {
        Configuration cfg = AwayFromAuction.config;
        
        try {
            cfg.load();
            initGeneralConfig(cfg);
            initHypixelConfig(cfg);
        } catch(Exception e) {
            AwayFromAuction.getLogger().error("Problem loading config!", e);
        } finally {
            if (cfg.hasChanged()) {
                cfg.save();
            }
        }
    }

    private static void initGeneralConfig(Configuration cfg) {
        cfg.addCustomCategoryComment(CATEGORY_GENERAL, "General Settings");
        GENERAL_REFRESH_DELAY = cfg.getInt("refreshDelay", CATEGORY_GENERAL, 60, 1, Integer.MAX_VALUE, "Delay in seconds between refreshing data from Hypixel's API");
    }

    private static void initHypixelConfig(Configuration cfg) {
        cfg.addCustomCategoryComment(CATEGORY_HYPIXEL, "Hypixel API Settings");
        HYPIXEL_API_KEY = cfg.getString("apiKey", CATEGORY_HYPIXEL, "", "Hypixel API Key");
    }
}