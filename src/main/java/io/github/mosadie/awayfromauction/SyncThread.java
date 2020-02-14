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
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;

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
                    AwayFromAuction.getLogger().error("EDGE CASE SUCCESS"); //TODO remove debug
                }
            } catch (InterruptedException e) {
                // Do nothing, it's fine.
            }
        }
    }

    /**
     * Syncs all active auction data from the Hypixel API. Sorts that data into
     * multiple groups and calls
     * {@link AwayFromAuction#updateAuctions(Map, Map, Map, List)} to update the
     * auction cache.
     */
    private void sync() {
        // Get updated auction house state
        if (Minecraft.getMinecraft().thePlayer == null) {
            AwayFromAuction.getLogger().info("Player is null, skipping sync.");
            return;
        }
        HypixelAPI hypixelApi = afa.getOrRefreshHypixelAPI();
        Map<UUID, Auction> tmpAllAuctions = new HashMap<>();
        Map<UUID, List<Auction>> tmpPlayerAuctionMap = new HashMap<>();
        Map<String, List<Auction>> tmpItemAuctionMap = new HashMap<>();
        List<Auction> tmpBidAuctions = new ArrayList<>();
        long tmpTotalCoins = 0;
        try {
            SkyBlockAuctionsReply reply = hypixelApi.getSkyBlockAuctions(0).get(1, TimeUnit.MINUTES);
            int pageTotal = reply.getTotalPages();
            AwayFromAuction.getLogger().info("Syncing " + pageTotal + " pages of auctions");
            if (reply.getAuctions().size() > 0) {
                for (int i = 0; i < reply.getAuctions().size(); i++) {
                    Auction tmpAuction = new Auction(reply.getAuctions().get(i).getAsJsonObject(), afa);

                    tmpAllAuctions.put(tmpAuction.getAuctionUUID(), tmpAuction);

                    if (!tmpPlayerAuctionMap.containsKey(tmpAuction.getAuctionOwnerUUID())) {
                        tmpPlayerAuctionMap.put(tmpAuction.getAuctionOwnerUUID(), new ArrayList<>());
                    }
                    tmpPlayerAuctionMap.get(tmpAuction.getAuctionOwnerUUID()).add(tmpAuction);

                    if (!tmpItemAuctionMap.containsKey(tmpAuction.getItemName().toLowerCase())) {
                        tmpItemAuctionMap.put(tmpAuction.getItemName().toLowerCase(), new ArrayList<>());
                    }
                    tmpItemAuctionMap.get(tmpAuction.getItemName().toLowerCase()).add(tmpAuction);

                    if (AfAUtils.bidsContainUUID(tmpAuction.getBids(),
                            Minecraft.getMinecraft().thePlayer.getUniqueID()))
                        tmpBidAuctions.add(tmpAuction);

                    for (Bid bid : tmpAuction.getBids()) {
                        tmpTotalCoins += bid.getAmount();
                    }
                }
            } else {
                AwayFromAuction.getLogger().error("Attempted to get auctions, but no auctions found!");
            }
            for (int p = 1; p < pageTotal; p++) {
                SkyBlockAuctionsReply replyPage = hypixelApi.getSkyBlockAuctions(p).get(1, TimeUnit.MINUTES);
                for (int i = 0; i < replyPage.getAuctions().size(); i++) {
                    Auction tmpAuction = new Auction(replyPage.getAuctions().get(i).getAsJsonObject(), afa);

                    tmpAllAuctions.put(tmpAuction.getAuctionUUID(), tmpAuction);

                    if (!tmpPlayerAuctionMap.containsKey(tmpAuction.getAuctionOwnerUUID())) {
                        tmpPlayerAuctionMap.put(tmpAuction.getAuctionOwnerUUID(), new ArrayList<>());
                    }
                    tmpPlayerAuctionMap.get(tmpAuction.getAuctionOwnerUUID()).add(tmpAuction);

                    if (!tmpItemAuctionMap.containsKey(tmpAuction.getItemName().toLowerCase())) {
                        tmpItemAuctionMap.put(tmpAuction.getItemName().toLowerCase(), new ArrayList<>());
                    }
                    tmpItemAuctionMap.get(tmpAuction.getItemName().toLowerCase()).add(tmpAuction);

                    if (AfAUtils.bidsContainUUID(tmpAuction.getBids(),
                            Minecraft.getMinecraft().thePlayer.getUniqueID()))
                        tmpBidAuctions.add(tmpAuction);

                    for (Bid bid : tmpAuction.getBids()) {
                        tmpTotalCoins += bid.getAmount();
                    }
                }
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
                    if (currentAuction.getHighestBidAmount() < newAuction.getHighestBidAmount() && currentAuction
                            .getHighestBid().getBidderUUID().equals(Minecraft.getMinecraft().thePlayer.getUniqueID())) {
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
                    if (newState.getHighestBidAmount() > auction.getHighestBidAmount()) {
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

    public Map<UUID, Auction> getAllAuctions() {
        return allAuctions;
    }

    public List<Auction> getPlayerAuctions(String name) {
        UUID uuid = afa.getPlayerUUID(name);
        return getPlayerAuctions(uuid);
    }

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

    public String[] getAllItems() {
        return itemAuctionMap.keySet().toArray(new String[0]);
    }

    public boolean isItem(String item) {
        item = item.toLowerCase();
        return !itemAuctionMap.containsKey(item);
    }

    public List<Auction> getItemAuctions(String item) {
        if (itemAuctionMap.containsKey(item.toLowerCase())) {
            return itemAuctionMap.get(item.toLowerCase());
        } else {
            return new ArrayList<>();
        }
    }

    public List<Auction> getBidOnAuctions() {
        return bidAuctions;
    }

    public long getTotalCoins() {
        return totalCoins;
    }

    public void saveToCache() {
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

    public void loadFromCache() {
        if (!syncCache.exists()) {
            AwayFromAuction.getLogger().warn("Cache Not Found!");
            return;
        }

        try {
            Scanner scanner = new Scanner(syncCache);
            String content = scanner.nextLine();
            scanner.close();

            AwayFromAuction.getLogger().info("Content Length: " + content.length());

            Auction[] auctions = GSON.fromJson(content, Auction[].class);

            AwayFromAuction.getLogger().info("Auction Length: " + auctions.length);

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
}