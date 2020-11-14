package io.github.mosadie.awayfromauction.core;

import java.lang.reflect.Type;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import io.github.mosadie.awayfromauction.core.Auction.Bid;

public class AuctionJsonSerializer implements JsonSerializer<Auction[]> {

    @Override
    public JsonElement serialize(Auction[] src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray array = new JsonArray();

        for (Auction auction : src) {
            JsonObject auctionJson = new JsonObject();
            auctionJson.addProperty("uuid", AfAUtils.removeHyphens(auction.getAuctionUUID().toString()));
            auctionJson.addProperty("auctioneer", AfAUtils.removeHyphens(auction.getAuctionOwnerUUID().toString()));
            auctionJson.addProperty("profile_id",
                    AfAUtils.removeHyphens(auction.getAuctionOwnerProfileUUID().toString()));
            JsonArray coop = new JsonArray();
            for (UUID coopMember : auction.getCoop()) {
                coop.add(new JsonPrimitive(AfAUtils.removeHyphens(coopMember.toString())));
            }
            auctionJson.add("coop", coop);
            auctionJson.addProperty("start", auction.getStart().getTime());
            auctionJson.addProperty("end", auction.getEnd().getTime());
            auctionJson.addProperty("syncTimestamp", auction.getSyncTimestamp().getTime());
            auctionJson.addProperty("item_name", auction.getItemName());
            auctionJson.addProperty("item_lore", auction.getItemLore());
            auctionJson.addProperty("extra", auction.getExtra());
            auctionJson.addProperty("category", auction.getCategory());
            auctionJson.addProperty("tier", auction.getTier());
            auctionJson.addProperty("starting_bid", auction.getStartingBid());

            auctionJson.addProperty("item_count", auction.getItemCount());

            auctionJson.addProperty("claimed", auction.getClaimed());
            JsonArray claimedBidders = new JsonArray();
            for (UUID bidder : auction.getClaimedBidders()) {
                claimedBidders.add(new JsonPrimitive(bidder.toString()));
            }
            auctionJson.add("claimed_bidders", claimedBidders);
            auctionJson.addProperty("highest_bid_amount", auction.getHighestBidAmount());
            JsonArray bids = new JsonArray();
            for (Bid bid : auction.getBids()) {
                JsonObject bidJson = new JsonObject();
                bidJson.addProperty("auction_id", AfAUtils.removeHyphens(bid.getAuctionUUID().toString()));
                bidJson.addProperty("bidder", AfAUtils.removeHyphens(bid.getBidderUUID().toString()));
                bidJson.addProperty("amount", bid.getAmount());
                bidJson.addProperty("timestamp", bid.getTimestamp().getTime());
                bids.add(bidJson);
            }
            auctionJson.add("bids", bids);

            auctionJson.addProperty("bin", auction.isBIN());

            array.add(auctionJson);
        }

        return array;
    }

}