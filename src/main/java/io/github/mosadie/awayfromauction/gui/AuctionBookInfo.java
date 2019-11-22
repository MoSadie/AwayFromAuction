package io.github.mosadie.awayfromauction.gui;

import java.time.Duration;

import io.github.mosadie.awayfromauction.AwayFromAuction;
import io.github.mosadie.awayfromauction.util.Auction;
import net.minecraft.client.gui.screen.ReadBookScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.event.HoverEvent.Action;

public class AuctionBookInfo implements ReadBookScreen.IBookInfo {
    private final Auction auction;

    @Override
    public int func_216918_a() { // Number of pages
        return 3;
    }

    @Override
    public ITextComponent func_216915_a(int page) { // Get text for a specific page, starting at 0
        StringTextComponent root = new StringTextComponent("Auction Details for \n");
        StringTextComponent sibling = new StringTextComponent(auction.getItemName() + "\n\n");
        sibling.getStyle()
                .setUnderlined(true)
                .setColor(getColorFromTier(auction.getTier()))
                .setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new StringTextComponent(auction.getItemLore())));
                
        root.appendSibling(sibling);

        switch(page) {
            case 0: // Overview
            StringTextComponent owner = new StringTextComponent("Auction Owner:\n" + auction.getAWA().getPlayerName(auction.getAuctionOwnerUUID()) + "\n");

            StringTextComponent currentBid;
            if (auction.getHighestBid() != null) {
                currentBid = new StringTextComponent("Current bid: " + auction.getHighestBidAmount() + " by " + auction.getAWA().getPlayerName(auction.getHighestBid().getBidderUUID()) + "\n");
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
            StringTextComponent name = new StringTextComponent("Item Name: " + auction.getItemName() + "\n");

            StringTextComponent rarity = new StringTextComponent("Rarity: " + auction.getTier() + "\n");

            StringTextComponent lore = new StringTextComponent("Lore: ");
            StringTextComponent loreHover = new StringTextComponent("Hover");
            loreHover.getStyle().setUnderlined(true).setColor(getColorFromTier(auction.getTier())).setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new StringTextComponent(auction.getItemLore())));
            lore.appendSibling(loreHover);
            lore.appendSibling(new StringTextComponent("\n"));

            root.appendSibling(name);
            root.appendSibling(rarity);
            root.appendSibling(lore);
            break;

            case 2: // UUID Copy / Join Hypixel
            StringTextComponent info = new StringTextComponent("Auction UUID: (Click to get view command in chat)\n");
            StringTextComponent uuid = new StringTextComponent(auction.getAuctionUUID().toString() + "\n");
            uuid.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa view " + auction.getAuctionUUID().toString()));

            StringTextComponent joinHypixel = new StringTextComponent("\nClick HERE to join the Hypixel server!");
            joinHypixel.getStyle().setUnderlined(true).setBold(true).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa joinhypixel"));

            root.appendSibling(info);
            root.appendSibling(uuid);
            root.appendSibling(joinHypixel);
        }

        return root;
    }

    private TextFormatting getColorFromTier(String tier) {
        switch(tier) {
            case "COMMON":
                return TextFormatting.GRAY;
            
            case "UNCOMMON":
                return TextFormatting.GREEN;

            case "RARE":
                return TextFormatting.DARK_BLUE;
                
            case "EPIC":
                return TextFormatting.DARK_PURPLE;
                
            case "LEGENDARY":
                return TextFormatting.GOLD;

            default:
                AwayFromAuction.getLogger().info("Unknown tier type! " + tier);
                return TextFormatting.GRAY;
        }
    }

    public AuctionBookInfo(Auction auction) {
        this.auction = auction;
    }

}