package io.github.mosadie.awayfromauction.gui;

import java.util.ArrayList;
import java.util.List;

import io.github.mosadie.awayfromauction.util.AfAUtils;
import io.github.mosadie.awayfromauction.util.Auction;
import net.minecraft.client.gui.screen.ReadBookScreen.IBookInfo;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

public class AuctionSearchBookInfo implements IBookInfo {

    private final String search;
    private final Auction[] auctions;

    private static final int RESULTS_PER_PAGE = 3;

    @Override
    public int func_216918_a() { // Number of pages
        return auctions.length/RESULTS_PER_PAGE + 1;
    }

    @Override
    public ITextComponent func_216915_a(int page) { // Get text on page
        ITextComponent root = new StringTextComponent("Search Results for: " + search + "\n\n");
        for (int i = 0; i < RESULTS_PER_PAGE; i++) {
            root.appendSibling(getAuctionLineOrBlank(page*RESULTS_PER_PAGE + i));
            root.appendText("\n\n");
        }
        return root;
    }

    public AuctionSearchBookInfo(Auction[] auctions, String search) {
        this.search = search;
        List<Auction> activeAuctions = new ArrayList<>();
        for (Auction auction : auctions) {
            if (!auction.getEnd().before(auction.getSyncTimestamp()))
                activeAuctions.add(auction);
        }
        this.auctions = activeAuctions.toArray(new Auction[0]);
    }

    private ITextComponent getAuctionLineOrBlank(int index) {
        if (index >= auctions.length) return new StringTextComponent("");
        Auction auction = auctions[index];
        StringTextComponent auctionTextComponent = new StringTextComponent(auction.getItemName() + (auction.getItemCount() > 0 ? " x" + auction.getItemCount() : "") + ": " + (auction.getHighestBidAmount() > 0 ? auction.getHighestBidAmount() : auction.getStartingBid()) + " coins");
        auctionTextComponent.getStyle()
                                .setColor(AfAUtils.getColorFromTier(auction.getTier()))
                                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa view " + auction.getAuctionUUID().toString()))
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Auctioneer: " + auction.getAFA().getPlayerName(auction.getAuctionOwnerUUID()) + "\n" + auction.getItemLore())));
        return auctionTextComponent;
    }
}