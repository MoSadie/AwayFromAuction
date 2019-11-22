package io.github.mosadie.awayfromauction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.mosadie.awayfromauction.util.Auction;
import net.hypixel.api.HypixelAPI;
import net.hypixel.api.reply.skyblock.SkyBlockAuctionReply;
import net.hypixel.api.reply.skyblock.SkyBlockAuctionsReply;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("awayfromauction")
public class AwayFromAuction {
    public static final String MOD_ID = "awayfromauction";

    private static final Logger LOGGER = LogManager.getLogger();

    private HypixelAPI hypixelApi;

    public AwayFromAuction() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG);

        IEventBus event = FMLJavaModLoadingContext.get().getModEventBus();
        event.addListener(this::setupClient);

        Config.loadConfig(Config.CLIENT_CONFIG, FMLPaths.CONFIGDIR.get().resolve("awayfromauction-client.toml"));

        nameCache = new HashMap<>();
    }

    private void setupClient(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler(this));

        if (!Config.HYPIXEL_API_KEY.get().equals("")) {
            refreshHypixelApi();
        }
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static TranslationTextComponent getTranslatedTextComponent(String key, Object... args) {
        return new TranslationTextComponent(MOD_ID + "." + key, args);
    }

    void sync() {
        // Features list:
        // Your Auction:
        // - New Bid
        // - Almost Done
        // - Done
    }

    public boolean validateAPIKey(String arg) {
        try {
            UUID.fromString(arg);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    public boolean testAPIKey(String arg) {
        UUID apiKey = UUID.fromString(arg);
        HypixelAPI tmpApi = new HypixelAPI(apiKey);

        try {
            tmpApi.getPlayerByUuid(Minecraft.getInstance().player.getUniqueID()).get(1, TimeUnit.MINUTES);
            LOGGER.info("API key test passed!");
        } catch (Exception e) {
            LOGGER.warn("API key test failed! Exception: " + e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    public boolean setAPIKey(String arg) {
        if (testAPIKey(arg)) {
            Config.HYPIXEL_API_KEY.set(arg);
            return refreshHypixelApi();
        }
        return false;
    }

    public boolean refreshHypixelApi() {
        if (testAPIKey(Config.HYPIXEL_API_KEY.get())) {
            hypixelApi = new HypixelAPI(UUID.fromString(Config.HYPIXEL_API_KEY.get()));
            LOGGER.info("Refreshed Hypixel API Object!");
            return true;
        } else {
            LOGGER.error("Failed to refresh Hypixel API Object! API test failed!");
            return false;
        }
    }

    public boolean onHypixel() {
        try {
            return Minecraft.getInstance().getCurrentServerData().serverIP.contains(".hypixel.net");
        } catch (Exception e) {
            return false;
        }
    }

    private Map<UUID, String> nameCache;

    public String getPlayerName(UUID uuid) {
        if (nameCache.containsKey(uuid)) {
            return nameCache.get(uuid);
        }

        try {
            JsonObject player = hypixelApi.getPlayerByUuid(uuid).get(1, TimeUnit.MINUTES).getPlayer();
            
            if (player != null) {
                nameCache.put(uuid, player.get("displayname").getAsString());
                return player.get("displayname").getAsString();
            }
            else
                return "";
        } catch (InterruptedException e) {
            return "";
        } catch (ExecutionException e) {
            return "";
        } catch (TimeoutException e) {
            return "";
        }
    }

    public Auction getAuction(UUID auctionUUID) {
        try {
            if (hypixelApi == null) {
                if (!refreshHypixelApi()) {
                    LOGGER.error("Something went wrong refreshing Hypixel API object!");
                    return Auction.ERROR_AUCTION;
                }
            }

            SkyBlockAuctionReply reply = hypixelApi.getSkyblockAuctionsByUUID(auctionUUID).get(1, TimeUnit.MINUTES);
            LOGGER.info(reply.toString());
            if (reply.getAuctions().size() > 0) {
                return new Auction(reply.getAuctions().get(0).getAsJsonObject(), this);
            } else {
                LOGGER.warn("Attempted to get auction, but no auction found!");
                return Auction.ERROR_AUCTION;
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return Auction.ERROR_AUCTION;
        }
        
	}

	public Auction[] getAuctions() {
		try {
            if (hypixelApi == null) {
                if (!refreshHypixelApi()) {
                    LOGGER.error("Something went wrong refreshing Hypixel API object!");
                    return new Auction[] {Auction.ERROR_AUCTION};
                }
            }

            SkyBlockAuctionsReply reply = hypixelApi.getSkyBlockAuctions(0).get(1, TimeUnit.MINUTES);
            LOGGER.info(reply.toString());
            if (reply.getAuctions().size() > 0) {
                Auction[] auctions = new Auction[reply.getAuctions().size()];
                for (int i = 0; i < reply.getAuctions().size(); i++) {
                    auctions[i] = new Auction(reply.getAuctions().get(i).getAsJsonObject(), this);
                }
                return auctions;
            } else {
                LOGGER.warn("Attempted to get auctions, but no auctions found!");
                return new Auction[] {Auction.ERROR_AUCTION};
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return new Auction[] {Auction.ERROR_AUCTION};
        }
	}
}