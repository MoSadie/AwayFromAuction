package io.github.mosadie.awayfromauction.core.book;

import io.github.mosadie.awayfromauction.core.AfAUtils;
import io.github.mosadie.awayfromauction.core.Auction;
import io.github.mosadie.awayfromauction.core.text.ClickEvent;
import io.github.mosadie.awayfromauction.core.text.HoverEvent;
import io.github.mosadie.awayfromauction.core.text.ITextComponent;
import io.github.mosadie.awayfromauction.core.text.StringComponent;

import java.time.Duration;
import java.util.Date;

public class AuctionBookInfo implements IBookInfo {
    private final Auction auction;

    /**
     * @return Total number of pages in the book.
     */
    @Override
    public int getTotalPages() {
        return 3;
    }

    /**
     * Get the content of a specified page.
     * 
     * @param page The page (zero indexed) to get content for.
     * @return An ITextComponent for the specified page.
     */
    @Override
    public ITextComponent getPageContent(int page) {
        ITextComponent root = new StringComponent("Auction Details for \n");
        ITextComponent sibling = new StringComponent(
                auction.getItemName() + (auction.getItemCount() > 0
                        ? " x" + auction.getItemCount()
                        : "")).setUnderlined(true).setColor(AfAUtils.getColorFromTier(auction.getTier()))
                .setHoverEvent(
                        new HoverEvent(HoverEvent.HoverAction.SHOW_TEXT, new StringComponent(auction.getItemLore())));

        ITextComponent newlines = new StringComponent("\n\n");

        root.appendSibling(sibling);
        root.appendSibling(newlines);

        switch (page) {
            case 0: // Overview
                String ownerUsername = auction.getAFA().getCore().getPlayerName(auction.getAuctionOwnerUUID());
                if (ownerUsername == null) { ownerUsername = auction.getAuctionOwnerUUID().toString(); }

                StringComponent owner = new StringComponent("Auction Owner: ");
                ITextComponent ownerName = new StringComponent(ownerUsername).setUnderlined(true)
                        .setClickEvent(new ClickEvent(ClickEvent.ClickAction.RUN_COMMAND,
                                "/afa searchuser " + auction.getAuctionOwnerUUID().toString()))
                        .setHoverEvent(new HoverEvent(HoverEvent.HoverAction.SHOW_TEXT,
                                new StringComponent("Click to view other auctions by " + ownerUsername)));
                owner.appendSibling(ownerName);
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
                    Duration time = Duration.between(auction.getSyncTimestamp().toInstant(),
                            auction.getEnd().toInstant());
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
                Duration timeSinceSync = Duration.between(auction.getSyncTimestamp().toInstant(),
                        new Date().toInstant());
                timeLeft.setHoverEvent(new HoverEvent(HoverEvent.HoverAction.SHOW_TEXT,
                        new StringComponent("Time since last sync: " + timeSinceSync.getSeconds() + " seconds.")));

                root.appendSibling(owner);
                root.appendSibling(currentBid);
                root.appendSibling(timeLeft);
                break;

            case 1: // Item Info
                StringComponent name = new StringComponent("Item Name: ");
                
                ITextComponent nameLink = new StringComponent(auction.getItemName()).setUnderlined(true)
                        .setClickEvent(
                                new ClickEvent(ClickEvent.ClickAction.RUN_COMMAND, "/afa search " + auction.getItemName()))
                        .setHoverEvent(new HoverEvent(HoverEvent.HoverAction.SHOW_TEXT,
                                new StringComponent("Click to view all auctions for " + auction.getItemName())));
                name.appendSibling(nameLink);
                name.appendText("\n\n");

                StringComponent rarity = new StringComponent("Rarity: " + auction.getTier() + "\n");

                StringComponent count = new StringComponent("Item Count: " + auction.getItemCount() + "\n");

                StringComponent lore = new StringComponent("Lore: ");
                ITextComponent loreHover = new StringComponent("Hover").setUnderlined(true).setColor(AfAUtils.getColorFromTier(auction.getTier()))
                        .setHoverEvent(new HoverEvent(HoverEvent.HoverAction.SHOW_TEXT,
                                new StringComponent(auction.getItemLore())));
                lore.appendSibling(loreHover);
                lore.appendText("\n\n");

                root.appendSibling(name);
                root.appendSibling(rarity);
                root.appendSibling(lore);
                root.appendSibling(count);
                break;

            case 2: // UUID Copy / Join Hypixel
                StringComponent info = new StringComponent("Auction UUID: (Click to get link in chat)\n");
                ITextComponent uuid = new StringComponent(auction.getAuctionUUID().toString() + "\n\n").setClickEvent(new ClickEvent(ClickEvent.ClickAction.RUN_COMMAND,
                        "/afa view " + auction.getAuctionUUID().toString()));

                ITextComponent joinHypixel = AfAUtils.createHypixelLink(auction.getAFA());

                root.appendSibling(info);
                root.appendSibling(uuid);
                root.appendSibling(joinHypixel);
        }

        return root;
    }

    public AuctionBookInfo(Auction auction) {
        this.auction = auction;
    }

}