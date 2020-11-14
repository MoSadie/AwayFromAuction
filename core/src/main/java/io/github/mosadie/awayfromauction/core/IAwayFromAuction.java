package io.github.mosadie.awayfromauction.core;

import org.apache.commons.codec.binary.Base64InputStream;

import java.io.IOException;
import java.util.UUID;

public interface IAwayFromAuction {
    void logDebug(String msg);
    void logInfo(String msg);
    void logWarn(String msg);
    void logError(String msg);
    void logException(Exception exception);
    boolean isMinecraftNull();
    boolean onHypixel();
    ConfigState getConfigState();
    UUID getCurrentPlayerUUID();
    void createNewBidEvent(Auction auction);
    void createOutbidEvent(Auction auction);
    void createEndingSoonEvent(Auction auction);
	Auction[] getAuctionsByItem(String item);

	AwayFromAuctionCore getCore();

    int getItemCountFromNBTStream(Base64InputStream inputStream) throws IOException;

    void setHypixelAPIKey(String apiKey);
}