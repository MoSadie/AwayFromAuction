package io.github.mosadie.awayfromauction.core.book;

import io.github.mosadie.awayfromauction.core.AfAUtils;
import io.github.mosadie.awayfromauction.core.Auction;
import io.github.mosadie.awayfromauction.core.text.ClickEvent;
import io.github.mosadie.awayfromauction.core.text.HoverEvent;
import io.github.mosadie.awayfromauction.core.text.ITextComponent;
import io.github.mosadie.awayfromauction.core.text.StringComponent;

import java.util.ArrayList;
import java.util.List;

public class AuctionSearchBookInfo implements IBookInfo {

    private final String query;
    private final List<Auction> auctions;

    private static final int RESULTS_PER_PAGE = 3;

    /**
     * @return The number of pages in the book.
     */
    @Override
    public int getTotalPages() {
        return auctions.size() / RESULTS_PER_PAGE + 1;
    }

    /**
     * Get the content of a specified page.
     * 
     * @param page The page (zero indexed) to get content for.
     * @return An ITextComponent with content for the specified page
     */
    @Override
    public ITextComponent getPageContent(int page) {
        ITextComponent root = new StringComponent("Search Results for: " + query + "\n\n");
        for (int i = 0; i < RESULTS_PER_PAGE; i++) {
            root.appendSibling(getAuctionLineOrBlank(page * RESULTS_PER_PAGE + i));
            root.appendText("\n\n");
        }
        return root;
    }

    public AuctionSearchBookInfo(Auction[] auctions, String query) {
        this.query = query;
        this.auctions = new ArrayList<>();
        for (Auction auction : auctions) {
            if (!auction.getEnd().before(auction.getSyncTimestamp()))
                this.auctions.add(auction);
        }
    }

    /**
     * Gets the one-line description for an auction or returns a blank line if the
     * auction is not found.
     * 
     * @param index The index of the auction in the auctions array.
     * @return ITextComponent that has the one-line description for the auction or a
     *         blank string.
     */
    private ITextComponent getAuctionLineOrBlank(int index) {
        if (index >= auctions.size())
            return new StringComponent("");
        Auction auction = auctions.get(index);
        String ownerUsername = auction.getAFA().getCore().getPlayerName(auction.getAuctionOwnerUUID());
        if (ownerUsername == null) { ownerUsername = auction.getAuctionOwnerUUID().toString(); }
        StringComponent auctionTextComponent = new StringComponent(
                auction.getItemName() + (auction.getItemCount() > 0 ? " x" + auction.getItemCount() : "") + ": "
                        + (auction.getHighestBidAmount() > 0 ? auction.getHighestBidAmount() : auction.getStartingBid())
                        + " coins");
        auctionTextComponent.setColor(AfAUtils.getColorFromTier(auction.getTier()))
                .setClickEvent(new ClickEvent(ClickEvent.ClickAction.RUN_COMMAND,
                        "/afa view " + auction.getAuctionUUID().toString()))
                .setHoverEvent(
                        new HoverEvent(HoverEvent.HoverAction.SHOW_TEXT,
                                new StringComponent("Auction Owner: "
                                        + ownerUsername + "\n"
                                        + auction.getItemLore())));
        return auctionTextComponent;
    }
}