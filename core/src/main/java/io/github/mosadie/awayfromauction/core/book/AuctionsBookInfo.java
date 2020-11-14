package io.github.mosadie.awayfromauction.core.book;

import io.github.mosadie.awayfromauction.core.AfAUtils;
import io.github.mosadie.awayfromauction.core.Auction;
import io.github.mosadie.awayfromauction.core.text.ClickEvent;
import io.github.mosadie.awayfromauction.core.text.HoverEvent;
import io.github.mosadie.awayfromauction.core.text.ITextComponent;
import io.github.mosadie.awayfromauction.core.text.StringComponent;

import java.time.Duration;
import java.util.Date;

public class AuctionsBookInfo implements IBookInfo {

    private final Auction[] auctions;

    public AuctionsBookInfo(Auction[] auctions) {
        this.auctions = auctions;
    }

    /**
     * @return The total number of pages in the book.
     */
    @Override
    public int getTotalPages() {
        return auctions.length;
    }

    /**
     * Gets the content of a specified page.
     * 
     * @param page The page (zero indexed) to get content for.
     * @return An ITextComponent with the content for the specified page.
     */
    @Override
    public ITextComponent getPageContent(int page) {
        StringComponent root = new StringComponent("Auction Details:\n");

        Auction auction = auctions[page];

        ITextComponent itemTitle = new StringComponent(
                auction.getItemName() + (auction.getItemCount() > 0 ? " x" + auction.getItemCount() : "")).setUnderlined(true).setColor(AfAUtils.getColorFromTier(auction.getTier()))
                .setHoverEvent(
                        new HoverEvent(HoverEvent.HoverAction.SHOW_TEXT, new StringComponent(auction.getItemLore())));

        StringComponent newLine = new StringComponent("\n\n");

        String ownerUsername = auction.getAFA().getCore().getPlayerName(auction.getAuctionOwnerUUID());
        if (ownerUsername == null) { ownerUsername = auction.getAuctionOwnerUUID().toString(); }

        StringComponent owner = new StringComponent("Auction Owner: ");
        ITextComponent ownerLink = new StringComponent(ownerUsername).setUnderlined(true)
                .setClickEvent(new ClickEvent(ClickEvent.ClickAction.RUN_COMMAND,
                        "/afa searchuser " + auction.getAuctionOwnerUUID()))
                .setHoverEvent(new HoverEvent(HoverEvent.HoverAction.SHOW_TEXT,
                        new StringComponent("Click to view all auctions by "
                                + ownerUsername)));
        owner.appendSibling(ownerLink);
        owner.appendText("\n\n");

        StringComponent currentBid;
        if (auction.isBIN()) {
            currentBid = new StringComponent("BIN Price: " + auction.getStartingBid() + "\n\n");
        } else if (auction.getHighestBid() != null) {
            String highestBidderUsername = auction.getAFA().getCore().getPlayerName(auction.getHighestBid().getBidderUUID());
            if (highestBidderUsername == null) { highestBidderUsername = auction.getHighestBid().getBidderUUID().toString(); }
            currentBid = new StringComponent("Current bid: " + auction.getHighestBidAmount() + " by "
                    + highestBidderUsername + "\n\n");
        } else {
            currentBid = new StringComponent("Starting bid: " + auction.getStartingBid() + "\n\n");
        }

        StringComponent timeLeft;
        if (auction.getEnd().before(auction.getSyncTimestamp())) {
            timeLeft = new StringComponent("Time Left: Ended!\n\n");
        } else {
            Duration time = Duration.between(auction.getSyncTimestamp().toInstant(), auction.getEnd().toInstant());
            String timeLeftString = "";
            if (time.toDays() > 0) {
                timeLeftString += time.toDays() + " day" + (time.toDays() > 1 ? "s" : "") + " ";
                time = time.minusDays(time.toDays());
            }
            if (time.toHours() > 0) {
                timeLeftString += time.toHours() + " hour" + (time.toHours() > 1 ? "s" : "") + " ";
                time = time.minusHours(time.toHours());
            }
            if (time.toMinutes() > 0) {
                timeLeftString += time.toMinutes() + " minute" + (time.toMinutes() > 1 ? "s" : "") + " ";
                time = time.minusMinutes(time.toMinutes());
            }
            if (time.getSeconds() > 0) {
                timeLeftString += time.getSeconds() + " second" + (time.getSeconds() > 1 ? "s" : "") + " ";
                time = time.minusSeconds(time.getSeconds());
            }
            timeLeft = new StringComponent("Time Left: " + timeLeftString + "\n\n");
        }
        Duration timeSinceSync = Duration.between(auction.getSyncTimestamp().toInstant(), new Date().toInstant());
        timeLeft.setHoverEvent(new HoverEvent(HoverEvent.HoverAction.SHOW_TEXT,
                new StringComponent("Time since last sync: " + timeSinceSync.getSeconds() + " seconds.")));

        root.appendSibling(itemTitle);
        root.appendSibling(newLine);
        root.appendSibling(owner);
        root.appendSibling(currentBid);
        root.appendSibling(timeLeft);

        return root;
    }

}