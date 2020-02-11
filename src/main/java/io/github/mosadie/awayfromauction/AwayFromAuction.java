package io.github.mosadie.awayfromauction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.mosadie.awayfromauction.util.AfAUtils;
import io.github.mosadie.awayfromauction.util.Auction;
import net.hypixel.api.HypixelAPI;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = AwayFromAuction.MOD_ID, name = "AwayFromAuction", version = "1.0.0", acceptedMinecraftVersions = "1.8.9", clientSideOnly = true, useMetadata = true, updateJSON = "https://raw.githubusercontent.com/MoSadie/AwayFromAuction/master/updateJSON.json")
public class AwayFromAuction {
    public static final String MOD_ID = "awayfromauction";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Gson GSON = new GsonBuilder().create();

    private SyncThread syncThread;

    private HypixelAPI hypixelApi;
    private HttpClient httpClient = HttpClientBuilder.create().build();

    public static Configuration config;

    public AwayFromAuction() {
        LOGGER.debug("Setting up maps");
        nameCache = new HashMap<>();
        uuidCache = new HashMap<>();

        LOGGER.debug("Map setup complete!");
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.debug("Reading/Setting up config file");
        File directory = event.getModConfigurationDirectory();
        config = new Configuration(new File(directory.getPath(), "awayfromauction.cfg"));
        Config.readConfig();

        LOGGER.debug("Registering Client Command");
        ClientCommandHandler.instance.registerCommand(new AfACommand(this));

        LOGGER.debug("Registering new ClientEventHandler");
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler(this));
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        if (config.hasChanged()) {
            config.save();
        }

        if (!Config.HYPIXEL_API_KEY.equals("")) {
            refreshHypixelApi();
        }
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    // public static ChatComponentTranslation getTranslatedTextComponent(String key, Object... args) {
    //     return new ChatComponentTranslation(MOD_ID + "." + key, args);
    // }

    public static ChatComponentText getTranslatedTextComponent(String key, Object... args) {
        String translated = StatCollector.translateToLocalFormatted(MOD_ID + "." + key, args);
        return new ChatComponentText(translated);
    }

    /**
     * Safely validates if the apiKey is in the correct form. In this case that is
     * UUID.
     * 
     * @param apiKey The apiKey to validate.
     * @return True if the apiKey is in the correct form, false otherwise.
     */
    public boolean validateAPIKey(String apiKey) {
        try {
            UUID.fromString(apiKey);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    /**
     * Tests the API key using the Hypixel API. It is recommended to use
     * {@link #validateAPIKey(String)} first to validate the key first.
     * 
     * @param apiKeyString The Hypixel API key to test.
     * @return True if the API key works, false otherwise.
     */
    public boolean testAPIKey(String apiKeyString) {
        if (!validateAPIKey(apiKeyString)) {
            LOGGER.warn("Invalid API key attempted to be tested! Key: " + apiKeyString);
            return false;
        }
        UUID apiKey = UUID.fromString(apiKeyString);
        HypixelAPI tmpApi = new HypixelAPI(apiKey);

        try {
            tmpApi.getPlayerByUuid(UUID.fromString("698fffb6-be83-4fb9-b80e-d799c18b53b5")).get(1, TimeUnit.MINUTES);
            LOGGER.info("API key test passed!");
        } catch (Exception e) {
            LOGGER.warn("API key test failed! Exception: " + e.getLocalizedMessage());
            LOGGER.catching(e);
            return false;
        }
        return true;
    }

    /**
     * Sets the Hypixel API key in the config file and refreshes our connection to
     * the HypixelAPI
     * 
     * @param apiKey The new HypixelAPI key to use.
     * @return True if the key works and nothing goes wrong refreshing our
     *         connection, false otherwise.
     */
    public boolean setAPIKey(String apiKey) {
        if (testAPIKey(apiKey)) {
            LOGGER.info("Setting new Hypixel API key");
            Config.setHypixelKey(apiKey);
            return refreshHypixelApi();
        }
        return false;
    }

    /**
     * Refreshes the HypixelAPI object we use to connect to the Hypixel API. Called
     * when the API key is loaded/changed.
     * 
     * @return True if the key works, false otherwise.
     */
    boolean refreshHypixelApi() {
        if (testAPIKey(Config.HYPIXEL_API_KEY)) {
            hypixelApi = new HypixelAPI(UUID.fromString(Config.HYPIXEL_API_KEY));
            LOGGER.info("Refreshed Hypixel API Object!");
            return true;
        } else {
            LOGGER.error("Failed to refresh Hypixel API Object! API test failed!");
            return false;
        }
    }

    

    private Map<UUID, String> nameCache;

    /**
     * Converts a player UUID to their Username.
     * 
     * @param uuid UUID of a player
     * @return Their current Username or "ERROR"
     */
    public String getPlayerName(UUID uuid) {
        if (nameCache.containsKey(uuid)) {
            return nameCache.get(uuid);
        }

        try {
            String url = "https://www.mc-heads.net/minecraft/profile/" + uuid.toString();
            HttpResponse response = httpClient.execute(new HttpGet(url));
            String content = EntityUtils.toString(response.getEntity(), "UTF-8");
            MCHeadsResponse mcHeadsResponse = GSON.fromJson(content, MCHeadsResponse.class);

            nameCache.put(uuid, mcHeadsResponse.getName());
            uuidCache.put(mcHeadsResponse.getName(), uuid);
            return mcHeadsResponse.getName();
        } catch (IOException e) {
            LOGGER.error("Exception encountered asking Mojang for player name! Exception: " + e.getLocalizedMessage());
            LOGGER.catching(Level.ERROR, e);
            return "ERROR";
        } catch (Exception e) {
            LOGGER.error("Exception encountered getting player name! Exception: " + e.getLocalizedMessage());
            LOGGER.catching(Level.ERROR, e);
            return "ERROR";
        }
    }

    private class MCHeadsResponse {
        private MCHeadsNameResponse[] name_history;
        private String name;
        private String id;
        private MCHeadsPropertyResponse[] properties;

        public String[] getNameHistory() {
            List<String> nameHistoryList = new ArrayList<>();
            for (MCHeadsNameResponse nameResponse : name_history) {
                nameHistoryList.add(nameResponse.getName());
            }
            return nameHistoryList.toArray(new String[0]);
        }

        public String getName() {
            return name;
        }

        public UUID getUUID() {
            return UUID.fromString(AfAUtils.addHyphens(id));
        }

        public Map<String,String> getProperties() {
            Map<String,String> propertiesMap = new HashMap<>();
            for (MCHeadsPropertyResponse propertyResponse: properties) {
                propertiesMap.put(propertyResponse.name, propertyResponse.value);
            }
            return propertiesMap;
        }
    }

    private class MCHeadsNameResponse {
        private String name;

        public String getName() {
            return name;
        }
    }

    private class MCHeadsPropertyResponse {
        private String name;
        private String value;

        public String getName() {
            return name;
        }
        
        public String getValue() {
            return value;
        }
    }

    private Map<String, UUID> uuidCache;

    /**
     * Converts a player Username to a UUID.
     * 
     * @param name Username of a player.
     * @return Their UUID or null.
     */
    public UUID getPlayerUUID(String name) {
        name = name.toLowerCase();
        if (uuidCache.containsKey(name)) {
            return uuidCache.get(name);
        }

        try {
            String url = "https://www.mc-heads.net/minecraft/profile/" + name;
            HttpResponse response = httpClient.execute(new HttpGet(url));
            String content = EntityUtils.toString(response.getEntity(), "UTF-8");
            MCHeadsResponse mcHeadsResponse = GSON.fromJson(content, MCHeadsResponse.class);

            nameCache.put(mcHeadsResponse.getUUID(), name);
            uuidCache.put(name, mcHeadsResponse.getUUID());
            return mcHeadsResponse.getUUID();
        } catch (IOException e) {
            LOGGER.error("Exception encountered asking Mojang for player UUID! Exception: " + e.getLocalizedMessage());
            LOGGER.catching(Level.ERROR, e);
            return null;
        } catch (Exception e) {
            LOGGER.error("Exception encountered getting player UUID! Exception: " + e.getLocalizedMessage());
            LOGGER.catching(Level.ERROR, e);
            return null;
        }
    }

    /**
     * Gets an auction's current status (as of last sync) by its UUID.
     * 
     * @param auctionUUID The UUID of the auction to get state of.
     * @return The specified auction's current state or a special Error Auction.
     */
    public Auction getAuction(UUID auctionUUID) {
        if (syncThread.getAllAuctions().containsKey(auctionUUID)) {
            return syncThread.getAllAuctions().get(auctionUUID);
        } else {
            return Auction.ERROR_AUCTION;
        }
    }

    /**
     * Gets all auctions owner by a player.
     * 
     * @param playerUUID The UUID of the auction owner.
     * @return Array of current auction states of all auctions owner by the
     *         specified player.
     */
    public Auction[] getAuctionsByPlayer(UUID playerUUID) {
        return syncThread.getPlayerAuctions(playerUUID).toArray(new Auction[0]);
    }

    /**
     * @return An array containing all currently known auctions' states.
     */
    public Auction[] getAuctions() {
        return syncThread.getAllAuctions().values().toArray(new Auction[0]);
    }

    /**
     * @return An array of Strings with all currently known items up for auction.
     */
    public String[] getAuctionItems() {
        return syncThread.getAllItems();
    }

    /**
     * Checks if item string exactly matches an item up for auction
     * 
     * @param item Auction item name
     * @return True if at item name matches an auction item, false otherwise.
     */
    public boolean isAuctionItem(String item) {
        List<String> items = new ArrayList<>();
        Collections.addAll(items, syncThread.getAllItems());
        return items.contains(item.toLowerCase());
    }

    /**
     * Gets all known auction items that contain the specified string.
     * 
     * @param filter The string to filter the results.
     * @return Array with all currently known auction items that contain the filter
     *         string.
     */
    public String[] getAuctionItems(String filter) {
        List<String> result = new ArrayList<>();
        for (String item : syncThread.getAllItems()) {
            if (item.contains(filter.toLowerCase())) {
                if (syncThread.getItemAuctions(item).size() > 0) {
                    result.add(syncThread.getItemAuctions(item).get(0).getItemName());
                } else {
                    result.add(item);
                }
            }
        }
        return result.toArray(new String[0]);
    }

    /**
     * Gets all auctions that contain the specified item.
     * 
     * @param itemName The name of the item being auctioned.
     * @return Array of current auction states for all auctions of that item.
     */
    public Auction[] getAuctionsByItem(String itemName) {
        return syncThread.getItemAuctions(itemName).toArray(new Auction[0]);
    }

    /**
     * @return Array of current auction states for all active auctions currently bid
     *         on by the player.
     */
    public Auction[] getBidOnAuctions() {
        return syncThread.getBidOnAuctions().toArray(new Auction[0]);
    }

    /**
     * @return The current total number of coins sitting in the auction house in
     *         bids.
     */
    public long getTotalCoins() {
        return syncThread.getTotalCoins();
    }

    /**
     * Refreshes the HypixelAPI object if it's not been initialized yet.
     * 
     * @return The current/new HypixelAPI object or null if something went wrong.
     */
    HypixelAPI getOrRefreshHypixelAPI() {
        if (hypixelApi == null) {
            if (!refreshHypixelApi()) {
                LOGGER.error("Something went wrong refreshing Hypixel API object!");
                return null;
            }
        }
        return hypixelApi;
    }

    void createSyncThread() {
        if (syncThread == null) {
            LOGGER.debug("Sync thread created!");
            syncThread = new SyncThread(this);
            LOGGER.info("Starting sync thread");
            syncThread.start();
        }
    }

    void stopSyncThread() {
        if (syncThread != null) {
            LOGGER.debug("Stopping sync thread");
            syncThread.stopFlag();
            syncThread.interrupt();
            syncThread = null;
        }
    }
}