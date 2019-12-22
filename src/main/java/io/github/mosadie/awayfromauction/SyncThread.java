package io.github.mosadie.awayfromauction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.github.mosadie.awayfromauction.util.AfAUtils;
import io.github.mosadie.awayfromauction.util.Auction;
import io.github.mosadie.awayfromauction.util.Auction.Bid;
import net.hypixel.api.HypixelAPI;
import net.hypixel.api.reply.skyblock.SkyBlockAuctionsReply;
import net.minecraft.client.Minecraft;

public class SyncThread extends Thread {
    private final AwayFromAuction afa;
    private boolean stopFlag = false;
    
    public SyncThread(AwayFromAuction mod) {
        this.afa = mod;
    }
    
    @Override
    public void run() {
        while (!this.isInterrupted() && !stopFlag && Minecraft.getMinecraft() != null) {
            try {
                Thread.sleep(Config.GENERAL_REFRESH_DELAY * 1000);
                AwayFromAuction.getLogger().info("Syncing with Hypixel Skyblock Auction House");
                sync();
                AwayFromAuction.getLogger().info("Sync finished!");
            } catch (InterruptedException e) {
                // Do nothing, it's fine.
            }
        }
    }
    
    /**
    * Syncs all active auction data from the Hypixel API.
    * Sorts that data into multiple groups and calls {@link AwayFromAuction#updateAuctions(Map, Map, Map, List)} to update the auction cache.
    */
    private void sync() {
        if (Minecraft.getMinecraft().thePlayer == null) {
            return;
        }
        HypixelAPI hypixelApi = afa.getOrRefreshHypixelAPI();
        Map<UUID, Auction> allAuctions = new HashMap<>();
        Map<UUID, List<Auction>> playerAuctionMap = new HashMap<>();
        Map<String, List<Auction>> itemAuctionMap = new HashMap<>();
        List<Auction> bidAuctions = new ArrayList<>();
        long totalCoins = 0;
        try {
            SkyBlockAuctionsReply reply = hypixelApi.getSkyBlockAuctions(0).get(1, TimeUnit.MINUTES);
            int pageTotal = reply.getTotalPages();
            if (reply.getAuctions().size() > 0) {
                for (int i = 0; i < reply.getAuctions().size(); i++) {
                    Auction tmpAuction = new Auction(reply.getAuctions().get(i).getAsJsonObject(), afa);
                    
                    allAuctions.put(tmpAuction.getAuctionUUID(), tmpAuction);
                    
                    if (!playerAuctionMap.containsKey(tmpAuction.getAuctionOwnerUUID())) {
                        playerAuctionMap.put(tmpAuction.getAuctionOwnerUUID(), new ArrayList<>());
                    }
                    playerAuctionMap.get(tmpAuction.getAuctionOwnerUUID()).add(tmpAuction);
                    
                    if (!itemAuctionMap.containsKey(tmpAuction.getItemName().toLowerCase())) {
                        itemAuctionMap.put(tmpAuction.getItemName().toLowerCase(), new ArrayList<>());
                    }
                    itemAuctionMap.get(tmpAuction.getItemName().toLowerCase()).add(tmpAuction);
                    
                    if (AfAUtils.bidsContainUUID(tmpAuction.getBids(), Minecraft.getMinecraft().thePlayer.getUniqueID())) bidAuctions.add(tmpAuction);
                    
                    for (Bid bid : tmpAuction.getBids()) {
                        totalCoins += bid.getAmount();
                    }
                }
            } else {
                AwayFromAuction.getLogger().error("Attempted to get auctions, but no auctions found!");
            }
            for (int p = 1; p < pageTotal; p++) {
                SkyBlockAuctionsReply replyPage = hypixelApi.getSkyBlockAuctions(p).get(1, TimeUnit.MINUTES);
                for (int i = 0; i < replyPage.getAuctions().size(); i++) {
                    Auction tmpAuction = new Auction(replyPage.getAuctions().get(i).getAsJsonObject(), afa);
                    
                    allAuctions.put(tmpAuction.getAuctionUUID(), tmpAuction);
                    
                    if (!playerAuctionMap.containsKey(tmpAuction.getAuctionOwnerUUID())) {
                        playerAuctionMap.put(tmpAuction.getAuctionOwnerUUID(), new ArrayList<>());
                    }
                    playerAuctionMap.get(tmpAuction.getAuctionOwnerUUID()).add(tmpAuction);
                    
                    if (!itemAuctionMap.containsKey(tmpAuction.getItemName().toLowerCase())) {
                        itemAuctionMap.put(tmpAuction.getItemName().toLowerCase(), new ArrayList<>());
                    }
                    itemAuctionMap.get(tmpAuction.getItemName().toLowerCase()).add(tmpAuction);
                    
                    if (AfAUtils.bidsContainUUID(tmpAuction.getBids(), Minecraft.getMinecraft().thePlayer.getUniqueID())) bidAuctions.add(tmpAuction);
                    
                    for (Bid bid : tmpAuction.getBids()) {
                        totalCoins += bid.getAmount();
                    }
                }
            }
            afa.updateAuctions(allAuctions, playerAuctionMap, itemAuctionMap, bidAuctions);
            afa.setTotalCoins(totalCoins);
        } catch (InterruptedException | ExecutionException | TimeoutException | NullPointerException e) {
            AwayFromAuction.getLogger().warn("An exception occured while attempting to sync auction details: " + e.getLocalizedMessage());
            AwayFromAuction.getLogger().catching(e);
        }
    }
    
    public void stopFlag() {
        stopFlag = true;
    }
    
}