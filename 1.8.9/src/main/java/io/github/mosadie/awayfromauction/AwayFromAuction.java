package io.github.mosadie.awayfromauction;

import io.github.mosadie.awayfromauction.core.*;
import io.github.mosadie.awayfromauction.core.book.IBookInfo;
import io.github.mosadie.awayfromauction.core.text.ClickEvent;
import io.github.mosadie.awayfromauction.core.text.HoverEvent;
import io.github.mosadie.awayfromauction.core.text.ITextComponent;
import io.github.mosadie.awayfromauction.event.AuctionEndingSoonEvent;
import io.github.mosadie.awayfromauction.event.AuctionNewBidEvent;
import io.github.mosadie.awayfromauction.event.AuctionOutbidEvent;
import net.hypixel.api.reply.skyblock.BazaarReply;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.item.ItemEditableBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Mod(modid = AwayFromAuction.MOD_ID, name = "AwayFromAuction", version = "1.1.0", acceptedMinecraftVersions = "1.8.9", clientSideOnly = true, useMetadata = true, updateJSON = "https://raw.githubusercontent.com/MoSadie/AwayFromAuction/master/updateJSON.json")
public class AwayFromAuction implements IAwayFromAuction {
    public static final String MOD_ID = "awayfromauction";

    private static final Logger LOGGER = LogManager.getLogger();

    private AwayFromAuctionCore AfACore;

    public static Configuration config;

    public AwayFromAuction() {

        LOGGER.debug("Map setup complete!");
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.debug("Reading/Setting up config file");
        File directory = event.getModConfigurationDirectory();
        config = new Configuration(new File(directory.getPath(), "awayfromauction.cfg"));
        Config.readConfig();

        File syncAuctionCache = new File(directory.getPath(), "awayfromauction-cache.json");

        LOGGER.debug("Creating AwayFromAuctionCore");
        AfACore = new AwayFromAuctionCore(this, syncAuctionCache);

        LOGGER.debug("Registering Client Command");
        ClientCommandHandler.instance.registerCommand(new AfACommand(this));

        LOGGER.debug("Registering new ClientEventHandler");
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler(this));
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        if (config.hasChanged()) {
            config.save();
        }

        if (!Config.HYPIXEL_API_KEY.equals("")) {
            AfACore.refreshHypixelApi();
        }
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static ChatComponentText getTranslatedTextComponent(String key, Object... args) {
        String translated = StatCollector.translateToLocalFormatted(MOD_ID + "." + key, args);
        ChatComponentText component = new ChatComponentText(translated);
        component.getChatStyle().setColor(EnumChatFormatting.WHITE);
        return component;
    }

    private static IChatComponent createChatPrefix() {
        IChatComponent root = new ChatComponentText("[");
        root.getChatStyle().setColor(EnumChatFormatting.GOLD);

        IChatComponent middle = new ChatComponentText("AfA");
        middle.getChatStyle().setColor(EnumChatFormatting.BLUE);
        root.appendSibling(middle);

        IChatComponent right = new ChatComponentText("] ");
        right.getChatStyle().setColor(EnumChatFormatting.GOLD);
        root.appendSibling(right);

        return root;
    }

    public static void addChatComponentWithPrefix(IChatComponent component) {
        IChatComponent prefix = createChatPrefix();

        prefix.appendSibling(component);

        Minecraft.getMinecraft().thePlayer.addChatComponentMessage(prefix);
    }

    @Override
    public UUID getCurrentPlayerUUID() {
        if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null)
            return Minecraft.getMinecraft().thePlayer.getUniqueID();
        else
            return null;
    }

    @Override
    public void createNewBidEvent(Auction auction) {
        AuctionNewBidEvent event = new AuctionNewBidEvent(auction);
        MinecraftForge.EVENT_BUS.post(event);
    }

    @Override
    public void createOutbidEvent(Auction auction) {
        AuctionOutbidEvent event = new AuctionOutbidEvent(auction);
        MinecraftForge.EVENT_BUS.post(event);
    }

    @Override
    public void createEndingSoonEvent(Auction auction) {
        AuctionEndingSoonEvent event = new AuctionEndingSoonEvent(auction);
        MinecraftForge.EVENT_BUS.post(event);
    }


    /**
     * Gets an auction's current status (as of last sync) by its UUID.
     * 
     * @param auctionUUID The UUID of the auction to get state of.
     * @return The specified auction's current state or a special Error Auction.
     */
    public Auction getAuction(UUID auctionUUID) {
        return AfACore.getAllAuctions().getOrDefault(auctionUUID, Auction.ERROR_AUCTION);
    }

    /**
     * Gets all auctions owner by a player.
     * 
     * @param playerUUID The UUID of the auction owner.
     * @return Array of current auction states of all auctions owner by the
     *         specified player.
     */
    public Auction[] getAuctionsByPlayer(UUID playerUUID) {
        return AfACore.getPlayerAuctions(playerUUID).toArray(new Auction[0]);
    }

    /**
     * @return An array containing all currently known auctions' states.
     */
    public Auction[] getAuctions() {
        return AfACore.getAllAuctions().values().toArray(new Auction[0]);
    }

    /**
     * @return An array of Strings with all currently known items up for auction.
     */
    public String[] getAuctionItems() {
        return AfACore.getAllItems();
    }

    /**
     * Checks if item string exactly matches an item up for auction
     * 
     * @param item Auction item name
     * @return True if at item name matches an auction item, false otherwise.
     */
    public boolean isAuctionItem(String item) {
        List<String> items = new ArrayList<>();
        Collections.addAll(items, AfACore.getAllItems());
        return items.contains(item.toLowerCase());
    }

    /**
     * Gets all known auction items that contain the specified string.
     * 
     * @param filter The string to filter the results.
     * @return Array with all currently known auction items that contain the filter
     *         string.
     */
    public String[] getAuctionItems(String filter) {
        List<String> result = new ArrayList<>();
        for (String item : AfACore.getAllItems()) {
            if (item.contains(filter.toLowerCase())) {
                if (AfACore.getItemAuctions(item).size() > 0) {
                    result.add(AfACore.getItemAuctions(item).get(0).getItemName());
                } else {
                    result.add(item);
                }
            }
        }
        return result.toArray(new String[0]);
    }

    /**
     * Gets all auctions that contain the specified item.
     * 
     * @param itemName The name of the item being auctioned.
     * @return Array of current auction states for all auctions of that item.
     */
    public Auction[] getAuctionsByItem(String itemName) {
        return AfACore.getItemAuctions(itemName).toArray(new Auction[0]);
    }

    @Override
    public AwayFromAuctionCore getCore() {
        return AfACore;
    }

    @Override
    public int getItemCountFromNBTStream(Base64InputStream inputStream) throws IOException {
            NBTTagCompound tag = CompressedStreamTools.readCompressed(inputStream);
            return tag.getTagList("i", 10).getCompoundTagAt(0).getInteger("Count");
    }

    @Override
    public void setHypixelAPIKey(String apiKey) {
        if (AfACore.testAPIKey(apiKey)) {
            Config.setHypixelKey(apiKey);
        }
    }

    /**
     * @return Array of current auction states for all active auctions currently bid
     *         on by the player.
     */
    public Auction[] getBidOnAuctions() {
        return AfACore.getBidOnAuctions().toArray(new Auction[0]);
    }

    /**
     * @return The current total number of coins sitting in the auction house in
     *         bids.
     */
    public long getTotalCoins() {
        return AfACore.getTotalCoins();
    }

    /**
     * Gets a list of all the bazaar products.
     * 
     * @return A list of all bazaar products as {@link BazaarReply.Product} objects.
     */
    public List<BazaarReply.Product> getBazaarProducts() {
        return getBazaarProducts("");
    }

    /**
     * Gets a subset of the bazaar products that contain the filter, case
     * insensitive.
     * 
     * @param filter The string that must be part or all of the product name.
     * @return The list of bazaar products that contain the filter as
     *         {@link BazaarReply.Product} objects.
     */
    public List<BazaarReply.Product> getBazaarProducts(String filter) {
        List<BazaarReply.Product> products = new ArrayList<>();
        Map<String, BazaarReply.Product> bazaarState = AfACore.getBazaarProducts();
        for (String id : bazaarState.keySet()) {
            if (AfACore.getItemName(bazaarState.get(id).getProductId()).toLowerCase().contains(filter.toLowerCase())) {
                products.add(bazaarState.get(id));
            }
        }

        return products;
    }

    private static final String LOG_PREFIX = "[AfA] ";

    @Override
    public void logDebug(String msg) {
        LOGGER.debug(LOG_PREFIX + msg);
    }

    @Override
    public void logInfo(String msg) {
        LOGGER.info(LOG_PREFIX + msg);
    }

    @Override
    public void logWarn(String msg) {
        LOGGER.warn(LOG_PREFIX + msg);
    }

    @Override
    public void logError(String msg) {
        LOGGER.error(LOG_PREFIX + msg);
    }

    @Override
    public void logException(Exception exception) {
        LOGGER.catching(exception);
    }

    @Override
    public boolean isMinecraftNull() {
        return Minecraft.getMinecraft() == null;
    }

    @Override
    public boolean onHypixel() {
        if (Minecraft.getMinecraft() == null) {
            return false;
        } else if (Minecraft.getMinecraft().getCurrentServerData() == null) {
            return false;
        }

        return Minecraft.getMinecraft().getCurrentServerData().serverIP.contains(".hypixel.net");
    }

    @Override
    public ConfigState getConfigState() {
        return new ConfigState(Config.GENERAL_REFRESH_DELAY, Config.GENERAL_ALWAYS_NOTIFY, Config.HYPIXEL_API_KEY);
    }

    public static IChatComponent convertTextComponent(ITextComponent component) {
        IChatComponent chatComponent = new ChatComponentText(component.getAsUnformattedString());

        chatComponent.getChatStyle().setBold(component.getBold()).setItalic(component.getItalicized()).setUnderlined(component.getUnderlined()).setColor(convertTextColor(component.getColor()));

        net.minecraft.event.ClickEvent clickEvent = convertClickEvent(component.getClickEvent());
        if (clickEvent != null) chatComponent.getChatStyle().setChatClickEvent(clickEvent);

        net.minecraft.event.HoverEvent hoverEvent = convertHoverEvent(component.getHoverEvent());
        if (hoverEvent != null) chatComponent.getChatStyle().setChatHoverEvent(hoverEvent);

        for (ITextComponent sibling : component.getSiblings()) {
            chatComponent.appendSibling(convertTextComponent(sibling));
        }

        return chatComponent;
    }

    public static EnumChatFormatting convertTextColor(AfAUtils.ColorEnum color) {
        switch(color) {
            case BLACK:
                return EnumChatFormatting.BLACK;
            case DARK_BLUE:
                return EnumChatFormatting.DARK_BLUE;
            case DARK_GREEN:
                return EnumChatFormatting.DARK_GREEN;
            case DARK_AQUA:
                return EnumChatFormatting.DARK_AQUA;
            case DARK_RED:
                return EnumChatFormatting.DARK_RED;
            case DARK_PURPLE:
                return EnumChatFormatting.DARK_PURPLE;
            case GOLD:
                return EnumChatFormatting.GOLD;
            case GRAY:
                return EnumChatFormatting.GRAY;
            case DARK_GRAY:
                return EnumChatFormatting.DARK_GRAY;
            case BLUE:
                return EnumChatFormatting.BLUE;
            case GREEN:
                return EnumChatFormatting.GREEN;
            case AQUA:
                return EnumChatFormatting.AQUA;
            case RED:
                return EnumChatFormatting.RED;
            case LIGHT_PURPLE:
                return EnumChatFormatting.LIGHT_PURPLE;
            case YELLOW:
                return EnumChatFormatting.YELLOW;
            case WHITE:
                return EnumChatFormatting.WHITE;
            default:
                LOGGER.warn("[AfA] Unknown color sent from core! Color:" + color.toString());
                return EnumChatFormatting.BLACK;
        }
    }

    public static net.minecraft.event.ClickEvent convertClickEvent(ClickEvent event) {
        if (event == null) return null;
        switch(event.getAction()) {
            case OPEN_URL:
                return new net.minecraft.event.ClickEvent(net.minecraft.event.ClickEvent.Action.OPEN_URL, event.getValue());
            case RUN_COMMAND:
                return new net.minecraft.event.ClickEvent(net.minecraft.event.ClickEvent.Action.RUN_COMMAND, event.getValue());
            case SUGGEST_COMMAND:
                return new net.minecraft.event.ClickEvent(net.minecraft.event.ClickEvent.Action.SUGGEST_COMMAND, event.getValue());
            case OPEN_FILE:
                return new net.minecraft.event.ClickEvent(net.minecraft.event.ClickEvent.Action.OPEN_FILE, event.getValue());
            case CHANGE_PAGE:
                return new net.minecraft.event.ClickEvent(net.minecraft.event.ClickEvent.Action.CHANGE_PAGE, event.getValue());
            default:
                return null;
        }
    }

    public static net.minecraft.event.HoverEvent convertHoverEvent(HoverEvent event) {
        if (event == null) return null;
        switch (event.getAction()) {
            case SHOW_ITEM:
                return new net.minecraft.event.HoverEvent(net.minecraft.event.HoverEvent.Action.SHOW_ITEM, convertTextComponent(event.getDetails()));
            case SHOW_ACHIEVEMENT:
                return new net.minecraft.event.HoverEvent(net.minecraft.event.HoverEvent.Action.SHOW_ACHIEVEMENT, convertTextComponent(event.getDetails()));
            case SHOW_ENTITY:
                return new net.minecraft.event.HoverEvent(net.minecraft.event.HoverEvent.Action.SHOW_ENTITY, convertTextComponent(event.getDetails()));
            case SHOW_TEXT:
                return new net.minecraft.event.HoverEvent(net.minecraft.event.HoverEvent.Action.SHOW_TEXT, convertTextComponent(event.getDetails()));
            default:
                return null;
        }
    }

    public static ItemStack convertBookInfoToBook(IBookInfo bookInfo) {

        ItemStack bookStack = new ItemStack(new ItemEditableBook());
        bookStack.setTagCompound(new NBTTagCompound());
        bookStack.getTagCompound().setString("title", "AwayFromAuction");
        bookStack.getTagCompound().setString("author", "MoSadie");

        AwayFromAuction.getLogger().info("[AfA] Begin page creation. Total Pages: " + bookInfo.getTotalPages());
        NBTTagList pages = bookStack.getTagCompound().getTagList("pages", 8);
        for (int i = 0; i < bookInfo.getTotalPages(); i++) {
            AwayFromAuction.getLogger().info("[AfA] Creating page " + (i+1));
            pages.appendTag(new NBTTagString(IChatComponent.Serializer.componentToJson(convertTextComponent(bookInfo.getPageContent(i)))));
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

}