package io.github.mosadie.awayfromauction.core.book;

import io.github.mosadie.awayfromauction.core.IAwayFromAuction;
import io.github.mosadie.awayfromauction.core.text.ClickEvent;
import io.github.mosadie.awayfromauction.core.text.ITextComponent;
import io.github.mosadie.awayfromauction.core.text.StringComponent;

public class AuctionSelectItemBookInfo implements IBookInfo {

    private final String[] items;
    private final IAwayFromAuction afa;

    private static final int RESULTS_PER_PAGE = 3;

    /**
     * @return The number of pages in the book.
     */
    @Override
    public int getTotalPages() { // Number of pages
        return items.length / RESULTS_PER_PAGE + 1;
    }

    /**
     * Gets the content for a specified page.
     * 
     * @param page The page (zero indexed) to get content for.
     * @return An ITextComponent with content for the specified page.
     */
    @Override
    public ITextComponent getPageContent(int page) { // Get text on page
        StringComponent root = new StringComponent("Click an item to search for:\n\n");
        for (int i = 0; i < RESULTS_PER_PAGE; i++) {
            root.appendSibling(getItemLineOrBlank(page * RESULTS_PER_PAGE + i));
            root.appendText("\n\n");
        }
        return root;
    }

    public AuctionSelectItemBookInfo(String[] items, IAwayFromAuction afa) {
        this.items = items;
        this.afa = afa;
    }

    /**
     * Gets the item name or a blank text component.
     * 
     * @param index The index of the item in the items array.
     * @return ITextComponent that has the item name or a blank string.
     */
    private ITextComponent getItemLineOrBlank(int index) {
        if (index >= items.length)
            return new StringComponent("");
        String item = items[index];
        if (afa.getAuctionsByItem(item).length > 0) {
            item = afa.getAuctionsByItem(item)[0].getItemName();
        }
        return new StringComponent(item).setUnderlined(true).setBold(true)
                .setClickEvent(new ClickEvent(ClickEvent.ClickAction.RUN_COMMAND, "/afa search " + item));
    }
}