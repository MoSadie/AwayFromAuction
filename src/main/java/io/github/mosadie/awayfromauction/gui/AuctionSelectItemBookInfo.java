package io.github.mosadie.awayfromauction.gui;

import io.github.mosadie.awayfromauction.AwayFromAuction;
import io.github.mosadie.awayfromauction.util.IBookInfo;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public class AuctionSelectItemBookInfo implements IBookInfo {

    private final String[] items;
    private final AwayFromAuction afa;

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
     * @return An IChatComponent with content for the specified page.
     */
    @Override
    public IChatComponent getPageContent(int page) { // Get text on page
        ChatComponentText root = new ChatComponentText("Click an item to search for:\n\n");
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
     * 
     * @param index The index of the item in the items array.
     * @return IChatComponent that has the item name or a blank string.
     */
    private IChatComponent getItemLineOrBlank(int index) {
        if (index >= items.length)
            return new ChatComponentText("");
        String item = items[index];
        if (afa.getAuctionsByItem(item).length > 0) {
            item = afa.getAuctionsByItem(item)[0].getItemName();
        }
        ChatComponentText itemTextComponent = new ChatComponentText(item);
        itemTextComponent.getChatStyle().setUnderlined(true).setBold(true)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa search " + item));
        return itemTextComponent;
    }
}