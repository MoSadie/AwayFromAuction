package io.github.mosadie.awayfromauction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;

import io.github.mosadie.awayfromauction.AwayFromAuction.BazaarProduct;
import io.github.mosadie.awayfromauction.event.AuctionEndingSoonEvent;
import io.github.mosadie.awayfromauction.event.AuctionNewBidEvent;
import io.github.mosadie.awayfromauction.event.AuctionOutbidEvent;
import io.github.mosadie.awayfromauction.util.AfAUtils;
import io.github.mosadie.awayfromauction.util.Auction;
import io.github.mosadie.awayfromauction.util.Auction.Bid;
import net.hypixel.api.HypixelAPI;
import net.hypixel.api.reply.skyblock.SkyBlockAuctionsReply;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

public class SyncThread extends Thread {
    private final AwayFromAuction afa;
    private final File syncCache;
    private final Gson GSON;
    private boolean stopFlag = false;

    private Map<UUID, Auction> allAuctions;
    private Map<UUID, List<Auction>> playerAuctionMap;
    private Map<String, List<Auction>> itemAuctionMap;
    private List<Auction> bidAuctions;
    private long totalCoins;

    private Map<String, BazaarProduct> bazaarMap;

    public SyncThread(AwayFromAuction mod, File syncCache, Gson GSON) {
        setName("AwayFromAuctionSync");
        this.afa = mod;
        this.syncCache = syncCache;
        this.GSON = GSON;

        allAuctions = new HashMap<>();
        playerAuctionMap = new HashMap<>();
        itemAuctionMap = new HashMap<>();
        bidAuctions = new ArrayList<>();
        totalCoins = 0;

        bazaarMap = new TreeMap<>();
    }

    @Override
    public void run() {

        if (syncCache.exists()) {
            loadFromCache();
        } else {
            saveToCache();
        }

        while (!this.isInterrupted() && !stopFlag && Minecraft.getMinecraft() != null) {
            try {
                Thread.sleep(Config.GENERAL_REFRESH_DELAY * 1000);
                if (!stopFlag) {
                    AwayFromAuction.getLogger().info("Syncing with Hypixel Skyblock Auction House");
                    sync();
                    AwayFromAuction.getLogger().info("Sync finished!");
                } else {
                    // All is fine, silently fall out, the stop flag was set while we were sleeping.
                }
            } catch (InterruptedException e) {
                // Do nothing, it's fine.
            }
        }
    }

    /**
     * Syncs all active auction and bazaar data from the Hypixel API and
     * https://sky.lea.moe. Sorts that data into multiple groups and caches it.
     */
    private void sync() {
        if (Minecraft.getMinecraft().thePlayer == null) {
            AwayFromAuction.getLogger().info("Player is null, skipping sync.");
            return;
        }
        HypixelAPI hypixelApi = afa.getOrRefreshHypixelAPI();

        // Get updated auction house state

        Map<UUID, Auction> tmpAllAuctions = new HashMap<>();
        Map<UUID, List<Auction>> tmpPlayerAuctionMap = new HashMap<>();
        Map<String, List<Auction>> tmpItemAuctionMap = new HashMap<>();
        List<Auction> tmpBidAuctions = new ArrayList<>();
        long tmpTotalCoins = 0;
        try {
            // Sync the first page.
            SkyBlockAuctionsReply reply = hypixelApi.getSkyBlockAuctions(0).get(1, TimeUnit.MINUTES);
            int pageTotal = reply.getTotalPages();
            AwayFromAuction.getLogger().info("Syncing " + pageTotal + " pages of auctions");
            if (reply.getAuctions().size() > 0) {
                AwayFromAuction.getLogger()
                        .info("Syncing auction house page 1 (" + reply.getAuctions().size() + " auctions)");
                for (int i = 0; i < reply.getAuctions().size(); i++) {
                    Auction tmpAuction = new Auction(reply.getAuctions().get(i).getAsJsonObject(), afa);

                    // Fetch Username
                    fetchPlayerName(tmpAuction.getAuctionOwnerUUID());

                    // Add to all auctions cache
                    if (!tmpAllAuctions.containsKey(tmpAuction.getAuctionUUID()))
                        tmpAllAuctions.put(tmpAuction.getAuctionUUID(), tmpAuction);

                    // Sort into groups by auction owner UUID.
                    if (!tmpPlayerAuctionMap.containsKey(tmpAuction.getAuctionOwnerUUID())) {
                        tmpPlayerAuctionMap.put(tmpAuction.getAuctionOwnerUUID(), new ArrayList<>());
                    }
                    tmpPlayerAuctionMap.get(tmpAuction.getAuctionOwnerUUID()).add(tmpAuction);

                    // Sort into groups by item name.
                    if (!tmpItemAuctionMap.containsKey(tmpAuction.getItemName().toLowerCase())) {
                        tmpItemAuctionMap.put(tmpAuction.getItemName().toLowerCase(), new ArrayList<>());
                    }
                    tmpItemAuctionMap.get(tmpAuction.getItemName().toLowerCase()).add(tmpAuction);

                    // Group all auctions with a bid from the current player.
                    if (AfAUtils.bidsContainUUID(tmpAuction.getBids(),
                            Minecraft.getMinecraft().thePlayer.getUniqueID())) {
                        tmpBidAuctions.add(tmpAuction);
                    }

                    // Add to to the total coins in the auction house.
                    for (Bid bid : tmpAuction.getBids()) {
                        tmpTotalCoins += bid.getAmount();
                    }
                }
            } else {
                AwayFromAuction.getLogger().error("Attempted to get auctions, but no auctions found!");
            }

            // Repeat for the rest of the pages.
            for (int p = 1; p < pageTotal; p++) {
                AwayFromAuction.getLogger().info(
                        "Syncing auction house page " + (p + 1) + " (" + reply.getAuctions().size() + " auctions)");
                SkyBlockAuctionsReply replyPage = hypixelApi.getSkyBlockAuctions(p).get(1, TimeUnit.MINUTES);
                for (int i = 0; i < replyPage.getAuctions().size(); i++) {
                    Auction tmpAuction = new Auction(replyPage.getAuctions().get(i).getAsJsonObject(), afa);

                    // Fetch Username
                    fetchPlayerName(tmpAuction.getAuctionOwnerUUID());

                    // Add to all auctions cache
                    tmpAllAuctions.put(tmpAuction.getAuctionUUID(), tmpAuction);

                    // Sort into groups by auction owner UUID.
                    if (!tmpPlayerAuctionMap.containsKey(tmpAuction.getAuctionOwnerUUID())) {
                        tmpPlayerAuctionMap.put(tmpAuction.getAuctionOwnerUUID(), new ArrayList<>());
                    }
                    tmpPlayerAuctionMap.get(tmpAuction.getAuctionOwnerUUID()).add(tmpAuction);

                    // Sort into groups by item name.
                    if (!tmpItemAuctionMap.containsKey(tmpAuction.getItemName().toLowerCase())) {
                        tmpItemAuctionMap.put(tmpAuction.getItemName().toLowerCase(), new ArrayList<>());
                    }
                    tmpItemAuctionMap.get(tmpAuction.getItemName().toLowerCase()).add(tmpAuction);

                    // Group all auctions with a bid from the current player.
                    if (AfAUtils.bidsContainUUID(tmpAuction.getBids(),
                            Minecraft.getMinecraft().thePlayer.getUniqueID())) {
                        tmpBidAuctions.add(tmpAuction);
                    }

                    // Add to to the total coins in the auction house.
                    for (Bid bid : tmpAuction.getBids()) {
                        tmpTotalCoins += bid.getAmount();
                    }
                }
            }

            // Sync Bazaar

            Map<String, BazaarProduct> tmpBazaarMap = afa.getBazaarState();
            if (tmpBazaarMap != null) {
                bazaarMap = tmpBazaarMap;
            }

            // Look for events

            // - Ending Soon -
            if (tmpPlayerAuctionMap.containsKey(Minecraft.getMinecraft().thePlayer.getUniqueID())) {
                for (Auction auction : tmpPlayerAuctionMap.get(Minecraft.getMinecraft().thePlayer.getUniqueID())) {
                    if (auction.getEnd().getTime() - auction.getSyncTimestamp().getTime() < (5 * 60 * 1000)) {
                        AuctionEndingSoonEvent endingSoonEvent = new AuctionEndingSoonEvent(auction);
                        MinecraftForge.EVENT_BUS.post(endingSoonEvent);
                    }
                }
            }

            // - Outbid -
            Map<UUID, Auction> tmpOutbidAuctionMap = new HashMap<>();
            for (Auction incomingAuction : tmpBidAuctions) {
                tmpOutbidAuctionMap.put(incomingAuction.getAuctionUUID(), incomingAuction);
            }

            for (Auction currentAuction : bidAuctions) {
                if (tmpOutbidAuctionMap.containsKey(currentAuction.getAuctionUUID())) {
                    Auction newAuction = tmpOutbidAuctionMap.get(currentAuction.getAuctionUUID());

                    boolean newHighBid = currentAuction.getHighestBidAmount() < newAuction.getHighestBidAmount();
                    boolean notThePlayer = !newAuction.getHighestBid().getBidderUUID()
                            .equals(Minecraft.getMinecraft().thePlayer.getUniqueID());
                    if (newHighBid && notThePlayer) {
                        AuctionOutbidEvent outbidEvent = new AuctionOutbidEvent(newAuction);
                        MinecraftForge.EVENT_BUS.post(outbidEvent);
                    }
                }
            }

            // - New Bid -
            List<Auction> tmpCurrentOwnedAuctions = new ArrayList<>();
            List<Auction> tmpNewOwnedAuctions = new ArrayList<>();

            if (playerAuctionMap.containsKey(Minecraft.getMinecraft().thePlayer.getUniqueID())) {
                tmpCurrentOwnedAuctions = playerAuctionMap.get(Minecraft.getMinecraft().thePlayer.getUniqueID());
            }
            if (tmpPlayerAuctionMap.containsKey(Minecraft.getMinecraft().thePlayer.getUniqueID())) {
                tmpNewOwnedAuctions = tmpPlayerAuctionMap.get(Minecraft.getMinecraft().thePlayer.getUniqueID());
            }

            Map<UUID, Auction> tmpNewBidAuctionMap = new HashMap<>();
            for (Auction auction : tmpNewOwnedAuctions) {
                tmpNewBidAuctionMap.put(auction.getAuctionUUID(), auction);
            }
            for (Auction auction : tmpCurrentOwnedAuctions) {
                if (tmpNewBidAuctionMap.containsKey(auction.getAuctionUUID())) {
                    Auction newState = tmpNewBidAuctionMap.get(auction.getAuctionUUID());
                    if (newState.getBids().length > auction.getBids().length) {
                        AuctionNewBidEvent newBidEvent = new AuctionNewBidEvent(newState);
                        MinecraftForge.EVENT_BUS.post(newBidEvent);
                    }
                }
            }

            // Update cache
            allAuctions = tmpAllAuctions;
            playerAuctionMap = tmpPlayerAuctionMap;
            itemAuctionMap = tmpItemAuctionMap;
            bidAuctions = tmpBidAuctions;
            totalCoins = tmpTotalCoins;

            saveToCache();

        } catch (InterruptedException | ExecutionException | TimeoutException | NullPointerException e) {
            AwayFromAuction.getLogger()
                    .warn("An exception occured while attempting to sync auction details: " + e.getMessage());
            AwayFromAuction.getLogger().catching(e);
        }
    }

    public void stopFlag() {
        stopFlag = true;
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
        UUID uuid = afa.getPlayerUUID(name);
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
            AwayFromAuction.getLogger().warn("Attempted to get auctions for non existing player!");
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
        return bidAuctions;
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
     * @return A mapping from a bazaar product's name to the BazaarProduct object.
     */
    public Map<String, BazaarProduct> getBazaarProducts() {
        return bazaarMap;
    }

    /**
     * Saves the current auctions to the cache file.
     */
    private void saveToCache() {
        Auction[] auctions = getAllAuctions().values().toArray(new Auction[0]);
        String json = GSON.toJson(auctions);

        try {
            syncCache.createNewFile();

            OutputStreamWriter outputWriter = new OutputStreamWriter(new FileOutputStream(syncCache));
            outputWriter.write(json);
            outputWriter.close();
        } catch (IOException e) {
            AwayFromAuction.getLogger().error("Exception occured saving cache. ", e);
        }
    }

    /**
     * Loads the auctions from the cache file.
     */
    private void loadFromCache() {
        if (!syncCache.exists()) {
            AwayFromAuction.getLogger().warn("Cache Not Found!");
            return;
        }

        try {
            Scanner scanner = new Scanner(syncCache);
            String content = scanner.nextLine();
            scanner.close();

            AwayFromAuction.getLogger().info("[AfA]Cache Content Length: " + content.length());

            Auction[] auctions = GSON.fromJson(content, Auction[].class);

            AwayFromAuction.getLogger().info("Cache Auction Number: " + auctions.length);

            Map<UUID, Auction> tmpAllAuctions = new HashMap<>();
            Map<UUID, List<Auction>> tmpPlayerAuctionMap = new HashMap<>();
            Map<String, List<Auction>> tmpItemAuctionMap = new HashMap<>();
            List<Auction> tmpBidAuctions = new ArrayList<>();
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

                if (AfAUtils.bidsContainUUID(auction.getBids(), Minecraft.getMinecraft().thePlayer.getUniqueID()))
                    tmpBidAuctions.add(auction);

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
            AwayFromAuction.getLogger().error("Exception occured loading from cache. ", e);
        }
    }

    /**
     * Attempt to fetch the player name asynchronously if not already cached. Does
     * not return the player name.
     * 
     * @param uuid The UUID of the player.
     */
    private void fetchPlayerName(UUID uuid) {
        if (!afa.isPlayerCached(uuid)) {
            CompletableFuture<Void> username = CompletableFuture.runAsync(() -> afa.getPlayerName(uuid));
            // username.thenAccept(name -> AwayFromAuction.getLogger().info("Username
            // Cached: " + name));
        }
    }
}