package io.github.mosadie.awayfromauction.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import io.github.mosadie.awayfromauction.AwayFromAuction;
import io.github.mosadie.awayfromauction.AwayFromAuction.BazaarProduct;
import io.github.mosadie.awayfromauction.util.Auction.Bid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.item.ItemEditableBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

public class AfAUtils {
    public static String addHyphens(String uuid) {
        return uuid.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"); // Shamelessly stolen
                                                                                               // from StackOverflow
    }

    public static String removeHyphens(UUID uuid) {
        return removeHyphens(uuid.toString());
    }

    public static String removeHyphens(String uuid) {
        return uuid.replace("-", "");
    }

    public static EnumChatFormatting getColorFromTier(String tier) {
        switch (tier) {
            case "COMMON":
                return EnumChatFormatting.GRAY;

            case "UNCOMMON":
                return EnumChatFormatting.GREEN;

            case "RARE":
                return EnumChatFormatting.BLUE;

            case "EPIC":
                return EnumChatFormatting.DARK_PURPLE;

            case "LEGENDARY":
                return EnumChatFormatting.GOLD;

            case "MYTHIC":
                return EnumChatFormatting.LIGHT_PURPLE;

            case "SPECIAL":
                return EnumChatFormatting.RED;

            case "VERY SPECIAL":
                return EnumChatFormatting.RED;

            default:
                AwayFromAuction.getLogger().warn("Unknown tier type! " + tier);
                return EnumChatFormatting.GRAY;
        }
    }

    public static boolean bidsContainUUID(Bid[] bids, UUID uuid) {
        if (bids == null || bids.length == 0) {
            if (bids == null)
                AwayFromAuction.getLogger().warn("Null bids!");
            return false;
        }

        for (Bid bid : bids) {
            if (bid.getBidderUUID().equals(uuid)) {
                AwayFromAuction.getLogger().debug("BID FOUND AUCTION " + bid.getAuctionUUID().toString());
                return true;
            }
        }

        return false;
    }

    public static String formatCoins(long coins) {
        return String.format("%,d", coins);
    }

    public static ItemStack convertBookInfoToBook(IBookInfo bookInfo) {

        ItemStack bookStack = new ItemStack(new ItemEditableBook());
        bookStack.setTagCompound(new NBTTagCompound());
        bookStack.getTagCompound().setString("title", "AwayFromAuction");
        bookStack.getTagCompound().setString("author", "MoSadie");

        AwayFromAuction.getLogger().info("[AfA] Begin page creation. Total Pages: " + bookInfo.getTotalPages());
        NBTTagList pages = bookStack.getTagCompound().getTagList("pages", 8);
        for (int i = 0; i < bookInfo.getTotalPages(); i++) {
            pages.appendTag(new NBTTagString(IChatComponent.Serializer.componentToJson(bookInfo.getPageContent(i))));
        }
        AwayFromAuction.getLogger().info("[AfA] Finished creating pages");

        bookStack.getTagCompound().setTag("pages", pages);

        return bookStack;
    }

    public static void displayBook(ItemStack book) {
        if (book == null) {
            return;
        }
        Minecraft.getMinecraft().addScheduledTask(() -> {
            GuiScreenBook bookScreen = new GuiScreenBook(Minecraft.getMinecraft().thePlayer, book, false);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Minecraft.getMinecraft().displayGuiScreen(bookScreen);
                }
            }, 50);
        });
    }

    /**
     * @return True if connected to the Hypixel Minecraft server, false otherwise.
     */
    public static boolean onHypixel() {
        try {
            return Minecraft.getMinecraft().getCurrentServerData().serverIP.contains(".hypixel.net");
        } catch (Exception e) {
            return false;
        }
    }

    public static IChatComponent createHypixelLink() {
        if (onHypixel())
            return new ChatComponentText("");

        ChatComponentText hypixelLink = new ChatComponentText("CLICK HERE");
        hypixelLink.getChatStyle().setUnderlined(true).setBold(true)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa joinhypixel"))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText("Click to join the Hypixel server!")));
        ChatComponentText ending = new ChatComponentText(" to join the Hypixel server!");
        hypixelLink.appendSibling(ending);
        return hypixelLink;
    }

    public static IChatComponent createBazaarHistoryLink(BazaarProduct product) {
        ChatComponentText bazaarLink = new ChatComponentText("Click to view history");
        bazaarLink.getChatStyle().setUnderlined(true)
                .setChatClickEvent(
                        new ClickEvent(ClickEvent.Action.OPEN_URL, "https://stonks.gg/product/" + product.getId()))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText("Click to view the history of the item on stonks.gg")));

        return bazaarLink;
    }
}