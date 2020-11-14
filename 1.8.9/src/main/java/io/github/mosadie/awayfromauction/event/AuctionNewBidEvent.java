package io.github.mosadie.awayfromauction.event;

import io.github.mosadie.awayfromauction.core.Auction;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Represents a new bid being placed on a tracked auction.
 */
public class AuctionNewBidEvent extends Event {
    private final Auction auctionState;

    public AuctionNewBidEvent(Auction auctionState) {
        super();
        this.auctionState = auctionState;
    }

    public Auction getAuction() {
        return auctionState;
    }

    /**
     * @return The new bid.
     */
    public Auction.Bid getBid() {
        return auctionState.getHighestBid();
    }
}