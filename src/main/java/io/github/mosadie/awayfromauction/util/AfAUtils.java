package io.github.mosadie.awayfromauction.util;

import java.util.UUID;

import io.github.mosadie.awayfromauction.AwayFromAuction;
import io.github.mosadie.awayfromauction.util.Auction.Bid;
import net.minecraft.util.datafix.fixes.BlockStateFlatteningMap;
import net.minecraft.util.datafix.fixes.ItemIntIDToString;
import net.minecraft.util.text.TextFormatting;

public class AfAUtils {
    public static String addHyphens(String uuid) {
        return uuid.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"); // Shamelessly stolen from StackOverflow  
    }
    
    public static String removeHyphens(UUID uuid) {
        return removeHyphens(uuid.toString());
    }
    
    public static String removeHyphens(String uuid) {
        return uuid.replace("-","");
    }
    
    public static TextFormatting getColorFromTier(String tier) {
        switch(tier) {
            case "COMMON":
            return TextFormatting.GRAY;
            
            case "UNCOMMON":
            return TextFormatting.GREEN;
            
            case "RARE":
            return TextFormatting.DARK_BLUE;
            
            case "EPIC":
            return TextFormatting.DARK_PURPLE;
            
            case "LEGENDARY":
            return TextFormatting.GOLD;
            
            case "SPECIAL":
            return TextFormatting.LIGHT_PURPLE;
            
            default:
            AwayFromAuction.getLogger().warn("Unknown tier type! " + tier);
            return TextFormatting.GRAY;
        }
    }
    
    public static boolean bidsContainUUID(Bid[] bids, UUID uuid) {
        if (bids == null || bids.length == 0) {
            if (bids == null) AwayFromAuction.getLogger().warn("Null bids!");
            return false;
        }
        
        for (Bid bid : bids) {
            if (bid.getBidderUUID().equals(uuid)){
                AwayFromAuction.getLogger().debug("BID FOUND AUCTION " + bid.getAuctionUUID().toString());
                return true;
            }
        }
        
        return false;
    }
    
    public static String convertIDtoRegName(int itemId, String itemName) {
        switch(itemId) {
            case 409:
            return "minecraft:prismarine_shard";
            
            case 410:
            return "minecraft:prismarine_crystals";
            
            case 411:
            return "minecraft:rabbit";
            
            case 414:
            return "minecraft:rabbit_foot";
            
            case 415:
            return "minecraft:rabbit_hide";
            
            case 423:
            return "minecraft:mutton";
            
            default:
            String regItemName = ItemIntIDToString.getItem(itemId);
            String regBlockName = BlockStateFlatteningMap.updateId(itemId);
            if (regItemName.equals("minecraft:air") && regBlockName.equals("minecraft:air")) {
                AwayFromAuction.getLogger().warn("Unknown Item Received! ID: " + itemId + " with Item Name: " + itemName + ". Setting to stone to (hopefully) peserve NBT!");
                return "minecraft:stone";
            } else if (regItemName.equals("minecraft:air")) {
                return regBlockName;
            } else {
                return regItemName;
            }
        }
    }

    public static String formatCoins(long coins) {
        return String.format("%,d", coins);
    }
}