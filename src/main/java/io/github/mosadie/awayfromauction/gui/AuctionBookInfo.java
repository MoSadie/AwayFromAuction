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
     * @return An IChatComponent for the specified page.
     */
    @Override
    public IChatComponent getPageContent(int page) {
        ChatComponentText root = new ChatComponentText("Auction Details for \n");
        ChatComponentText sibling = new ChatComponentText(
                auction.getItemName() + (auction.getItemStack() != null && auction.getItemStack().stackSize > 0
                        ? " x" + auction.getItemStack().stackSize
                        : ""));
        sibling.getChatStyle().setUnderlined(true).setColor(AfAUtils.getColorFromTier(auction.getTier()))
                .setChatHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(auction.getItemLore())));

        ChatComponentText newlines = new ChatComponentText("\n\n");

        root.appendSibling(sibling);
        root.appendSibling(newlines);

        switch (page) {
            case 0: // Overview
                ChatComponentText owner = new ChatComponentText("Auction Owner: ");
                ChatComponentText ownerName = new ChatComponentText(
                        auction.getAFA().getUsernameCached(auction.getAuctionOwnerUUID()));
                ownerName.getChatStyle().setUnderlined(true)
                        .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/afa searchuser " + auction.getAuctionOwnerUUID().toString()))
                        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new ChatComponentText("Click to view other auctions by "
                                        + auction.getAFA().getUsernameCached(auction.getAuctionOwnerUUID()))));
                owner.appendSibling(ownerName);
                owner.appendText("\n\n");

                ChatComponentText currentBid;
                if (auction.isBIN()) {
                    currentBid = new ChatComponentText("BIN Price: " + auction.getStartingBid() + "\n\n");
                } else if (auction.getHighestBid() != null) {
                    currentBid = new ChatComponentText("Current bid: " + auction.getHighestBidAmount() + " by "
                            + auction.getAFA().getUsernameCached(auction.getHighestBid().getBidderUUID()) + "\n\n");
                } else {
                    currentBid = new ChatComponentText("Starting bid: " + auction.getStartingBid() + "\n\n");
                }

                ChatComponentText timeLeft;
                if (auction.getEnd().before(auction.getSyncTimestamp())) {
                    timeLeft = new ChatComponentText("Time Left: Ended!\n\n");
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
                    timeLeft = new ChatComponentText("Time Left: " + timeLeftString + "\n\n");
                }
                Duration timeSinceSync = Duration.between(auction.getSyncTimestamp().toInstant(),
                        new Date().toInstant());
                timeLeft.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText("Time since last sync: " + timeSinceSync.getSeconds() + " seconds.")));

                root.appendSibling(owner);
                root.appendSibling(currentBid);
                root.appendSibling(timeLeft);
                break;

            case 1: // Item Info
                ChatComponentText name = new ChatComponentText("Item Name: ");
                // name.getChatStyle().setColor(EnumChatFormatting.BLACK);
                ChatComponentText nameLink = new ChatComponentText(auction.getItemName());
                nameLink.getChatStyle().setUnderlined(true)
                        .setChatClickEvent(
                                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa search " + auction.getItemName()))
                        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new ChatComponentText("Click to view all auctions for " + auction.getItemName())));
                name.appendSibling(nameLink);
                name.appendText("\n\n");

                ChatComponentText rarity = new ChatComponentText("Rarity: " + auction.getTier() + "\n");

                ChatComponentText count = new ChatComponentText("Item Count: " + auction.getItemCount() + "\n");

                ChatComponentText lore = new ChatComponentText("Lore: ");
                ChatComponentText loreHover = new ChatComponentText("Hover");
                loreHover.getChatStyle().setUnderlined(true).setColor(AfAUtils.getColorFromTier(auction.getTier()))
                        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new ChatComponentText(auction.getItemLore())));
                lore.appendSibling(loreHover);
                lore.appendText("\n\n");

                root.appendSibling(name);
                root.appendSibling(rarity);
                root.appendSibling(lore);
                root.appendSibling(count);
                break;

            case 2: // UUID Copy / Join Hypixel
                ChatComponentText info = new ChatComponentText("Auction UUID: (Click to get link in chat)\n");
                ChatComponentText uuid = new ChatComponentText(auction.getAuctionUUID().toString() + "\n\n");
                uuid.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/afa view " + auction.getAuctionUUID().toString()));

                IChatComponent joinHypixel = AfAUtils.createHypixelLink();

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