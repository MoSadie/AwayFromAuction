package io.github.mosadie.awayfromauction.gui;

import java.time.Duration;
import java.util.Date;

import io.github.mosadie.awayfromauction.util.AfAUtils;
import io.github.mosadie.awayfromauction.util.Auction;
import io.github.mosadie.awayfromauction.util.IBookInfo;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

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
     * @return An IChatComponent with the content for the specified page.
     */
    @Override
    public IChatComponent getPageContent(int page) {
        ChatComponentText root = new ChatComponentText("Auction Details:\n");

        Auction auction = auctions[page];

        ChatComponentText itemTitle = new ChatComponentText(
                auction.getItemName() + (auction.getItemCount() > 0 ? " x" + auction.getItemCount() : ""));
        itemTitle.getChatStyle().setUnderlined(true).setColor(AfAUtils.getColorFromTier(auction.getTier()))
                .setChatHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(auction.getItemLore())));

        ChatComponentText newLine = new ChatComponentText("\n\n");

        ChatComponentText owner = new ChatComponentText("Auction Owner: ");
        ChatComponentText ownerLink = new ChatComponentText(
                auction.getAFA().getUsernameCached(auction.getAuctionOwnerUUID()));
        ownerLink.getChatStyle().setUnderlined(true)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/afa searchuser " + auction.getAuctionOwnerUUID()))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText("Click to view all auctions by "
                                + auction.getAFA().getUsernameCached(auction.getAuctionOwnerUUID()))));
        owner.appendSibling(ownerLink);
        owner.appendText("\n\n");

        ChatComponentText currentBid;
        if (auction.isBIN()) {
            currentBid = new ChatComponentText("BIN Price: " + auction.getStartingBid() + "\n\n");
        } else if (auction.getHighestBid() != null) {
            currentBid = new ChatComponentText("Current bid: " + auction.getHighestBidAmount() + " by "
                    + auction.getAFA().getPlayerName(auction.getHighestBid().getBidderUUID()) + "\n\n");
        } else {
            currentBid = new ChatComponentText("Starting bid: " + auction.getStartingBid() + "\n\n");
        }

        ChatComponentText timeLeft;
        if (auction.getEnd().before(auction.getSyncTimestamp())) {
            timeLeft = new ChatComponentText("Time Left: Ended!\n\n");
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
            timeLeft = new ChatComponentText("Time Left: " + timeLeftString + "\n\n");
        }
        Duration timeSinceSync = Duration.between(auction.getSyncTimestamp().toInstant(), new Date().toInstant());
        timeLeft.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ChatComponentText("Time since last sync: " + timeSinceSync.getSeconds() + " seconds.")));

        root.appendSibling(itemTitle);
        root.appendSibling(newLine);
        root.appendSibling(owner);
        root.appendSibling(currentBid);
        root.appendSibling(timeLeft);

        return root;
    }

}