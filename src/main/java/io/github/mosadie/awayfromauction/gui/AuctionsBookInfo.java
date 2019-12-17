package io.github.mosadie.awayfromauction.gui;

import java.time.Duration;

import io.github.mosadie.awayfromauction.util.AfAUtils;
import io.github.mosadie.awayfromauction.util.Auction;
import net.minecraft.client.gui.screen.ReadBookScreen.IBookInfo;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

public class AuctionsBookInfo implements IBookInfo {
    
    private final Auction[] auctions;
    
    public AuctionsBookInfo(Auction[] auctions) {
        this.auctions = auctions;
    }
    
    /**
     * @return The total number of pages in the book.
     */
    @Override
    public int func_216918_a() {
        return auctions.length;
    }
    
    /**
     * Gets the content of a specified page.
     * @param page The page (zero indexed) to get content for.
     * @return An ITextComponent with the content for the specified page.
     */
    @Override
    public ITextComponent func_216915_a(int page) {
        StringTextComponent root = new StringTextComponent("Auction Details:\n");
        
        Auction auction = auctions[page];
        
        StringTextComponent itemTitle = new StringTextComponent(auction.getItemName() + (auction.getItemCount() > 0 ? " x" + auction.getItemCount() : "") + "\n");
        itemTitle.getStyle()
        .setUnderlined(true)
        .setColor(AfAUtils.getColorFromTier(auction.getTier()))
        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(auction.getItemLore())));
        
        StringTextComponent owner = new StringTextComponent("Auction Owner: ");
        StringTextComponent ownerLink = new StringTextComponent(auction.getItemName());
        ownerLink.getStyle()
        .setUnderlined(true)
        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa searchuser " + auction.getItemName()))
        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to view all auctions by " + auction.getItemName())));
        owner.appendSibling(ownerLink);
        owner.appendText("\n\n");
        
        StringTextComponent currentBid;
        if (auction.getHighestBid() != null) {
            currentBid = new StringTextComponent("Current bid: " + auction.getHighestBidAmount() + " by " + auction.getAFA().getPlayerName(auction.getHighestBid().getBidderUUID()) + "\n\n");
        } else {
            currentBid = new StringTextComponent("Starting bid: " + auction.getStartingBid() + "\n\n");
        }
        
        StringTextComponent timeLeft;
        if (auction.getEnd().before(auction.getSyncTimestamp())) {
            timeLeft = new StringTextComponent("Time Left: Ended!\n\n");
        } else {
            Duration time = Duration.between(auction.getSyncTimestamp().toInstant(), auction.getEnd().toInstant());
            String timeLeftString = "";
            if (time.toDays() > 0) {
                timeLeftString += time.toDays() + " day" + (time.toDays() > 1 ? "s" : "" ) + " ";
                time = time.minusDays(time.toDays());
            }
            if (time.toHours() > 0) {
                timeLeftString += time.toHours() + " hour" + (time.toHours() > 1 ? "s" : "" ) + " ";
                time = time.minusHours(time.toHours());
            }
            if (time.toMinutes() > 0) {
                timeLeftString += time.toMinutes() + " minute" + (time.toMinutes() > 1 ? "s" : "" ) + " ";
                time = time.minusMinutes(time.toMinutes());
            }
            if (time.getSeconds() > 0) {
                timeLeftString += time.getSeconds() + " second" + (time.getSeconds() > 1 ? "s" : "") + " ";
                time = time.minusSeconds(time.getSeconds());
            }
            timeLeft = new StringTextComponent("Time Left: " + timeLeftString + "\n\n");
        }
        
        root.appendSibling(itemTitle);
        root.appendSibling(owner);
        root.appendSibling(currentBid);
        root.appendSibling(timeLeft);
        
        return root;
    }
    
}