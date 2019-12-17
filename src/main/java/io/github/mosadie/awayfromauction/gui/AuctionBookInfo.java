package io.github.mosadie.awayfromauction.gui;

import java.time.Duration;

import io.github.mosadie.awayfromauction.util.AfAUtils;
import io.github.mosadie.awayfromauction.util.Auction;
import net.minecraft.client.gui.screen.ReadBookScreen.IBookInfo;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.event.HoverEvent.Action;

public class AuctionBookInfo implements IBookInfo {
    private final Auction auction;
    
    /**
    * @return Total number of pages in the book.
    */
    @Override
    public int func_216918_a() {
        return 3;
    }
    
    /**
    * Get the content of a specified page.
    * @param page The page (zero indexed) to get content for.
    * @return An ITextComponent for the specified page.
    */
    @Override
    public ITextComponent func_216915_a(int page) {
        StringTextComponent root = new StringTextComponent("Auction Details for \n");
        StringTextComponent sibling = new StringTextComponent(auction.getItemName() + (auction.getItemStack().getCount() > 0 ? " x" + auction.getItemStack().getCount() : "") + "\n\n");
        sibling.getStyle()
        .setUnderlined(true)
        .setColor(AfAUtils.getColorFromTier(auction.getTier()))
        .setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new StringTextComponent(auction.getItemLore())));
        
        root.appendSibling(sibling);
        
        switch(page) {
            case 0: // Overview
            StringTextComponent owner = new StringTextComponent("Auction Owner: ");
            StringTextComponent ownerName = new StringTextComponent(auction.getAFA().getPlayerName(auction.getAuctionOwnerUUID()));
            ownerName.getStyle()
            .setUnderlined(true)
            .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa searchuser " + auction.getAFA().getPlayerName(auction.getAuctionOwnerUUID())))
            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to view other auctions by " + auction.getAFA().getPlayerName(auction.getAuctionOwnerUUID()))));
            owner.appendSibling(ownerName);
            owner.appendText("\n");
            
            StringTextComponent currentBid;
            if (auction.getHighestBid() != null) {
                currentBid = new StringTextComponent("Current bid: " + auction.getHighestBidAmount() + " by " + auction.getAFA().getPlayerName(auction.getHighestBid().getBidderUUID()) + "\n");
            } else {
                currentBid = new StringTextComponent("Starting bid: " + auction.getStartingBid() + "\n");
            }
            
            StringTextComponent timeLeft;
            if (auction.getEnd().before(auction.getSyncTimestamp())) {
                timeLeft = new StringTextComponent("Time Left: Ended!\n");
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
                timeLeft = new StringTextComponent("Time Left: " + timeLeftString + "\n");
            }
            
            root.appendSibling(owner);
            root.appendSibling(currentBid);
            root.appendSibling(timeLeft);
            break;
            
            case 1: // Item Info
            StringTextComponent name = new StringTextComponent("Item Name: ");
            StringTextComponent nameLink = new StringTextComponent(auction.getItemName());
            nameLink.getStyle()
            .setUnderlined(true)
            .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa search " + auction.getItemName()))
            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to view all auctions for " + auction.getItemName())));
            name.appendSibling(nameLink);
            name.appendText("\n");
            
            StringTextComponent rarity = new StringTextComponent("Rarity: " + auction.getTier() + "\n");
            
            StringTextComponent count = new StringTextComponent("Item Count: " + auction.getItemCount() + "\n");
            
            StringTextComponent lore = new StringTextComponent("Lore: ");
            StringTextComponent loreHover = new StringTextComponent("Hover");
            loreHover.getStyle().setUnderlined(true).setColor(AfAUtils.getColorFromTier(auction.getTier())).setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new StringTextComponent(auction.getItemLore())));
            lore.appendSibling(loreHover);
            lore.appendSibling(new StringTextComponent("\n"));
            
            root.appendSibling(name);
            root.appendSibling(rarity);
            root.appendSibling(lore);
            root.appendSibling(count);
            break;
            
            case 2: // UUID Copy / Join Hypixel
            StringTextComponent info = new StringTextComponent("Auction UUID: (Click to get view command in chat)\n");
            StringTextComponent uuid = new StringTextComponent(auction.getAuctionUUID().toString() + "\n");
            uuid.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa view " + auction.getAuctionUUID().toString()));
            
            StringTextComponent joinHypixel = new StringTextComponent("\nClick HERE to join the Hypixel server!");
            joinHypixel.getStyle().setUnderlined(true).setBold(true).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa joinhypixel"));
            
            root.appendSibling(info);
            root.appendSibling(uuid);
            if (!auction.getAFA().onHypixel()) root.appendSibling(joinHypixel);
        }
        
        return root;
    }
    
    public AuctionBookInfo(Auction auction) {
        this.auction = auction;
    }
    
}