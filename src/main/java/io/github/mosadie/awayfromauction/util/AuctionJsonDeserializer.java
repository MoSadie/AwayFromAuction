package io.github.mosadie.awayfromauction.util;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import io.github.mosadie.awayfromauction.AwayFromAuction;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class AuctionJsonDeserializer implements JsonDeserializer<Auction[]> {

    @Override
    public Auction[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        List<Auction> array = new ArrayList<>();

        AwayFromAuction afa = (AwayFromAuction) FMLCommonHandler.instance().findContainerFor(AwayFromAuction.MOD_ID)
                .getMod();

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