package io.github.mosadie.awayfromauction.core;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AuctionJsonDeserializer implements JsonDeserializer<Auction[]> {

    private final IAwayFromAuction afa;

    public AuctionJsonDeserializer(IAwayFromAuction afa) {
        this.afa = afa;
    }

    @Override
    public Auction[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        List<Auction> array = new ArrayList<>();

        if (!json.isJsonArray()) {
            throw new JsonParseException("Not an array!");
        }

        for (JsonElement jsonElement : (JsonArray) json) {
            if (!jsonElement.isJsonObject()) {
                throw new JsonParseException("Auction object not found!");
            }

            Auction auction = new Auction(jsonElement.getAsJsonObject(), afa);
            array.add(auction);
        }

        return array.toArray(new Auction[0]);
    }

}