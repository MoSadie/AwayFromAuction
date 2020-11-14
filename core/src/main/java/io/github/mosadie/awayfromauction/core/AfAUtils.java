package io.github.mosadie.awayfromauction.core;

import io.github.mosadie.awayfromauction.core.Auction.Bid;
import io.github.mosadie.awayfromauction.core.text.ClickEvent;
import io.github.mosadie.awayfromauction.core.text.HoverEvent;
import io.github.mosadie.awayfromauction.core.text.ITextComponent;
import io.github.mosadie.awayfromauction.core.text.StringComponent;
import net.hypixel.api.reply.skyblock.BazaarReply;

import java.util.UUID;

public class AfAUtils {
    public static String addHyphens(String uuid) {
        return uuid.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"); // Shamelessly stolen
                                                                                               // from StackOverflow
    }

    public static String removeHyphens(UUID uuid) {
        return removeHyphens(uuid.toString());
    }

    public static String removeHyphens(String string) {
        return string.replace("-", "");
    }

    public static ColorEnum getColorFromTier(String tier) {
        switch (tier) {
            case "COMMON":
                return ColorEnum.GRAY;

            case "UNCOMMON":
                return ColorEnum.GREEN;

            case "RARE":
                return ColorEnum.BLUE;

            case "EPIC":
                return ColorEnum.DARK_PURPLE;

            case "LEGENDARY":
                return ColorEnum.GOLD;

            case "MYTHIC":
                return ColorEnum.LIGHT_PURPLE;

            case "SPECIAL":
                return ColorEnum.RED;

            case "VERY SPECIAL":
                return ColorEnum.RED;

            default:
                // AwayFromAuction.getLogger().warn("Unknown tier type! " + tier);
                return ColorEnum.GRAY;
        }
    }

    public static boolean bidsContainUUID(Bid[] bids, UUID uuid) {
        if (bids == null || bids.length == 0) {
            if (bids == null)
                // AwayFromAuction.getLogger().warn("Null bids!");
            return false;
        }

        for (Bid bid : bids) {
            if (bid.getBidderUUID().equals(uuid)) {
                // AwayFromAuction.getLogger().debug("BID FOUND AUCTION " + bid.getAuctionUUID().toString());
                return true;
            }
        }

        return false;
    }

    public static String formatCoins(long coins) {
        return String.format("%,d", coins);
    }

    public static ITextComponent createHypixelLink(IAwayFromAuction afa) {
        if (afa.onHypixel())
            return new StringComponent("");

        ITextComponent hypixelLink = new StringComponent("CLICK HERE")
                                        .setUnderlined(true).setBold(true)
                .setClickEvent(new ClickEvent(ClickEvent.ClickAction.RUN_COMMAND, "/afa joinhypixel"))
                .setHoverEvent(new HoverEvent(HoverEvent.HoverAction.SHOW_TEXT,
                        new StringComponent("Click to join the Hypixel server!")));
        StringComponent ending = new StringComponent(" to join the Hypixel server!");
        hypixelLink.appendSibling(ending);
        return hypixelLink;
    }

    public static ITextComponent createBazaarHistoryLink(BazaarReply.Product product) {

        return new StringComponent("Click to view history")
                .setUnderlined(true)
                .setColor(ColorEnum.WHITE)
                .setClickEvent(
                        new ClickEvent(ClickEvent.ClickAction.OPEN_URL, "https://stonks.gg/product/" + product.getProductId()))
                .setHoverEvent(new HoverEvent(HoverEvent.HoverAction.SHOW_TEXT,
                        new StringComponent("Click to view the history of the item on stonks.gg")));
    }

    public enum ColorEnum {
        BLACK('0'),
        DARK_BLUE('1'),
        DARK_GREEN('2'),
        DARK_AQUA('3'),
        DARK_RED('4'),
        DARK_PURPLE('5'),
        GOLD('6'),
        GRAY('7'),
        DARK_GRAY('8'),
        BLUE ('9'),
        GREEN('a'),
        AQUA('b'),
        RED('c'),
        LIGHT_PURPLE('d'),
        YELLOW('e'),
        WHITE('f');

        private final char character;

        ColorEnum(char character) {
            this.character = character;
        }

        public String getFullPrefix() {
            return "§" + character;
        }

        public char getChar() {
            return character;
        }
    }
}