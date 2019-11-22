package io.github.mosadie.awayfromauction.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import com.google.gson.JsonObject;

import org.apache.commons.codec.binary.Base64InputStream;

import io.github.mosadie.awayfromauction.AwayFromAuction;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;

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
    private final String extra;
    private final String category;
    private final String tier;
    private final int startingBid;
    private final ItemStack itemStack;
    private final boolean claimed;
    private final UUID[] claimedBidders; // I have no idea what this means, other than it's player UUIDs.
    private final int highestBidAmount;
    private final Bid[] bids;

    private final AwayFromAuction awa;

    public Auction(JsonObject auctionData, AwayFromAuction awa) {
        this.awa = awa;
        auctionUUID = UUID.fromString(addHyphens(auctionData.get("uuid").getAsString()));
        ownerUUID = UUID.fromString(addHyphens(auctionData.get("auctioneer").getAsString()));
        ownerProfileUUID = UUID.fromString(addHyphens(auctionData.get("profile_id").getAsString()));

        int coopSize = auctionData.get("coop").getAsJsonArray().size();
        coop = new UUID[auctionData.get("coop").getAsJsonArray().size()];
        for(int i = 0; i < coopSize; i++) {
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

        CompoundNBT nbt = null;

        try {
            String bytes;
            if (auctionData.get("item_bytes").isJsonObject()) {
                bytes = auctionData.get("item_bytes").getAsJsonObject().get("data").getAsString();
            } else {
                bytes = auctionData.get("item_bytes").getAsString();
            }
            Base64InputStream is = new Base64InputStream(new ByteArrayInputStream(bytes.getBytes(StandardCharsets.UTF_8)));
            nbt = CompressedStreamTools.readCompressed(is);
        } catch (IOException ioException) {
            AwayFromAuction.getLogger().error("Exception occured creating ItemStack from item_bytes: " + ioException.getLocalizedMessage());
            AwayFromAuction.getLogger().catching(ioException);
        }

        if (nbt == null) {
            itemStack = ItemStack.EMPTY;
        } else {
            itemStack = ItemStack.read(nbt);
        }

        claimed = auctionData.get("claimed").getAsBoolean();
        
        int claimedBidderSize = auctionData.get("claimed_bidders").getAsJsonArray().size();
        claimedBidders = new UUID[claimedBidderSize];
        for (int i = 0; i < claimedBidderSize; i++) {
            claimedBidders[i] = UUID.fromString(addHyphens(auctionData.get("claimed_bidders").getAsJsonArray().get(i).getAsString()));
        }

        highestBidAmount = auctionData.get("highest_bid_amount").getAsInt();
        
        int bidsSize = auctionData.get("bids").getAsJsonArray().size();
        bids = new Bid[bidsSize];
        for (int i = 0; i < bidsSize; i++) {
            bids[i] = new Bid(auctionData.get("bids").getAsJsonArray().get(i).getAsJsonObject());
        }
    }

    private Auction() {
        auctionUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        ownerUUID = Minecraft.getInstance().player.getUniqueID();
        ownerProfileUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        coop = new UUID[] {Minecraft.getInstance().player.getUniqueID()};
        start = new Date();
        end = new Date();
        syncTimestamp = new Date();
        itemName = "Error";
        itemLore = "Error";
        extra = "Error";
        category = "Error";
        tier = "Error";
        startingBid = 0;
        itemStack = ItemStack.EMPTY;
        claimed = false;
        claimedBidders =new UUID[] {Minecraft.getInstance().player.getUniqueID()};
        highestBidAmount = 0;
        bids = new Bid[] {new Bid()};
        awa = null;
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

    public ItemStack getItemStack() {
        return itemStack;
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

        return bids[bids.length-1];
    }

    public int getHighestBidAmount() {
        return highestBidAmount;
    }

    public Bid[] getBids() {
        return bids;
    }

    public AwayFromAuction getAWA() {
        return awa;
    }

    public class Bid {
        private final UUID auctionUUID;
        private final UUID bidderUUID;
        private final UUID profileUUID;
        private final int amount;
        private final Date timestamp;
    
        public Bid(JsonObject bidData) {
            auctionUUID = UUID.fromString(addHyphens(bidData.get("auction_id").getAsString()));
            bidderUUID = UUID.fromString(addHyphens(bidData.get("bidder").getAsString()));
            profileUUID = UUID.fromString(addHyphens(bidData.get("profile_id").getAsString()));
            amount = bidData.get("amount").getAsInt();
            timestamp = new Date(bidData.get("timestamp").getAsLong());
        }

        private Bid() {
            auctionUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
            bidderUUID = Minecraft.getInstance().player.getUniqueID();
            profileUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
            amount = 0;
            timestamp = new Date();
        }

        public UUID getAuctionUUID() {
            return auctionUUID;
        }

        public UUID getBidderUUID() {
            return bidderUUID;
        }

        public UUID getProfileUUID() {
            return profileUUID;
        }

        public int getAmount() {
            return amount;
        }

        public Date getTimestamp() {
            return timestamp;
        }
    }

    private static String addHyphens(String uuid) {
        return uuid.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"); // Shamelessly stolen from StackOverflow  
    }
}

