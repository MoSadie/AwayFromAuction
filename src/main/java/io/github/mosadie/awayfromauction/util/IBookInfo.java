package io.github.mosadie.awayfromauction.util;

import net.minecraft.util.IChatComponent;

public interface IBookInfo {
    /**
    * @return The number of pages in the book.
    */
    public int getTotalPages();

    /**
    * Get the content of a specified page.
    * @param page The page (zero indexed) to get content for.
    * @return An IChatComponent with content for the specified page
    */
    public IChatComponent getPageContent(int page);
}