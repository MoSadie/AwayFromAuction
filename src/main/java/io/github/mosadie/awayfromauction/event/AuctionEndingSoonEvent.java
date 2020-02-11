package io.github.mosadie.awayfromauction.event;

import java.time.Duration;
import java.time.Instant;

import io.github.mosadie.awayfromauction.util.Auction;
import net.minecraftforge.fml.common.eventhandler.Event;

public class AuctionEndingSoonEvent extends Event {
    private final Auction auctionState;

    public AuctionEndingSoonEvent(Auction auctionState) {
        super();
        this.auctionState = auctionState;
    }

    public Auction getAuction() {
        return auctionState;
    }

    public Duration getTimeLeft() {
        return Duration.between(auctionState.getEnd().toInstant(), Instant.now());
    }
}