package io.github.mosadie.awayfromauction.core.book;

import io.github.mosadie.awayfromauction.core.AfAUtils;
import io.github.mosadie.awayfromauction.core.IAwayFromAuction;
import io.github.mosadie.awayfromauction.core.SkyblockItem;
import io.github.mosadie.awayfromauction.core.text.ITextComponent;
import io.github.mosadie.awayfromauction.core.text.StringComponent;
import net.hypixel.api.reply.skyblock.BazaarReply;

public class BazaarBookInfo implements IBookInfo {

    private final BazaarReply.Product[] products;
    private final IAwayFromAuction afa;

    public BazaarBookInfo(BazaarReply.Product[] products, IAwayFromAuction afa) {
        this.products = products;
        this.afa = afa;
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
     * @return An ITextComponent with the content for the specified page.
     */
    @Override
    public ITextComponent getPageContent(int page) {

        // Page layout:
        // Title
        StringComponent root = new StringComponent("Bazaar Details:\n");

        BazaarReply.Product product = products[page];

        // Item Name
        SkyblockItem item = afa.getCore().getItem(product.getProductId());
        ITextComponent itemTitle = new StringComponent(item == null ? product.getProductId() : item.getName())
                .setUnderlined(true)
                .setColor(AfAUtils.getColorFromTier(item == null ? "COMMON" : item.getTier()));

        // Newline
        StringComponent newLine = new StringComponent("\n\n");

        // Buy and Sell Price
        StringComponent buyPrice = new StringComponent(
                "Buy Price: " + Math.round(100 * product.getQuickStatus().getBuyPrice()) / 100.0 + "\n");
        StringComponent sellPrice = new StringComponent(
                "Sell Price: " + Math.round(100 * product.getQuickStatus().getSellPrice()) / 100.0 + "\n");

        // Newline

        // Buy and Sell Volumes
        StringComponent buyVolume = new StringComponent("Buy Volume: " + product.getQuickStatus().getBuyVolume() + "\n");
        StringComponent sellVolume = new StringComponent("Sell Volume: " + product.getQuickStatus().getSellVolume() + "\n");

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