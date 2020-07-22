package io.github.mosadie.awayfromauction.gui;

import io.github.mosadie.awayfromauction.AwayFromAuction.BazaarProduct;
import io.github.mosadie.awayfromauction.util.AfAUtils;
import io.github.mosadie.awayfromauction.util.IBookInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public class BazaarBookInfo implements IBookInfo {

    private final BazaarProduct[] products;

    public BazaarBookInfo(BazaarProduct[] products) {
        this.products = products;
    }

    /**
     * @return The total number of pages in the book.
     */
    @Override
    public int getTotalPages() {
        return products.length;
    }

    /**
     * Gets the content of a specified page.
     * 
     * @param page The page (zero indexed) to get content for.
     * @return An IChatComponent with the content for the specified page.
     */
    @Override
    public IChatComponent getPageContent(int page) {

        // Page layout:
        // Title
        ChatComponentText root = new ChatComponentText("Bazaar Details:\n");

        BazaarProduct product = products[page];
        // Item Name
        ChatComponentText itemTitle = new ChatComponentText(product.getName());
        itemTitle.getChatStyle().setUnderlined(true);

        // Newline
        ChatComponentText newLine = new ChatComponentText("\n\n");

        // Buy and Sell Price
        ChatComponentText buyPrice = new ChatComponentText(
                "Buy Price: " + Math.round(100 * product.getBuyPrice()) / 100.0 + "\n");
        ChatComponentText sellPrice = new ChatComponentText(
                "Sell Price: " + Math.round(100 * product.getSellPrice()) / 100.0 + "\n");

        // Newline

        // Buy and Sell Volumes
        ChatComponentText buyVolume = new ChatComponentText("Buy Volume: " + product.getBuyVolume() + "\n");
        ChatComponentText sellVolume = new ChatComponentText("Sell Volume: " + product.getSellVolume() + "\n");

        // Newline

        // Link to history site (created by AfAUtils when appending everything together)

        // Append everything together.

        root.appendSibling(itemTitle);
        root.appendSibling(newLine);
        root.appendSibling(buyPrice);
        root.appendSibling(sellPrice);
        root.appendSibling(newLine);
        root.appendSibling(buyVolume);
        root.appendSibling(sellVolume);
        root.appendSibling(newLine);
        root.appendSibling(AfAUtils.createBazaarHistoryLink(product));

        return root;
    }

}