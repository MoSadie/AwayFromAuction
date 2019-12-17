package io.github.mosadie.awayfromauction.gui;

import io.github.mosadie.awayfromauction.AwayFromAuction;
import net.minecraft.client.gui.screen.ReadBookScreen.IBookInfo;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.ClickEvent;

public class AuctionSelectItemBookInfo implements IBookInfo {

    private final String[] items;
    private final AwayFromAuction afa;

    private static final int RESULTS_PER_PAGE = 3;

    /**
     * @return The number of pages in the book.
     */
    @Override
    public int func_216918_a() { // Number of pages
        return items.length / RESULTS_PER_PAGE + 1;
    }

    /**
     * Gets the content for a specified page.
     * @param page The page (zero indexed) to get content for.
     * @return An ITextComponent with content for the specified page.
     */
    @Override
    public ITextComponent func_216915_a(int page) { // Get text on page
        ITextComponent root = new StringTextComponent("Click an item to search for:\n\n");
        for (int i = 0; i < RESULTS_PER_PAGE; i++) {
            root.appendSibling(getItemLineOrBlank(page * RESULTS_PER_PAGE + i));
            root.appendText("\n\n");
        }
        return root;
    }

    public AuctionSelectItemBookInfo(String[] items, AwayFromAuction afa) {
        this.items = items;
        this.afa = afa;
    }

    /**
     * Gets the item name or a blank text component.
     * @param index The index of the item in the items array.
     * @return ITextComponent that has the item name or a blank string.
     */
    private ITextComponent getItemLineOrBlank(int index) {
        if (index >= items.length) return new StringTextComponent("");
        String item = items[index];
        if (afa.getAuctionsByItem(item).length > 0) {
            item = afa.getAuctionsByItem(item)[0].getItemName();
        }
        StringTextComponent itemTextComponent = new StringTextComponent(item);
        itemTextComponent.getStyle()
                                .setUnderlined(true)
                                .setBold(true)
                                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa search " + item));
        return itemTextComponent;
    }
}