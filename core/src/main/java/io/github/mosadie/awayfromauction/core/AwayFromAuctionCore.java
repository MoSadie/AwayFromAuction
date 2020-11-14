package io.github.mosadie.awayfromauction.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.mosadie.awayfromauction.core.Auction.Bid;
import net.hypixel.api.HypixelAPI;
import net.hypixel.api.reply.skyblock.BazaarReply;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AwayFromAuctionCore {

    private final Gson GSON;

    private final IAwayFromAuction afa;
    private final File syncCache;

    private HypixelAPI hypixelAPI;

    private SyncThread syncThread;

    private final HttpClient httpClient = HttpClientBuilder.create().build();

    private final Map<String, UUID> playerUUIDMap;
    private final Map<UUID, String> playerNameMap;

    private Map<UUID, Auction> allAuctions;
    private Map<UUID, List<Auction>> playerAuctionMap;
    private Map<String, List<Auction>> itemAuctionMap;
    private List<UUID> bidAuctions;
    private long totalCoins;

    private Map<String, BazaarReply.Product> bazaarMap;
    private final Map<String, SkyblockItem> itemMap;

    public AwayFromAuctionCore(IAwayFromAuction mod, File syncCache) {
        this.afa = mod;
        this.syncCache = syncCache;

        playerUUIDMap = new HashMap<>();
        playerNameMap = new HashMap<>();


        allAuctions = new HashMap<>();
        playerAuctionMap = new HashMap<>();
        itemAuctionMap = new HashMap<>();
        bidAuctions = new ArrayList<>();
        totalCoins = 0;

        bazaarMap = new TreeMap<>();

        GSON = new GsonBuilder()
            .registerTypeAdapter(Auction[].class, new AuctionJsonDeserializer(mod))
            .registerTypeAdapter(Auction[].class, new AuctionJsonSerializer()).create();

        getStartingNameCache();

        itemMap = getSkyblockItemMap();
    }

    private Map<String,SkyblockItem> getSkyblockItemMap() {
        afa.logInfo("Attempting to fetch Skyblock item friendly names from Slothpixel.");
        try {
            String url = "https://api.slothpixel.me/api/skyblock/items";
            HttpResponse response = httpClient.execute(new HttpGet(url));
            Type responseType = new TypeToken<Map<String, SkyblockItem>>() {}.getType();
            String content = EntityUtils.toString(response.getEntity(), "UTF-8");

            afa.logInfo("Successfully fetched Skyblock item friendly names.");
            return GSON.fromJson(content, responseType);
        } catch (IOException e) {
            afa.logError("IOException encountered getting Skyblock items from Slothpixel! Exception: "
                    + e.getLocalizedMessage());
            afa.logException(e);
            return null;
        } catch (Exception e) {
            afa.logError("Exception encountered getting Skyblock items from Slothpixel! Exception: "
                    + e.getLocalizedMessage());
            afa.logException(e);
            return null;
        }
    }

    private void getStartingNameCache() {
        afa.logInfo("Attempting to fetch starting UUID cache from https://github.com/MoSadie/AwayFromAuction-Cache.");
        try {
            String url = "https://raw.githubusercontent.com/MoSadie/AwayFromAuction-Cache/master/docs/usernames.json";
            HttpResponse response = httpClient.execute(new HttpGet(url));
            Type responseType = new TypeToken<Map<String, String>>() {}.getType();
            String content = EntityUtils.toString(response.getEntity(), "UTF-8");

            Map<String, String> startNameMap = GSON.fromJson(content, responseType);
            for (String uuidString : startNameMap.keySet()) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    String username = startNameMap.get(uuidString);

                    playerUUIDMap.put(username.toLowerCase(), uuid);
                    playerNameMap.put(uuid, username);
                } catch (IllegalArgumentException e) {
                    afa.logWarn("A malformed UUID was encountered loading the starting cache! UUID: " + uuidString);
                }
            }
            afa.logInfo("Finished fetching starting UUID cache.");
        } catch (IOException e) {
            afa.logError("IOException encountered getting starting name cache! Exception: "
                    + e.getLocalizedMessage());
            afa.logException(e);
        } catch (Exception e) {
            afa.logError("Exception encountered getting starting name cache! Exception: "
                    + e.getLocalizedMessage());
            afa.logException(e);
        }
    }

    public SkyblockItem getItem(String itemId) {
        if (itemId == null || itemMap == null || itemMap.isEmpty() || !itemMap.containsKey(itemId.toUpperCase())) {
            return null;
        }

        return itemMap.get(itemId.toUpperCase());
    }

    public String getItemName(String itemId) {
        SkyblockItem item = getItem(itemId);
        if (item != null) {
            return item.getName();
        } else {
            return itemId;
        }
    }

    public File getSyncCache() {
        return syncCache;
    }

    IAwayFromAuction getAfA() {
        return afa;
    }

    /**
     * Returns a map of all current auctions. The mapping is from Auction UUID ->
     * Auction object.
     * 
     * @return A map from auction UUID to an Auction object
     */
    public Map<UUID, Auction> getAllAuctions() {
        return allAuctions;
    }

    /**
     * Returns a list of active/unclaimed auctions owned by a player.
     * 
     * @param name The username of the player to get auctions for.
     * @return A list of active/unclaimed auctions owned by the specified player.
     */
    public List<Auction> getPlayerAuctions(String name) {
        UUID uuid = getCachedPlayerUUID(name);
        return getPlayerAuctions(uuid);
    }

    /**
     * Returns a list of active/unclaimed auctions owned by a player.
     * 
     * @param uuid The UUID of the player to get auctions for.
     * @return A list of active/unclaimed auctions owned by the specified player.
     */
    public List<Auction> getPlayerAuctions(UUID uuid) {
        if (uuid == null) {
            afa.logWarn("Attempted to get auctions for non existing player!");
            return new ArrayList<>();
        }

        if (playerAuctionMap.containsKey(uuid)) {
            return playerAuctionMap.get(uuid);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * @return An array of all known items that are up for auction.
     */
    public String[] getAllItems() {
        return itemAuctionMap.keySet().toArray(new String[0]);
    }

    /**
     * Checks if an item matches the name of any item up for auction.
     * 
     * @param item the name of the item to check. Case insensitive.
     * @return True if the item exists in the auction house, false otherwise.
     */
    public boolean isItem(String item) {
        item = item.toLowerCase();
        return !itemAuctionMap.containsKey(item);
    }

    /**
     * Returns a list of all active/unclaimed auctions for a specified item.
     * 
     * @param item The case-insensitive name of the item.
     * @return A list of all the active/unclaimed auctions for the specified item.
     */
    public List<Auction> getItemAuctions(String item) {
        if (itemAuctionMap.containsKey(item.toLowerCase())) {
            return itemAuctionMap.get(item.toLowerCase());
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * @return A list of all active/unclaimed auctions the player has bid on.
     */
    public List<Auction> getBidOnAuctions() {
        List<Auction> list = new ArrayList<>();
        for (UUID auctionUUID : bidAuctions) {
            list.add(allAuctions.get(auctionUUID));
        }
        return list;
    }

    /**
     * @return The total number of coins currently in bids in the auction house.
     */
    public long getTotalCoins() {
        return totalCoins;
    }

    /**
     * Gets the current state of the Bazaar.
     * 
     * @return A mapping from a bazaar product's name to the {@link BazaarReply.Product} object.
     */
    public Map<String, BazaarReply.Product> getBazaarProducts() {
        return bazaarMap;
    }

    void setBazaarState(Map<String, BazaarReply.Product> state) {
        if (state != null)
            this.bazaarMap = state;
    }

    void setAuctionState(Map<UUID, Auction> allAuctions, Map<UUID, List<Auction>> playerAuctionMap, Map<String, List<Auction>> itemAuctionMap, List<UUID> bidAuctions, long totalCoins) {
        if (allAuctions == null || playerAuctionMap == null || itemAuctionMap == null || bidAuctions == null) {
            return;
        }

        this.allAuctions = allAuctions;
        this.playerAuctionMap = playerAuctionMap;
        this.itemAuctionMap = itemAuctionMap;
        this.bidAuctions = bidAuctions;
        this.totalCoins = totalCoins;
    }

    /**
     * Saves the current auctions to the cache file.
     */
    void saveToCache() {
        Auction[] auctions = getAllAuctions().values().toArray(new Auction[0]);
        String json = GSON.toJson(auctions);

        try {
            boolean createdFile = syncCache.createNewFile();

            if (createdFile) {
                afa.logInfo("Created new auction sync cache file!");
            }

            OutputStreamWriter outputWriter = new OutputStreamWriter(new FileOutputStream(syncCache));
            outputWriter.write(json);
            outputWriter.close();
        } catch (IOException e) {
            afa.logError("Exception occurred saving cache: " + e.getLocalizedMessage());
            afa.logException(e);
        }
    }

    /**
     * Loads the auctions from the cache file.
     */
    void loadFromCache() {
        if (!syncCache.exists()) {
            afa.logWarn("Cache Not Found!");
            return;
        }

        try {
            Scanner scanner = new Scanner(syncCache);
            String content = scanner.nextLine();
            scanner.close();

            afa.logInfo("Cache Content Length: " + content.length());

            Auction[] auctions = GSON.fromJson(content, Auction[].class);

            afa.logInfo("Cache Auction Number: " + auctions.length);

            Map<UUID, Auction> tmpAllAuctions = new HashMap<>();
            Map<UUID, List<Auction>> tmpPlayerAuctionMap = new HashMap<>();
            Map<String, List<Auction>> tmpItemAuctionMap = new HashMap<>();
            List<UUID> tmpBidAuctions = new ArrayList<>();
            long tmpTotalCoins = 0;

            for (Auction auction : auctions) {
                tmpAllAuctions.put(auction.getAuctionUUID(), auction);

                if (!tmpPlayerAuctionMap.containsKey(auction.getAuctionOwnerUUID())) {
                    tmpPlayerAuctionMap.put(auction.getAuctionOwnerUUID(), new ArrayList<>());
                }
                tmpPlayerAuctionMap.get(auction.getAuctionOwnerUUID()).add(auction);

                if (!tmpItemAuctionMap.containsKey(auction.getItemName().toLowerCase())) {
                    tmpItemAuctionMap.put(auction.getItemName().toLowerCase(), new ArrayList<>());
                }
                tmpItemAuctionMap.get(auction.getItemName().toLowerCase()).add(auction);

                if (AfAUtils.bidsContainUUID(auction.getBids(), afa.getCurrentPlayerUUID()))
                    if (!tmpBidAuctions.contains(auction.getAuctionUUID()))
                        tmpBidAuctions.add(auction.getAuctionUUID());

                for (Bid bid : auction.getBids()) {
                    tmpTotalCoins += bid.getAmount();
                }
            }

            allAuctions = tmpAllAuctions;
            playerAuctionMap = tmpPlayerAuctionMap;
            itemAuctionMap = tmpItemAuctionMap;
            bidAuctions = tmpBidAuctions;
            totalCoins = tmpTotalCoins;

        } catch (FileNotFoundException e) {
            afa.logError("Exception occurred loading from cache: " + e.getLocalizedMessage());
            afa.logException(e);
        }
    }

    /**
     * Get the player name from cache using their UUID. Returns null if not cached.
     * @param uuid The UUID of the player to look up.
     * @return The username of the player as a String if the user is cached, null otherwise.
     */
    public String getCachedPlayerName(UUID uuid) {
        if (playerNameMap.containsKey(uuid)) {
            return playerNameMap.get(uuid);
        }

        return null;
    }

    /**
     * Get the player UUID from cache using their username. Returns null if not cached.
     * @param username The username of the player to look up.
     * @return The UUID of the player if the user is cached, null otherwise.
     */
    public UUID getCachedPlayerUUID(String username) {
        if (playerUUIDMap.containsKey(username.toLowerCase())) {
            return playerUUIDMap.get(username.toLowerCase());
        }

        return null;
    }

    /**
     * Get a player's username from their UUID. Will first return from cache, then fetch from the internet.
     * @param uuid The UUID of the player.
     * @return The name of the player or null if something went wrong.
     */
    public String getPlayerName(UUID uuid) {
        if (getCachedPlayerName(uuid) != null) {
            return getCachedPlayerName(uuid);
        }

        fetchPlayer(uuid.toString());

        if (playerNameMap.containsKey(uuid)) {
            return playerNameMap.get(uuid);
        }

        return null;
    }

    /**
     * Get a player UUID from their username. Will first return from cache, then fetch from the internet.
     * @param username The username of the player.
     * @return The UUID of the player or null if something went wrong.
     */
    public UUID getPlayerUUID(String username) {
        if (getCachedPlayerUUID(username) != null) {
            return getCachedPlayerUUID(username);
        }

        fetchPlayer(username);

        if (playerUUIDMap.containsKey(username.toLowerCase())) {
            return playerUUIDMap.get(username.toLowerCase());
        }

        return null;
    }

    /**
     * Check if a username is in the user cache.
     * @param username The username of the player to check.
     * @return True if the player is in the cache, false otherwise.
     */
    public boolean isPlayerCached(String username) {
        return playerUUIDMap.containsKey(username.toLowerCase());
    }

    /**
     * Check if a UUID is in the user cache.
     * @param uuid The UUID of the player to check.
     * @return True if the player is in the cache, false otherwise.
     */
    public boolean isPlayerCached(UUID uuid) {
        return playerNameMap.containsKey(uuid);
    }

    /**
     * If not already cached, fetches the username for the specified player and caches it. Runs async.
     * @param uuid The uuid of the player to cache.
     */
    public void cachePlayer(UUID uuid) {
        if (!playerNameMap.containsKey(uuid)) {
            playerNameMap.put(uuid, uuid.toString()); //Block multiple cachePlayer calls with the same UUID from leading to multiple fetches.
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> fetchPlayer(uuid.toString()));
        }
    }

    /**
     * If not already cached, fetches the UUID for the specified player and caches it. Runs async.
     * @param username The username of the player to cache.
     */
    public void cachePlayer(String username) {
        if (!playerUUIDMap.containsKey(username.toLowerCase())) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> fetchPlayer(username));
        }
    }

    private void fetchPlayer(String user) {
            try {
                String url = "https://api.ashcon.app/mojang/v2/user/" + user;
                HttpResponse httpResponse = httpClient.execute(new HttpGet(url));
                if (httpResponse.getEntity() == null)
                    throw new NullPointerException("Profile response entity was null!");
                String content = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                AshconResponse response = GSON.fromJson(content, AshconResponse.class);
                if (response.error != null) {
                    throw new IOException("Error occurred getting username/uuid: " + response.error + " Details: " + response.reason);
                }

                playerNameMap.put(UUID.fromString(response.uuid), response.username);
                playerUUIDMap.put(response.username.toLowerCase(), UUID.fromString(response.uuid));
                afa.logInfo("Fetched Player: " + response.username + " UUID: " + response.uuid); //TODO remove debug
            } catch (IOException e) {
                afa.logError("An exception occurred getting username/uuid for a player!");
                afa.logException(e);
            }
    }

    private class AshconResponse {
        private String uuid;
        private String username;

        private int code;
        private String error;
        private String reason;
    }

    public void startSyncThread() {
        if (syncThread == null) {
            syncThread = new SyncThread(this);
            syncThread.start();
        }
    }

    public void stopSyncThread() {
        if (syncThread != null) {
            syncThread.stopFlag();
            syncThread = null;
        }
    }

    public boolean isFirstSyncDone() {
        return (syncThread != null && syncThread.isFirstSyncDone());
    }

    /**
     * Safely validates if the apiKey is in the correct form. In this case that is
     * UUID.
     *
     * @param apiKey The apiKey to validate.
     * @return True if the apiKey is in the correct form, false otherwise.
     */
    public static boolean validateAPIKey(String apiKey) {
        try {
            //noinspection ResultOfMethodCallIgnored
            UUID.fromString(apiKey);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
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
            afa.logWarn("Invalid API key attempted to be tested! Key: " + apiKeyString);
            return false;
        }
        UUID apiKey = UUID.fromString(apiKeyString);
        HypixelAPI tmpApi = new HypixelAPI(apiKey);

        try {
            tmpApi.getKey().get(1, TimeUnit.MINUTES);
            afa.logInfo("API key test passed!");
        } catch (Exception e) {
            afa.logWarn("API key test failed! Exception: " + e.getLocalizedMessage());
            afa.logException(e);
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
            afa.logInfo("Setting new Hypixel API key");
            afa.setHypixelAPIKey(apiKey);
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
    public boolean refreshHypixelApi() {
        if (testAPIKey(afa.getConfigState().getHypixelAPIKey())) {
            hypixelAPI = new HypixelAPI(UUID.fromString(afa.getConfigState().getHypixelAPIKey()));
            afa.logInfo("Refreshed Hypixel API Object!");
            return true;
        } else {
            afa.logError("Failed to refresh Hypixel API Object! API test failed!");
            return false;
        }
    }

    /**
     * Get the HypixelAPI object. Refreshes the HypixelAPI object if it's not been
     * initialized yet.
     *
     * @return The current/new HypixelAPI object or null if something went wrong.
     */
    public HypixelAPI getOrRefreshHypixelAPI() {
        if (hypixelAPI == null) {
            if (!refreshHypixelApi()) {
                afa.logError("Something went wrong refreshing Hypixel API object!");
                return null;
            }
        }
        return hypixelAPI;
    }
}