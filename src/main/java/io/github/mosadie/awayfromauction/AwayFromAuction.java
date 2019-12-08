package io.github.mosadie.awayfromauction;

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
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
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
    
    private static final Gson GSON = new GsonBuilder().create();
    
    private SyncThread syncThread;
    
    private HypixelAPI hypixelApi;
    private HttpClient httpClient = HttpClientBuilder.create().build();
    
    private Map<UUID, Auction> allAuctions;
    private Map<UUID, List<Auction>> playerAuctionMap;
    private Map<String, List<Auction>> itemAuctionMap;
    private List<Auction> bidAuctions;
    private long totalCoins;
    
    public AwayFromAuction() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG);
        
        IEventBus event = FMLJavaModLoadingContext.get().getModEventBus();
        event.addListener(this::setupClient);
        
        Config.loadConfig(Config.CLIENT_CONFIG, FMLPaths.CONFIGDIR.get().resolve("awayfromauction-client.toml"));
        
        nameCache = new HashMap<>();
        uuidCache = new HashMap<>();
        
        allAuctions = new HashMap<>();
        playerAuctionMap = new HashMap<>();
        itemAuctionMap = new HashMap<>();
        bidAuctions = new ArrayList<Auction>();
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
    
    void updateAuctions(Map<UUID, Auction> allAuctionsMap, Map<UUID, List<Auction>> playerAuctionsMap, Map<String, List<Auction>> itemAuctionsMap, List<Auction> bidAuctions) {
        notifyEndingSoon(playerAuctionsMap.get(Minecraft.getInstance().player.getUniqueID()));
        notifyNewBid(this.playerAuctionMap.get(Minecraft.getInstance().player.getUniqueID()), playerAuctionsMap.get(Minecraft.getInstance().player.getUniqueID()));
        notifyOutbid(this.bidAuctions, bidAuctions);
        this.allAuctions = allAuctionsMap;
        this.playerAuctionMap = playerAuctionsMap;
        this.itemAuctionMap = itemAuctionsMap;
        this.bidAuctions = bidAuctions;
    }

    private void notifyEndingSoon(List<Auction> auctionsToCheck) {
        for(Auction auction : auctionsToCheck) {
            if (auction.getEnd().getTime()-auction.getSyncTimestamp().getTime() < (5*60)) {
                Minecraft.getInstance().player.sendMessage(createEndingSoonText(auction));
            }
        }
    }

    private ITextComponent createEndingSoonText(Auction auction) {
        StringTextComponent root = new StringTextComponent("[AfA] Your auction for ");
        StringTextComponent itemName = new StringTextComponent(auction.getItemName());
        itemName.getStyle()
                    .setUnderlined(true)
                    .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa view " + auction.getAuctionUUID()))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to view auction!")));
        long time = auction.getEnd().getTime()-auction.getSyncTimestamp().getTime();
        StringTextComponent endingTime = new StringTextComponent(" is ending in about " + time + "second" + (time > 1 ? "s" : "") + "! ");
        StringTextComponent hypixelLink = new StringTextComponent("CLICK HERE");
        hypixelLink.getStyle()
                    .setUnderlined(true)
                    .setBold(true)
                    .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa joinhypixel"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to join the Hypixel server!")));
        StringTextComponent ending = new StringTextComponent(" to join the Hypixel server now!");
        root.appendSibling(itemName);
        root.appendSibling(endingTime);
        root.appendSibling(hypixelLink);
        root.appendSibling(ending);

        return root;
    }

    private void notifyNewBid(List<Auction> current, List<Auction> incoming) {
        Map<UUID, Auction> auctionMap = new HashMap<>();
        for(Auction auction : incoming) {
            auctionMap.put(auction.getAuctionUUID(), auction);
        }
        for(Auction auction : current) {
            if (auctionMap.containsKey(auction.getAuctionUUID())) {
                Auction other = auctionMap.get(auction.getAuctionUUID());
                if (other.getHighestBidAmount() > auction.getHighestBidAmount()) {
                    Minecraft.getInstance().player.sendMessage(createNewBidText(auction, other));
                }
            }
        }
    }

    private ITextComponent createNewBidText(Auction current, Auction other) {
        StringTextComponent root = new StringTextComponent("[AfA] There is a new bid on your auction for the ");
        StringTextComponent itemName = new StringTextComponent(current.getItemName());
        itemName.getStyle()
                    .setUnderlined(true)
                    .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa view " + current.getAuctionUUID()))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to view auction!")));
        int newBid = other.getHighestBidAmount();
        String otherUser = other.getAFA().getPlayerName(other.getHighestBid().getBidderUUID());
        StringTextComponent outbidBy = new StringTextComponent(" for " + newBid + "coin" + (newBid > 1 ? "s" : "") + " by " + otherUser + "! ");
        StringTextComponent hypixelLink = new StringTextComponent("CLICK HERE");
        hypixelLink.getStyle()
                    .setUnderlined(true)
                    .setBold(true)
                    .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa joinhypixel"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to join the Hypixel server!")));
        StringTextComponent ending = new StringTextComponent(" to join the Hypixel server!");
        root.appendSibling(itemName);
        root.appendSibling(outbidBy);
        root.appendSibling(hypixelLink);
        root.appendSibling(ending);

        return root;
    }

    private void notifyOutbid(List<Auction> current, List<Auction> incoming) {
        Map<UUID, Auction> auctionMap = new HashMap<>();
        for(Auction incomingAuction : incoming) {
            auctionMap.put(incomingAuction.getAuctionUUID(), incomingAuction);
        }

        for(Auction currentAuction : current) {
            if (auctionMap.containsKey(currentAuction.getAuctionUUID())) {
                Auction other = auctionMap.get(currentAuction.getAuctionUUID());
                if (currentAuction.getHighestBidAmount() < other.getHighestBidAmount()) {
                    Minecraft.getInstance().player.sendMessage(createOutbidText(currentAuction, other));
                }
            }
        }
    }

    private ITextComponent createOutbidText(Auction current, Auction other) {
        StringTextComponent root = new StringTextComponent("[AfA] You have been outbid on the auction for ");
        StringTextComponent itemName = new StringTextComponent(current.getItemName());
        itemName.getStyle()
                    .setUnderlined(true)
                    .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa view " + current.getAuctionUUID()))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to view auction!")));
        Bid yourBid = null;
        for(Bid bid : current.getBids()) {
            if (bid.getBidderUUID().equals(Minecraft.getInstance().player.getUniqueID())){
                yourBid = bid;
                break;
            }
        }
        long diff = other.getHighestBidAmount() - (yourBid != null ? yourBid.getAmount() : 0);
        String otherUser = other.getAFA().getPlayerName(other.getHighestBid().getBidderUUID());
        StringTextComponent outbidBy = new StringTextComponent(" by " + diff + "coin" + (diff > 1 ? "s" : "") + " by " + otherUser + "! ");
        StringTextComponent hypixelLink = new StringTextComponent("CLICK HERE");
        hypixelLink.getStyle()
                    .setUnderlined(true)
                    .setBold(true)
                    .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa joinhypixel"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to join the Hypixel server!")));
        StringTextComponent ending = new StringTextComponent(" to join the Hypixel server now!");
        root.appendSibling(itemName);
        root.appendSibling(outbidBy);
        root.appendSibling(hypixelLink);
        root.appendSibling(ending);

        return root;
    }
    
    void setTotalCoins(long coins) {
        this.totalCoins = coins;
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
            return null;//UUID.fromString("00000000-0000-0000-0000-000000000000");
        } catch (Exception e) {
            LOGGER.error("Exception encountered getting player UUID! Exception: " + e.getLocalizedMessage());
            LOGGER.catching(Level.ERROR, e);
            return null;//UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
    }
    
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
    
    public Auction[] getAuctions() {
        return allAuctions.values().toArray(new Auction[0]);
        /*
        try {
            if (hypixelApi == null) {
                if (!refreshHypixelApi()) {
                    LOGGER.error("Something went wrong refreshing Hypixel API object!");
                    return new Auction[] {Auction.ERROR_AUCTION};
                }
            }
            
            SkyBlockAuctionsReply reply = hypixelApi.getSkyBlockAuctions(0).get(1, TimeUnit.MINUTES);
            
            if (reply.getAuctions().size() > 0) {
                Auction[] auctions = new Auction[reply.getAuctions().size()];
                for (int i = 0; i < reply.getAuctions().size(); i++) {
                    auctions[i] = new Auction(reply.getAuctions().get(i).getAsJsonObject(), this);
                }
                return auctions;
            } else {
                LOGGER.error("Attempted to get auctions, but no auctions found!");
                return new Auction[] {Auction.ERROR_AUCTION};
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error("Something went wrong getting auctions. Exception: " + e.getLocalizedMessage());
            LOGGER.catching(Level.ERROR, e);
            return new Auction[] {Auction.ERROR_AUCTION};
        }
        */
    }
    
    public String[] getAuctionItems() {
        return itemAuctionMap.keySet().toArray(new String[0]);
        // return getAuctionItems("");
        //TODO see which is better
    }
    
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
    
    public Auction[] getBidOnAuctions() {
        return bidAuctions.toArray(new Auction[0]);
    }
    
    public long getTotalCoins() {
        return totalCoins;
    }
    
    HypixelAPI getOrRefreshHypixelAPI() {
        if (hypixelApi == null) {
            if (!refreshHypixelApi()) {
                LOGGER.error("Something went wrong refreshing Hypixel API object!");
                return null;
            }
        }
        return hypixelApi;
    }
    
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
        
        if (AfAUtils.bidsContainUUID(auction.getBids(), Minecraft.getInstance().player.getUniqueID())) {
            bidAuctions.add(auction);
        }
    }
    
    void createSyncThread() {
        if (syncThread == null) {
            syncThread = new SyncThread(this);
            syncThread.start();
        }
    }
    
    void stopSyncThread() {
        if (syncThread != null) {
            syncThread.interrupt();
            syncThread = null;
        }
    }
}