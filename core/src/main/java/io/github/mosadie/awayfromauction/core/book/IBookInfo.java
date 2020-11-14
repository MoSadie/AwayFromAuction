package io.github.mosadie.awayfromauction.core.book;

import io.github.mosadie.awayfromauction.core.text.ITextComponent;

public interface IBookInfo {
    /**
     * @return The number of pages in the book.
     */
    int getTotalPages();

    /**
     * Get the content of a specified page.
     * 
     * @param page The page (zero indexed) to get content for.
     * @return An ITextComponent with content for the specified page
     */
    ITextComponent getPageContent(int page);
}