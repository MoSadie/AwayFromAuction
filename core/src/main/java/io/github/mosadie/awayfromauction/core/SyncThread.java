package io.github.mosadie.awayfromauction.core;

import net.hypixel.api.HypixelAPI;
import net.hypixel.api.reply.skyblock.BazaarReply;
import net.hypixel.api.reply.skyblock.SkyBlockAuctionsReply;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SyncThread extends Thread {
    private final AwayFromAuctionCore core;
    private boolean firstSyncDone = false;
    private boolean stopFlag = false;

    public SyncThread(AwayFromAuctionCore core) {
        setName("AwayFromAuctionCore-Sync");
        this.core = core;
    }

    @Override
    public void run() {

        if (core.getSyncCache().exists()) {
            core.loadFromCache();
        } else {
            core.saveToCache();
        }

        while (!this.isInterrupted() && !stopFlag && !core.getAfA().isMinecraftNull()) {
            try {
                Thread.sleep(core.getAfA().getConfigState().getGeneralRefreshDelay() * 1000);
                if (!stopFlag) {
                    core.getAfA().logInfo("Syncing with Hypixel Skyblock Auction House");
                    sync();
                    core.getAfA().logInfo("Sync finished!");
                    firstSyncDone = true;
                } else {
                    // All is fine, silently fall out, the stop flag was set while we were sleeping.
                }
            } catch (InterruptedException e) {
                // Do nothing, it's fine.
            }
        }
    }

    /**
     * Check if the initial sync has been completed.
     * @return True if the initial sync has been completed, false otherwise.
     */
    public boolean isFirstSyncDone() {
        return firstSyncDone;
    }

    /**
     * Syncs all active auction and bazaar data from the Hypixel API and
     * https://sky.lea.moe. Sorts that data into multiple groups and caches it.
     */
    private void sync() {
        if (core.getAfA().getCurrentPlayerUUID() == null) {
            core.getAfA().logInfo("Player is null, skipping sync.");
            return;
        }
        HypixelAPI hypixelApi = core.getOrRefreshHypixelAPI();

        // Get updated auction house state

        Map<UUID, Auction> tmpAllAuctions = new HashMap<>();
        Map<UUID, List<Auction>> tmpPlayerAuctionMap = new HashMap<>();
        Map<String, List<Auction>> tmpItemAuctionMap = new HashMap<>();
        List<UUID> tmpBidAuctions = new ArrayList<>();
        long tmpTotalCoins = 0;
        try {
            // Sync the first page.
            SkyBlockAuctionsReply reply = hypixelApi.getSkyBlockAuctions(0).get(1, TimeUnit.MINUTES);
            int pageTotal = reply.getTotalPages();
            core.getAfA().logInfo("Syncing " + pageTotal + " pages of auctions");
            if (reply.getAuctions().size() > 0) {
                core.getAfA().logInfo("Syncing auction house page 1 (" + reply.getAuctions().size() + " auctions)");
                for (int i = 0; i < reply.getAuctions().size(); i++) {
                    Auction tmpAuction = new Auction(reply.getAuctions().get(i).getAsJsonObject(), core.getAfA());

                    // Fetch Username
                    core.cachePlayer(tmpAuction.getAuctionOwnerUUID());

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
                    if (AfAUtils.bidsContainUUID(tmpAuction.getBids(), core.getAfA().getCurrentPlayerUUID())) {
                        if (!tmpBidAuctions.contains(tmpAuction.getAuctionUUID()))
                            tmpBidAuctions.add(tmpAuction.getAuctionUUID());
                    }

                    // Add to to the total coins in the auction house.
                    for (Auction.Bid bid : tmpAuction.getBids()) {
                        tmpTotalCoins += bid.getAmount();
                    }
                }
            } else {
                core.getAfA().logError("Attempted to get auctions, but no auctions found!");
            }

            // Repeat for the rest of the pages.
            for (int p = 1; p < pageTotal; p++) {
                core.getAfA().logInfo(
                        "Syncing auction house page " + (p + 1) + " (" + reply.getAuctions().size() + " auctions)");
                SkyBlockAuctionsReply replyPage = hypixelApi.getSkyBlockAuctions(p).get(1, TimeUnit.MINUTES);
                for (int i = 0; i < replyPage.getAuctions().size(); i++) {
                    Auction tmpAuction = new Auction(replyPage.getAuctions().get(i).getAsJsonObject(), core.getAfA());

                    // Fetch Username
                    core.cachePlayer(tmpAuction.getAuctionOwnerUUID());

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
                    if (AfAUtils.bidsContainUUID(tmpAuction.getBids(), core.getAfA().getCurrentPlayerUUID())) {
                        if (!tmpBidAuctions.contains(tmpAuction.getAuctionUUID()))
                            tmpBidAuctions.add(tmpAuction.getAuctionUUID());
                    }

                    // Add to to the total coins in the auction house.
                    for (Auction.Bid bid : tmpAuction.getBids()) {
                        tmpTotalCoins += bid.getAmount();
                    }
                }
            }

            // Sync Bazaar

            Map<String, BazaarReply.Product> tmpBazaarMap = fetchBazaarState();

            // Look for events

            // - Ending Soon -
            if (tmpPlayerAuctionMap.containsKey(core.getAfA().getCurrentPlayerUUID())) {
                for (Auction auction : tmpPlayerAuctionMap.get(core.getAfA().getCurrentPlayerUUID())) {
                    if (auction.getEnd().getTime() - auction.getSyncTimestamp().getTime() < (5 * 60 * 1000)) {
                        core.getAfA().createEndingSoonEvent(auction);
                    }
                }
            }

            // - Outbid -
            Map<UUID, Auction> tmpOutbidAuctionMap = new HashMap<>();
            for (UUID incomingAuctionUUID : tmpBidAuctions) {
                Auction incomingAuction = tmpAllAuctions.get(incomingAuctionUUID);
                tmpOutbidAuctionMap.put(incomingAuction.getAuctionUUID(), incomingAuction);
            }

            for (Auction currentAuction : core.getBidOnAuctions()) {
                if (tmpOutbidAuctionMap.containsKey(currentAuction.getAuctionUUID())) {
                    Auction newAuction = tmpOutbidAuctionMap.get(currentAuction.getAuctionUUID());

                    boolean newHighBid = currentAuction.getHighestBidAmount() < newAuction.getHighestBidAmount();
                    boolean notThePlayer = !newAuction.getHighestBid().getBidderUUID().equals(core.getAfA().getCurrentPlayerUUID());
                    if (newHighBid && notThePlayer) {
                        core.getAfA().createOutbidEvent(newAuction);
                    }
                }
            }

            // - New Bid -
            List<Auction> tmpCurrentOwnedAuctions = core.getPlayerAuctions(core.getAfA().getCurrentPlayerUUID());
            List<Auction> tmpNewOwnedAuctions = new ArrayList<>();

            if (tmpPlayerAuctionMap.containsKey(core.getAfA().getCurrentPlayerUUID())) {
                tmpNewOwnedAuctions = tmpPlayerAuctionMap.get(core.getAfA().getCurrentPlayerUUID());
            }

            Map<UUID, Auction> tmpNewBidAuctionMap = new HashMap<>();
            for (Auction auction : tmpNewOwnedAuctions) {
                tmpNewBidAuctionMap.put(auction.getAuctionUUID(), auction);
            }
            for (Auction auction : tmpCurrentOwnedAuctions) {
                if (tmpNewBidAuctionMap.containsKey(auction.getAuctionUUID())) {
                    Auction newState = tmpNewBidAuctionMap.get(auction.getAuctionUUID());
                    if (newState.getBids().length > auction.getBids().length) {
                        core.getAfA().createNewBidEvent(newState);
                    }
                }
            }

            // Update core with new data
            core.setAuctionState(tmpAllAuctions, tmpPlayerAuctionMap, tmpItemAuctionMap, tmpBidAuctions, tmpTotalCoins);
            core.setBazaarState(tmpBazaarMap);

            // Save to cache
            core.saveToCache();

        } catch (InterruptedException | ExecutionException | TimeoutException | NullPointerException e) {
            core.getAfA().logWarn("An exception occurred while attempting to sync auction details: " + e.getMessage());
            core.getAfA().logException(e);
        }
    }

    /**
     * Gets the current bazaar state from the Hypixel API as an map of
     * {@link BazaarReply.Product} objects.
     *
     * @return The current state of the bazaar as an map of Bazaar Item IDs to
     *         {@link BazaarReply.Product} objects.
     */
    private Map<String, BazaarReply.Product> fetchBazaarState() {
        try {
            HypixelAPI hypixelAPI = core.getOrRefreshHypixelAPI();
            BazaarReply reply = hypixelAPI.getBazaar().get(1, TimeUnit.MINUTES);
            return reply.getProducts();
        } catch (Exception e) {
            core.getAfA().logError("Exception encountered getting Bazaar state from sky.lea.moe! Exception: "
                    + e.getLocalizedMessage());
            core.getAfA().logException(e);
            return null;
        }
    }

    public void stopFlag() {
        stopFlag = true;
    }
}
