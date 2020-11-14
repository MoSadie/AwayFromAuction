package io.github.mosadie.awayfromauction.core;

public class ConfigState {
    private final int refreshDelay;
    private final boolean alwaysNotify;
    private final String hypixelApiKey;

    public ConfigState(int refreshDelay, boolean alwaysNotify, String hypixelApiKey) {
        this.refreshDelay = refreshDelay;
        this.alwaysNotify = alwaysNotify;
        this.hypixelApiKey = hypixelApiKey;
    }
    public int getGeneralRefreshDelay() { return refreshDelay; }
    public boolean getGeneralAlwaysNotify() { return alwaysNotify; }
    public String getHypixelAPIKey() { return hypixelApiKey; }
}