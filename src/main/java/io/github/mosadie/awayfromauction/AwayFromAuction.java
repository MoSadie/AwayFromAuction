package io.github.mosadie.awayfromauction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

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
import io.github.mosadie.awayfromauction.util.Auction.Bid;
import net.hypixel.api.HypixelAPI;
import net.hypixel.api.reply.skyblock.SkyBlockAuctionReply;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = AwayFromAuction.MOD_ID, name = "AwayFromAuction", version = "v1.0.0", acceptedMinecraftVersions = "1.8.9", clientSideOnly = true, useMetadata = true, updateJSON = "https://raw.githubusercontent.com/MoSadie/AwayFromAuction/master/updateJSON.json")
public class AwayFromAuction {
    public static final String MOD_ID = "awayfromauction";
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static final Gson GSON = new GsonBuilder().create();
    
    private SyncThread syncThread;
    
    private HypixelAPI hypixelApi;
    private HttpClient httpClient = HttpClientBuilder.create().build();

    public static Configuration config;
    
    // Auction cache
    private Map<UUID, Auction> allAuctions;
    private Map<UUID, List<Auction>> playerAuctionMap;
    private Map<String, List<Auction>> itemAuctionMap;
    private List<Auction> bidAuctions;
    private long totalCoins;
    
    public AwayFromAuction() {        
        LOGGER.debug("Setting up maps");
        nameCache = new HashMap<>();
        uuidCache = new HashMap<>();
        
        allAuctions = new HashMap<>();
        playerAuctionMap = new HashMap<>();
        itemAuctionMap = new HashMap<>();
        bidAuctions = new ArrayList<Auction>();
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
    
    public static ChatComponentTranslation getTranslatedTextComponent(String key, Object... args) {
        return new ChatComponentTranslation(MOD_ID + "." + key, args);
    }
    
    /**
    * Update the auction caches with the specified maps.
    * @param allAuctionsMap Map of all auctions. Key is Auction UUID.
    * @param playerAuctionsMap Map of all auctions. Key is Auction Owner UUID.
    * @param itemAuctionsMap Map of all auctions. Key is Auction Item Name.
    * @param bidAuctions List of all active auctions bid on by player. 
    */
    void updateAuctions(Map<UUID, Auction> allAuctionsMap, Map<UUID, List<Auction>> playerAuctionsMap, Map<String, List<Auction>> itemAuctionsMap, List<Auction> bidAuctions) {
        if (allAuctionsMap == null || allAuctionsMap.isEmpty()) {
            LOGGER.error("No auctions found on the auction house!");
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Minecraft.getMinecraft().thePlayer.addChatMessage(getTranslatedTextComponent("error.noauctions"));
            });
            refreshHypixelApi();
            return;
        }
        if(!onHypixel()) {
            LOGGER.debug("Checking for notifications to send.");
            notifyEndingSoon(playerAuctionsMap.get(Minecraft.getMinecraft().thePlayer.getUniqueID()));
            notifyNewBid(this.playerAuctionMap.get(Minecraft.getMinecraft().thePlayer.getUniqueID()), playerAuctionsMap.get(Minecraft.getMinecraft().thePlayer.getUniqueID()));
            notifyOutbid(this.bidAuctions, bidAuctions);
        }
        LOGGER.debug("Updating auction cache!");
        this.allAuctions = allAuctionsMap;
        this.playerAuctionMap = playerAuctionsMap;
        this.itemAuctionMap = itemAuctionsMap;
        this.bidAuctions = bidAuctions;
    }
    
    /**
    * Notifies the player for each auction that is ending in the next 5 minutes.
    * @param auctionsToCheck List of auctions to check.
    */
    private void notifyEndingSoon(List<Auction> auctionsToCheck) {
        if (auctionsToCheck == null) {
            LOGGER.debug("NotifyEndingSoon Exiting because Null List");
            return;
        }
        for(Auction auction : auctionsToCheck) {
            if (auction.getEnd().getTime()-auction.getSyncTimestamp().getTime() < (5*60)) {
                LOGGER.info("Auction ending soon: " + auction.getAuctionUUID());
                Minecraft.getMinecraft().thePlayer.addChatMessage(createEndingSoonText(auction));
            }
        }
    }
    
    /**
    * Creates a user-friendly IChatComponent to be shown to the player about an auction that is ending soon.
    * @param auction The auction that is ending soon.
    * @return An IChatComponent to be shown to the player.
    */
    private IChatComponent createEndingSoonText(Auction auction) {
        ChatComponentText root = new ChatComponentText("[AfA] Your auction for ");
        ChatComponentText itemName = new ChatComponentText(auction.getItemName());
        itemName.getChatStyle()
        .setUnderlined(true)
        .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa view " + auction.getAuctionUUID()))
        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Click to view auction!")));
        long time = auction.getEnd().getTime()-auction.getSyncTimestamp().getTime();
        ChatComponentText endingTime = new ChatComponentText(" is ending in about " + time + "second" + (time > 1 ? "s" : "") + "! ");
        
        IChatComponent hypixelLink = createHypixelLink();
        
        root.appendSibling(itemName);
        root.appendSibling(endingTime);
        root.appendSibling(hypixelLink);
        
        return root;
    }
    
    /**
    * Notifies the player for every auction they own that has a new bid since last sync.
    * @param current Current list of auction states for all owned auctions.
    * @param incoming New list of auction states for all owned auctions.
    */
    private void notifyNewBid(List<Auction> current, List<Auction> incoming) {
        if (current == null) {
            LOGGER.debug("NotifyNewBid failing: Current Auction List is Null!");
            return;
        } else if (incoming == null) {
            LOGGER.debug("NotifyNewBid failing: Incomming Auction list is Null!");
            return;
        }

        Map<UUID, Auction> auctionMap = new HashMap<>();
        for(Auction auction : incoming) {
            auctionMap.put(auction.getAuctionUUID(), auction);
        }
        for(Auction auction : current) {
            if (auctionMap.containsKey(auction.getAuctionUUID())) {
                Auction other = auctionMap.get(auction.getAuctionUUID());
                if (other.getHighestBidAmount() > auction.getHighestBidAmount()) {
                    LOGGER.info("New bid on auction " + auction.getAuctionUUID());
                    Minecraft.getMinecraft().thePlayer.addChatMessage(createNewBidText(other));
                }
            }
        }
    }
    
    /**
    * Creates a user-friendly IChatComponent to be shown to the player about a new bid on their auction.
    * @param auction The auction state of the auction.
    * @return An IChatComponent to show to the player.
    */
    private IChatComponent createNewBidText(Auction auction) {
        ChatComponentText root = new ChatComponentText("[AfA] There is a new bid on your auction for the ");
        
        ChatComponentText itemName = new ChatComponentText(auction.getItemName());
        itemName.getChatStyle()
        .setUnderlined(true)
        .setColor(AfAUtils.getColorFromTier(auction.getTier()))
        .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa view " + auction.getAuctionUUID()))
        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Click to view auction!")));
        
        int newBid = auction.getHighestBidAmount();
        String otherUser = auction.getAFA().getPlayerName(auction.getHighestBid().getBidderUUID());
        ChatComponentText bidInfo = new ChatComponentText(" for " + AfAUtils.formatCoins(newBid) + " coin" + (newBid > 1 ? "s" : "") + " by " + otherUser + "! ");
        
        IChatComponent hypixelLink = createHypixelLink();
        
        root.appendSibling(itemName);
        root.appendSibling(bidInfo);
        root.appendSibling(hypixelLink);
        
        return root;
    }
    
    /**
    * Notifies the player for every auction they have been outbid on.
    * @param current Current auction states for the auctions the player has bid on.
    * @param incoming New auction states for the auctions the player has bid on.
    */
    private void notifyOutbid(List<Auction> current, List<Auction> incoming) {
        if (current == null) {
            LOGGER.debug("NotifyOutbid failing: Current Auction List is Null!");
            return;
        } else if (incoming == null) {
            LOGGER.debug("NotifyOutbid failing: Incomming Auction list is Null!");
            return;
        }

        Map<UUID, Auction> auctionMap = new HashMap<>();
        for(Auction incomingAuction : incoming) {
            auctionMap.put(incomingAuction.getAuctionUUID(), incomingAuction);
        }
        
        for(Auction currentAuction : current) {
            if (auctionMap.containsKey(currentAuction.getAuctionUUID())) {
                Auction newAuction = auctionMap.get(currentAuction.getAuctionUUID());
                if (currentAuction.getHighestBidAmount() < newAuction.getHighestBidAmount() && newAuction.getHighestBid().getBidderUUID().equals(Minecraft.getMinecraft().thePlayer.getUniqueID())) {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(createOutbidText(currentAuction, newAuction));
                }
            }
        }
    }
    
    /**
    * Creates a user-friendly IChatComponent to be shown to the player about being outbid on an auction.
    * @param current Current auction state.
    * @param other New auction state.
    * @return An IChatComponent to show to the player
    */
    private IChatComponent createOutbidText(Auction current, Auction other) {
        ChatComponentText root = new ChatComponentText("[AfA] You have been outbid on the auction for ");
        
        ChatComponentText itemName = new ChatComponentText(current.getItemName());
        itemName.getChatStyle()
        .setUnderlined(true)
        .setColor(AfAUtils.getColorFromTier(other.getTier()))
        .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa view " + current.getAuctionUUID()))
        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Click to view auction!")));
        
        Bid yourBid = null;
        for(Bid bid : current.getBids()) {
            if (bid.getBidderUUID().equals(Minecraft.getMinecraft().thePlayer.getUniqueID())){
                yourBid = bid;
                break;
            }
        }
        long diff = other.getHighestBidAmount() - (yourBid != null ? yourBid.getAmount() : 0);
        String otherUser = other.getAFA().getPlayerName(other.getHighestBid().getBidderUUID());
        ChatComponentText outbidBy = new ChatComponentText(" by " + AfAUtils.formatCoins(diff) + " coin" + (diff > 1 ? "s" : "") + " by " + otherUser + "! ");
        IChatComponent hypixelLink = createHypixelLink();
        root.appendSibling(itemName);
        root.appendSibling(outbidBy);
        root.appendSibling(hypixelLink);
        
        return root;
    }
    
    private IChatComponent createHypixelLink() {
        if (onHypixel()) return new ChatComponentText("");
        ChatComponentText hypixelLink = new ChatComponentText("CLICK HERE");
        hypixelLink.getChatStyle()
        .setUnderlined(true)
        .setBold(true)
        .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa joinhypixel"))
        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Click to join the Hypixel server!")));
        ChatComponentText ending = new ChatComponentText(" to join the Hypixel server!");
        hypixelLink.appendSibling(ending);
        return hypixelLink;
    }
    
    void setTotalCoins(long coins) {
        this.totalCoins = coins;
    }
    
    /**
    * Safely validates if the apiKey is in the correct form.
    * In this case that is UUID.
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
    * Tests the API key using the Hypixel API.
    * It is recommended to use {@link #validateAPIKey(String)} first to validate the key first.
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
            tmpApi.getPlayerByUuid(Minecraft.getMinecraft().thePlayer.getUniqueID()).get(1, TimeUnit.MINUTES);
            LOGGER.info("API key test passed!");
        } catch (Exception e) {
            LOGGER.warn("API key test failed! Exception: " + e.getLocalizedMessage());
            return false;
        }
        return true;
    }
    
    /**
    * Sets the Hypixel API key in the config file and refreshes our connection to the HypixelAPI
    * @param apiKey The new HypixelAPI key to use.
    * @return True if the key works and nothing goes wrong refreshing our connection, false otherwise.
    */
    public boolean setAPIKey(String apiKey) {
        if (testAPIKey(apiKey)) {
            LOGGER.info("Setting new Hypixel API key");
            Config.HYPIXEL_API_KEY = apiKey;
            config.save();
            return refreshHypixelApi();
        }
        return false;
    }
    
    /**
    * Refreshes the HypixelAPI object we use to connect to the Hypixel API.
    * Called when the API key is loaded/changed.
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
    
    /**
    * @return True if connected to the Hypixel Minecraft server, false otherwise.
    */
    public boolean onHypixel() {
        try {
            return Minecraft.getMinecraft().getCurrentServerData().serverIP.contains(".hypixel.net");
        } catch (Exception e) {
            return false;
        }
    }
    
    private Map<UUID, String> nameCache;
    
    /**
    * Converts a player UUID to their Username.
    * @param uuid UUID of a player
    * @return Their current Username or "ERROR"
    */
    public String getPlayerName(UUID uuid) {
        if (nameCache.containsKey(uuid)) {
            return nameCache.get(uuid);
        }
        
        try {
            String url = "https://api.mojang.com/user/profiles/" + AfAUtils.removeHyphens(uuid) + "/names";
            HttpResponse response = httpClient.execute(new HttpGet(url));
            String content = EntityUtils.toString(response.getEntity(), "UTF-8");
            PlayerNames[] names = GSON.fromJson(content, PlayerNames[].class);
            
            String name =  names[names.length - 1].getName();
            nameCache.put(uuid, name);
            uuidCache.put(name, uuid);
            return name;
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
    
    private class PlayerNames {
        private String name;
        
        String getName() {
            return name;
        }
    }
    
    private class PlayerUUID {
        private String name;
        private String id;
        
        String getName() {return name;}
        UUID getUUID() {return UUID.fromString(AfAUtils.addHyphens(id));}
    }
    
    private Map<String, UUID> uuidCache;
    
    /**
    * Converts a player Username to a UUID.
    * @param name Username of a player.
    * @return Their UUID or null.
    */
    public UUID getPlayerUUID(String name) {
        name = name.toLowerCase();
        if(uuidCache.containsKey(name)) {
            return uuidCache.get(name);
        }
        
        try {
            String url = "https://api.mojang.com/users/profiles/minecraft/" + name;
            HttpResponse response = httpClient.execute(new HttpGet(url));
            String content = EntityUtils.toString(response.getEntity(), "UTF-8");
            PlayerUUID uuidJson = GSON.fromJson(content, PlayerUUID.class);
            UUID uuid = uuidJson.getUUID();
            
            nameCache.put(uuid, uuidJson.getName());
            uuidCache.put(name, uuid);
            return uuid;
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
    * @param auctionUUID The UUID of the auction to get state of.
    * @return The specified auction's current state or a special Error Auction.
    */
    public Auction getAuction(UUID auctionUUID) {
        if (allAuctions.containsKey(auctionUUID)) {
            return allAuctions.get(auctionUUID);
        }
        
        try {
            if (hypixelApi == null) {
                if (!refreshHypixelApi()) {
                    LOGGER.error("Something went wrong refreshing Hypixel API object!");
                    return Auction.ERROR_AUCTION;
                }
            }
            
            SkyBlockAuctionReply reply = hypixelApi.getSkyblockAuctionsByUUID(auctionUUID).get(1, TimeUnit.MINUTES);
            
            if (reply.getAuctions().size() > 0) {
                LOGGER.debug("Previously unknown auction found! Sorting into cache.");
                Auction tmpAuction = new Auction(reply.getAuctions().get(0).getAsJsonObject(), this);
                
                sortAuction(tmpAuction);
                
                return tmpAuction;
            } else {
                LOGGER.error("Attempted to get auction, but no auction found!");
                return Auction.ERROR_AUCTION;
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error("Something went wrong getting that auction. Exception: " + e.getLocalizedMessage());
            LOGGER.catching(Level.ERROR, e);
            return Auction.ERROR_AUCTION;
        }
        
    }
    
    /**
    * Gets all auctions owner by a player.
    * @param playerUUID The UUID of the auction owner.
    * @return Array of current auction states of all auctions owner by the specified player.
    */
    public Auction[] getAuctionsByPlayer(UUID playerUUID) {
        if (playerAuctionMap.containsKey(playerUUID)) {
            return playerAuctionMap.get(playerUUID).toArray(new Auction[0]);
        }
        
        try {
            if (hypixelApi == null) {
                if (!refreshHypixelApi()) {
                    LOGGER.error("Something went wrong refreshing Hypixel API object!");
                    return new Auction[] {};
                }
            }
            
            SkyBlockAuctionReply reply = hypixelApi.getSkyblockAuctionsByPlayer(playerUUID).get(1, TimeUnit.MINUTES);
            
            List<Auction> result = new ArrayList<>();
            
            for (JsonElement element : reply.getAuctions()) {
                Auction tmpAuction = new Auction(element.getAsJsonObject(), this);
                sortAuction(tmpAuction);
                result.add(tmpAuction);
            }
            
            return result.toArray(new Auction[0]);
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error("Something went wrong getting auctions for that user. Exception: " + e.getLocalizedMessage());
            LOGGER.catching(Level.ERROR, e);
            return new Auction[] {};
        }
    }
    
    /**
    * @return An array containing all currently known auctions' states.
    */
    public Auction[] getAuctions() {
        return allAuctions.values().toArray(new Auction[0]);
    }
    
    /**
    * @return An array of Strings with all currently known items up for auction.
    */
    public String[] getAuctionItems() {
        return itemAuctionMap.keySet().toArray(new String[0]);
    }

    /**
     * Checks if item string exactly matches an item up for auction
     * @param item Auction item name
     * @return True if at item name matches an auction item, false otherwise.
     */
    public boolean isAuctionItem(String item) {
        return itemAuctionMap.keySet().contains(item.toLowerCase());
    }
    
    /**
    * Gets all known auction items that contain the specified string.
    * @param filter The string to filter the results.
    * @return Array with all currently known auction items that contain the filter string.
    */
    public String[] getAuctionItems(String filter) {
        List<String> result = new ArrayList<>();
        for(String item : itemAuctionMap.keySet()) {
            if (item.contains(filter.toLowerCase())) {
                if (itemAuctionMap.get(item).size() > 0) {
                    result.add(itemAuctionMap.get(item).get(0).getItemName());
                } else {
                    result.add(item);
                }
            }
        }
        return result.toArray(new String[0]);
    }
    
    /**
    * Gets all auctions that contain the specified item.
    * @param itemName The name of the item being auctioned.
    * @return Array of current auction states for all auctions of that item.
    */
    public Auction[] getAuctionsByItem(String itemName) {
        if (itemName == null) {
            LOGGER.warn("ItemName was null! Returning empty array!");
            return new Auction[] {};
        } else if (!itemAuctionMap.containsKey(itemName.toLowerCase())) {
            LOGGER.warn("Item not found!");
            return new Auction[] {};
        }
        
        return itemAuctionMap.get(itemName.toLowerCase()).toArray(new Auction[0]);
    }
    
    /**
    * @return Array of current auction states for all active auctions currently bid on by the player.
    */
    public Auction[] getBidOnAuctions() {
        return bidAuctions.toArray(new Auction[0]);
    }
    
    /**
    * @return The current total number of coins sitting in the auction house in bids.
    */
    public long getTotalCoins() {
        return totalCoins;
    }
    
    /**
    * Refreshes the HypixelAPI object if it's not been initialized yet.
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
    
    /**
    * Sorts a singular auction into all the various maps and lists.
    * @param auction The auction to sort.
    */
    private void sortAuction(Auction auction) {
        allAuctions.put(auction.getAuctionUUID(), auction);
        
        if (!playerAuctionMap.containsKey(auction.getAuctionOwnerUUID())) {
            playerAuctionMap.put(auction.getAuctionOwnerUUID(), new ArrayList<>());
        }
        playerAuctionMap.get(auction.getAuctionOwnerUUID()).add(auction);
        
        if (!itemAuctionMap.containsKey(auction.getItemName().toLowerCase())) {
            itemAuctionMap.put(auction.getItemName().toLowerCase(), new ArrayList<>());
        }
        itemAuctionMap.get(auction.getItemName().toLowerCase()).add(auction);
        
        if (AfAUtils.bidsContainUUID(auction.getBids(), Minecraft.getMinecraft().thePlayer.getUniqueID())) {
            bidAuctions.add(auction);
        }
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