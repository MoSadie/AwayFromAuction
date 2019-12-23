package io.github.mosadie.awayfromauction.gui;

import java.util.ArrayList;
import java.util.List;

import io.github.mosadie.awayfromauction.util.AfAUtils;
import io.github.mosadie.awayfromauction.util.Auction;
import io.github.mosadie.awayfromauction.util.IBookInfo;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public class AuctionSearchBookInfo implements IBookInfo {

    private final String query;
    private final Auction[] auctions;

    private static final int RESULTS_PER_PAGE = 3;

    /**
     * @return The number of pages in the book.
     */
    @Override
    public int getTotalPages() {
        return auctions.length / RESULTS_PER_PAGE + 1;
    }

    /**
     * Get the content of a specified page.
     * 
     * @param page The page (zero indexed) to get content for.
     * @return An IChatComponent with content for the specified page
     */
    @Override
    public IChatComponent getPageContent(int page) {
        IChatComponent root = new ChatComponentText("Search Results for: " + query + "\n\n");
        for (int i = 0; i < RESULTS_PER_PAGE; i++) {
            root.appendSibling(getAuctionLineOrBlank(page * RESULTS_PER_PAGE + i));
            root.appendText("\n\n");
        }
        return root;
    }

    public AuctionSearchBookInfo(Auction[] auctions, String query) {
        this.query = query;
        List<Auction> activeAuctions = new ArrayList<>();
        for (Auction auction : auctions) {
            if (!auction.getEnd().before(auction.getSyncTimestamp()))
                activeAuctions.add(auction);
        }
        this.auctions = activeAuctions.toArray(new Auction[0]);
    }

    /**
     * Gets the one-line description for an auction or returns a blank line if the
     * auction is not found.
     * 
     * @param index The index of the auction in the auctions array.
     * @return IChatComponent that has the one-line description for the auction or a
     *         blank string.
     */
    private IChatComponent getAuctionLineOrBlank(int index) {
        if (index >= auctions.length)
            return new ChatComponentText("");
        Auction auction = auctions[index];
        ChatComponentText auctionTextComponent = new ChatComponentText(
                auction.getItemName() + (auction.getItemCount() > 0 ? " x" + auction.getItemCount() : "") + ": "
                        + (auction.getHighestBidAmount() > 0 ? auction.getHighestBidAmount() : auction.getStartingBid())
                        + " coins");
        auctionTextComponent.getChatStyle().setColor(AfAUtils.getColorFromTier(auction.getTier()))
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/afa view " + auction.getAuctionUUID().toString()))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText(
                                "Auctioneer: " + auction.getAFA().getPlayerName(auction.getAuctionOwnerUUID()) + "\n"
                                        + auction.getItemLore())));
        return auctionTextComponent;
    }
}