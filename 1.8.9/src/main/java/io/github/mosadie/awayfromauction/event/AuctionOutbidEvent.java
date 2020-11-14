package io.github.mosadie.awayfromauction.event;

import io.github.mosadie.awayfromauction.core.Auction;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Represents being outbid on an auction.
 */
public class AuctionOutbidEvent extends Event {
    private final Auction auctionState;

    private final long coinDiff;

    public AuctionOutbidEvent(Auction auctionState) {
        super();
        this.auctionState = auctionState;

        Auction.Bid yourBid = null;
        for (Auction.Bid bid : auctionState.getBids()) {
            if (bid.getBidderUUID().equals(Minecraft.getMinecraft().thePlayer.getUniqueID())) {
                yourBid = bid;
                break;
            }
        }
        if (yourBid != null)
            coinDiff = auctionState.getHighestBidAmount() - (yourBid != null ? yourBid.getAmount() : 0);
        else
            coinDiff = 0;
    }

    public Auction getAuction() {
        return auctionState;
    }

    /**
     * @return The amount the player was outbid by.
     */
    public long getOutbidAmount() {
        return coinDiff;
    }
}