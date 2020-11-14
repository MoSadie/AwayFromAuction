package io.github.mosadie.awayfromauction.core;

public class SkyblockItem {
    public String getName() {
        return name;
    }

    public String getTier() {
        return tier;
    }

    public String getCategory() {
        return category;
    }

    public int getItemId() {
        return item_id;
    }

    public boolean isBazaar() {
        return bazaar;
    }

    private String name;
    private String tier;
    private String category;
    private int item_id;
    private final boolean bazaar = false; //TODO test if need to define default
}
