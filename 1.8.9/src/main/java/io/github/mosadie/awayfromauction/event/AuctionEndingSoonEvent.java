package io.github.mosadie.awayfromauction.event;

import java.time.Duration;
import java.time.Instant;

import io.github.mosadie.awayfromauction.core.Auction;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Represents an auction ending soon. This event may be sent multiple times for
 * the same auction.
 */
public class AuctionEndingSoonEvent extends Event {
    private final Auction auctionState;

    public AuctionEndingSoonEvent(Auction auctionState) {
        super();
        this.auctionState = auctionState;
    }

    public Auction getAuction() {
        return auctionState;
    }

    /**
     * @return How long is left on the auction.
     */
    public Duration getTimeLeft() {
        return Duration.between(auctionState.getEnd().toInstant(), Instant.now());
    }
}