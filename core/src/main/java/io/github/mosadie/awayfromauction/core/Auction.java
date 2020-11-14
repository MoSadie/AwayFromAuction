package io.github.mosadie.awayfromauction.core;

import com.google.gson.JsonObject;
import org.apache.commons.codec.binary.Base64InputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

public class Auction {

    public static final Auction ERROR_AUCTION = new Auction();

    private final UUID auctionUUID;
    private final UUID ownerUUID;
    private final UUID ownerProfileUUID;
    private final UUID[] coop;
    private final Date start;
    private final Date end;
    private final Date syncTimestamp;
    private final String itemName;
    private final String itemLore;
    private final int itemCount;
    private final String extra;
    private final String category;
    private final String tier;
    private final int startingBid;
    private final boolean claimed;
    private final UUID[] claimedBidders; // I have no idea what this means, other than it's player UUIDs.
    private final int highestBidAmount;
    private final Bid[] bids;
    private final boolean bin;

    private final IAwayFromAuction afa;

    public Auction(JsonObject auctionData, IAwayFromAuction afa) {
        this.afa = afa;
        auctionUUID = UUID.fromString(addHyphens(auctionData.get("uuid").getAsString()));
        ownerUUID = UUID.fromString(addHyphens(auctionData.get("auctioneer").getAsString()));
        ownerProfileUUID = UUID.fromString(addHyphens(auctionData.get("profile_id").getAsString()));

        int coopSize = auctionData.get("coop").getAsJsonArray().size();
        coop = new UUID[auctionData.get("coop").getAsJsonArray().size()];
        for (int i = 0; i < coopSize; i++) {
            coop[i] = UUID.fromString(addHyphens(auctionData.get("coop").getAsJsonArray().get(i).getAsString()));
        }

        start = new Date(auctionData.get("start").getAsLong());
        end = new Date(auctionData.get("end").getAsLong());
        syncTimestamp = new Date();

        itemName = auctionData.get("item_name").getAsString();
        itemLore = auctionData.get("item_lore").getAsString();
        extra = auctionData.get("extra").getAsString();
        category = auctionData.get("category").getAsString();
        tier = auctionData.get("tier").getAsString();
        startingBid = auctionData.get("starting_bid").getAsInt();

        if (!auctionData.has("item_count")) {
            int tmpItemCount = 1;
            try {
                String bytes;
                if (auctionData.get("item_bytes").isJsonObject()) {
                    bytes = auctionData.get("item_bytes").getAsJsonObject().get("data").getAsString();
                } else {
                    bytes = auctionData.get("item_bytes").getAsString();
                }
                Base64InputStream is = new Base64InputStream(
                        new ByteArrayInputStream(bytes.getBytes(StandardCharsets.UTF_8)));
                tmpItemCount = afa.getItemCountFromNBTStream(is);

            } catch (IOException ioException) {
                afa.logError(
                        "Exception occurred getting item count from item_bytes, assuming 1: " + ioException.getLocalizedMessage());
                afa.logException(ioException);
            }

            itemCount = tmpItemCount;
        } else {
            itemCount = auctionData.get("item_count").getAsInt();
        }

        claimed = auctionData.get("claimed").getAsBoolean();

        int claimedBidderSize = auctionData.get("claimed_bidders").getAsJsonArray().size();
        claimedBidders = new UUID[claimedBidderSize];
        for (int i = 0; i < claimedBidderSize; i++) {
            claimedBidders[i] = UUID
                    .fromString(addHyphens(auctionData.get("claimed_bidders").getAsJsonArray().get(i).getAsString()));
        }

        highestBidAmount = auctionData.get("highest_bid_amount").getAsInt();

        int bidsSize = auctionData.get("bids").getAsJsonArray().size();
        bids = new Bid[bidsSize];
        for (int i = 0; i < bidsSize; i++) {
            bids[i] = new Bid(auctionData.get("bids").getAsJsonArray().get(i).getAsJsonObject());
        }

        if (auctionData.has("bin")) {
            bin = auctionData.get("bin").getAsBoolean();
        } else {
            bin = false;
        }
    }

    private Auction() {
        auctionUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        ownerUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        ownerProfileUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        coop = new UUID[] { UUID.fromString("00000000-0000-0000-0000-000000000000") };
        start = new Date();
        end = new Date();
        syncTimestamp = new Date();
        itemName = "Error";
        itemLore = "Error";
        itemCount = 0;
        extra = "Error";
        category = "Error";
        tier = "Error";
        startingBid = 0;
        claimed = false;
        claimedBidders = new UUID[] { UUID.fromString("00000000-0000-0000-0000-000000000000") };
        highestBidAmount = 0;
        bids = new Bid[] { new Bid() };
        bin = false;
        afa = null;
    }

    public UUID getAuctionUUID() {
        return auctionUUID;
    }

    public UUID getAuctionOwnerUUID() {
        return ownerUUID;
    }

    public UUID getAuctionOwnerProfileUUID() {
        return ownerProfileUUID;
    }

    public UUID[] getCoop() {
        return coop;
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public Date getSyncTimestamp() {
        return syncTimestamp;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemLore() {
        return itemLore;
    }

    public int getItemCount() {
        return itemCount;
    }

    public String getExtra() {
        return extra;
    }

    public String getCategory() {
        return category;
    }

    public String getTier() {
        return tier;
    }

    public int getStartingBid() {
        return startingBid;
    }

    public boolean getClaimed() {
        return claimed;
    }

    public UUID[] getClaimedBidders() {
        return claimedBidders;
    }

    public Bid getHighestBid() {
        if (bids.length == 0) {
            return null;
        }

        return bids[bids.length - 1];
    }

    public int getHighestBidAmount() {
        return highestBidAmount;
    }

    public Bid[] getBids() {
        return bids;
    }

    public boolean isBIN() {
        return bin;
    }

    public IAwayFromAuction getAFA() {
        return afa;
    }

    public class Bid {
        private final UUID auctionUUID;
        private final UUID bidderUUID;
        private final int amount;
        private final Date timestamp;

        public Bid(JsonObject bidData) {
            auctionUUID = UUID.fromString(addHyphens(bidData.get("auction_id").getAsString()));
            bidderUUID = UUID.fromString(addHyphens(bidData.get("bidder").getAsString()));
            amount = bidData.get("amount").getAsInt();
            timestamp = new Date(bidData.get("timestamp").getAsLong());
        }

        private Bid() {
            auctionUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
            bidderUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
            amount = 0;
            timestamp = new Date();
        }

        public UUID getAuctionUUID() {
            return auctionUUID;
        }

        public UUID getBidderUUID() {
            return bidderUUID;
        }

        public int getAmount() {
            return amount;
        }

        public Date getTimestamp() {
            return timestamp;
        }
    }

    private static String addHyphens(String uuid) {
        return uuid.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"); // Shamelessly stolen
                                                                                               // from StackOverflow
    }
}
