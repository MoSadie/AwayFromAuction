package io.github.mosadie.awayfromauction.event;

import io.github.mosadie.awayfromauction.util.Auction;
import io.github.mosadie.awayfromauction.util.Auction.Bid;
import net.minecraftforge.fml.common.eventhandler.Event;

public class AuctionNewBidEvent extends Event {
    private final Auction auctionState;

    public AuctionNewBidEvent(Auction auctionState) {
        super();
        this.auctionState = auctionState;
    }

    public Auction getAuction() {
        return auctionState;
    }

    public Bid getBid() {
        return auctionState.getHighestBid();
    }
}