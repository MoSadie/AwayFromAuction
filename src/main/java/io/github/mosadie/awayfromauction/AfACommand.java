package io.github.mosadie.awayfromauction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.github.mosadie.awayfromauction.gui.AuctionBookInfo;
import io.github.mosadie.awayfromauction.gui.AuctionSearchBookInfo;
import io.github.mosadie.awayfromauction.gui.AuctionSelectItemBookInfo;
import io.github.mosadie.awayfromauction.gui.AuctionsBookInfo;
import io.github.mosadie.awayfromauction.util.AfAUtils;
import io.github.mosadie.awayfromauction.util.Auction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;

public class AfACommand extends CommandBase {

    private final AwayFromAuction mod;

    private JsonObject debugAuctionJson;

    public AfACommand(AwayFromAuction mod) {
        super();
        this.mod = mod;
    }

    @Override
    public String getCommandName() {
        return "afa";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "[AfA] Usage:\n/afa viewall: View all auctions.\n/afa viewbids: View all auctions you've bid on.\n/afa search [Item Name]: Search the auction house for a specific item.\n/afa searchuser [Username]: Search the auction house for active auctions by a specific user.\n/afa supriseme: Shows a random auction.\n/afa view <uuid>: Shows info on an auction.\n/afa stats: Shows some various stats about the auction house.\n/afa key <key>: Manually set your Hypixel API key.\n/afa test: Test your API key.\n/afa joinhypixel: Promps you to join the Hypixel server.";
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        AwayFromAuction.getLogger().debug("Command received! Command: " + CommandBase.buildString(args, 0));

        mod.createSyncThread();

        if (args.length < 1) {
            Minecraft.getMinecraft().thePlayer
                    .addChatMessage(AwayFromAuction.getTranslatedTextComponent("command.usage"));
            return;
        }

        switch (args[0].toLowerCase()) {
        // case "debug":
        // switch (args[1].toLowerCase()) {
        // case "on":
        // mod.stopSyncThread();
        // mod.clearCache();
        // debugAuctionJson = new JsonObject();
        // debugAuctionJson.add("uuid", new
        // JsonPrimitive("00000000000000000000000000000001"));
        // debugAuctionJson.add("auctioneer", new
        // JsonPrimitive("00000000000000000000000000000002"));
        // debugAuctionJson.add("profile_id", new
        // JsonPrimitive("00000000000000000000000000000003"));

        // JsonArray debugCoopArray = new JsonArray();
        // debugCoopArray.add(new JsonPrimitive("00000000000000000000000000000002"));
        // debugAuctionJson.add("coop", debugCoopArray);

        // debugAuctionJson.add("start", new JsonPrimitive(new Date().getTime()));
        // debugAuctionJson.add("end", new JsonPrimitive(new Date().getTime() + 600));
        // debugAuctionJson.add("item_name", new JsonPrimitive("Debug Auction Item"));
        // debugAuctionJson.add("item_lore", new JsonPrimitive("Debuce Auction Item
        // Lore\nMultiline!"));
        // debugAuctionJson.add("extra", new JsonPrimitive("Debug Auction Extra"));
        // debugAuctionJson.add("category", new JsonPrimitive("Debug"));
        // debugAuctionJson.add("tier", new JsonPrimitive("SPECIAL"));
        // debugAuctionJson.add("starting_bid", new JsonPrimitive(100));

        // try {
        // NBTTagCompound debugNbtTag = new NBTTagCompound();
        // ItemStack debugItemStack = new ItemStack(Blocks.stone, 12);
        // NBTTagList list = new NBTTagList();
        // list.appendTag(debugItemStack.serializeNBT());
        // debugNbtTag.setTag("i", list);

        // ByteArrayOutputStream debugNBTOutputStream = new ByteArrayOutputStream();
        // CompressedStreamTools.writeCompressed(debugNbtTag, debugNBTOutputStream);
        // debugAuctionJson.add("item_bytes", new
        // JsonPrimitive(debugNBTOutputStream.toString()));
        // } catch (IOException e) {
        // AwayFromAuction.getLogger().error("Something went wrong creating debug
        // auction.", e);
        // debugAuctionJson.add("item_bytes", new JsonPrimitive(""));
        // }

        // debugAuctionJson.add("claimed", new JsonPrimitive(false));

        // JsonArray debugClaimedBidders = new JsonArray();
        // debugClaimedBidders.add(
        // new
        // JsonPrimitive(AfAUtils.removeHyphens(Minecraft.getMinecraft().thePlayer.getUniqueID())));
        // debugAuctionJson.add("claimed_bidders", debugClaimedBidders);
        // debugAuctionJson.add("highest_bid_amount", new JsonPrimitive(100));

        // JsonArray debugBidsArray = new JsonArray();
        // JsonObject debugBid = new JsonObject();
        // debugBid.add("auction_id", new
        // JsonPrimitive("00000000000000000000000000000001"));
        // debugBid.add("bidder",
        // new
        // JsonPrimitive(AfAUtils.removeHyphens(Minecraft.getMinecraft().thePlayer.getUniqueID())));
        // debugBid.add("amount", new JsonPrimitive(100));
        // debugBid.add("timestamp", new JsonPrimitive(new Date().getTime() - 100));
        // debugBidsArray.add(debugBid);
        // debugAuctionJson.add("bids", debugBidsArray);

        // Auction debugAuction = new Auction(debugAuctionJson, mod);

        // Map<UUID, Auction> allAuctions = new HashMap<>();
        // allAuctions.put(debugAuction.getAuctionUUID(), debugAuction);

        // Map<UUID, List<Auction>> playerAuctions = new HashMap<>();
        // playerAuctions.put(debugAuction.getAuctionOwnerUUID(), new ArrayList<>());
        // playerAuctions.get(debugAuction.getAuctionOwnerUUID()).add(debugAuction);

        // Map<String, List<Auction>> itemAuctions = new HashMap<>();
        // itemAuctions.put(debugAuction.getItemName(), new ArrayList<>());
        // itemAuctions.get(debugAuction.getItemName()).add(debugAuction);

        // List<Auction> bidAuctions = new ArrayList<>();
        // bidAuctions.add(debugAuction);

        // mod.updateAuctions(allAuctions, playerAuctions, itemAuctions, bidAuctions);
        // break;

        // case "off":
        // mod.clearCache();
        // break;
        // }
        // break;

        case "key":
            if (args.length < 2) {
                Minecraft.getMinecraft().thePlayer
                        .addChatMessage(AwayFromAuction.getTranslatedTextComponent("command.usage"));
                return;
            }

            String key = args[1];

            if (mod.validateAPIKey(key)) {
                Minecraft.getMinecraft().thePlayer
                        .addChatMessage(AwayFromAuction.getTranslatedTextComponent("apitest.start"));
                if (mod.testAPIKey(key)) {
                    Minecraft.getMinecraft().thePlayer
                            .addChatMessage(AwayFromAuction.getTranslatedTextComponent("apitest.succeed"));
                    mod.setAPIKey(key);
                    Minecraft.getMinecraft().thePlayer
                            .addChatMessage(AwayFromAuction.getTranslatedTextComponent("command.key.success"));
                    return;
                } else {
                    Minecraft.getMinecraft().thePlayer
                            .addChatMessage(AwayFromAuction.getTranslatedTextComponent("apitest.fail"));
                    return;
                }
            } else {
                Minecraft.getMinecraft().thePlayer
                        .addChatMessage(AwayFromAuction.getTranslatedTextComponent("command.key.fail"));
                return;
            }

        case "test":
            Minecraft.getMinecraft().thePlayer
                    .addChatMessage(AwayFromAuction.getTranslatedTextComponent("apitest.start"));
            if (mod.testAPIKey(Config.HYPIXEL_API_KEY)) {
                Minecraft.getMinecraft().thePlayer
                        .addChatMessage(AwayFromAuction.getTranslatedTextComponent("apitest.succeed"));
            } else {
                Minecraft.getMinecraft().thePlayer
                        .addChatMessage(AwayFromAuction.getTranslatedTextComponent("apitest.fail"));
            }
            break;

        case "stats":
            Minecraft.getMinecraft().thePlayer.addChatMessage(AwayFromAuction.getTranslatedTextComponent(
                    "command.stats", mod.getAuctions().length, mod.getAuctionItems().length,
                    mod.getAuctionsByPlayer(Minecraft.getMinecraft().thePlayer.getUniqueID()).length,
                    mod.getBidOnAuctions().length, AfAUtils.formatCoins(mod.getTotalCoins())));
            break;

        case "mine":
        case "me":
        case "myauctions":
        case "viewmine":
        case "viewme":
            args = new String[] {args[0], Minecraft.getMinecraft().thePlayer.getName()};
            // Fall to the searchuser command

        case "searchuser":
            if (args.length != 2) {
                Minecraft.getMinecraft().thePlayer
                        .addChatMessage(AwayFromAuction.getTranslatedTextComponent("command.searchuser.help"));
                return;
            }
            UUID userUUID = mod.getPlayerUUID(args[1]);
            if (userUUID == null) {
                Minecraft.getMinecraft().thePlayer
                        .addChatMessage(AwayFromAuction.getTranslatedTextComponent("command.searchuser.notfound"));
                return;
            }
            Auction[] userAuctions = mod.getAuctionsByPlayer(userUUID);
            AuctionSearchBookInfo searchBookInfo = new AuctionSearchBookInfo(userAuctions, args[1]);
            ItemStack searchBookItemStack = AfAUtils.convertBookInfoToBook(searchBookInfo);
            AfAUtils.displayBook(searchBookItemStack);
            break;

        case "search":
            if (args.length < 2) {
                Minecraft.getMinecraft().thePlayer
                        .addChatMessage(AwayFromAuction.getTranslatedTextComponent("command.search.help"));
                return;
            }
            String item = args[1];
            for (int i = 2; i < args.length; i++) {
                item += " " + args[i];
            }
            Auction[] itemAuctions;
            if (mod.isAuctionItem(item)) {
                itemAuctions = mod.getAuctionsByItem(item);
            } else {
                String[] possibleItems = mod.getAuctionItems(item);
                if (possibleItems.length == 0) {
                    Minecraft.getMinecraft().thePlayer
                            .addChatMessage(AwayFromAuction.getTranslatedTextComponent("command.search.itemnotfound"));
                    return;
                } else if (possibleItems.length == 1) {
                    itemAuctions = mod.getAuctionsByItem(possibleItems[0]);
                } else {
                    AuctionSelectItemBookInfo itemSelectBookInfo = new AuctionSelectItemBookInfo(possibleItems, mod);
                    AfAUtils.displayBook(AfAUtils.convertBookInfoToBook(itemSelectBookInfo));
                    return;
                }
            }
            AuctionSearchBookInfo itemAuctionSearchBookInfo = new AuctionSearchBookInfo(itemAuctions, item);
            AfAUtils.displayBook(AfAUtils.convertBookInfoToBook(itemAuctionSearchBookInfo));
            break;

        case "joinhypixel":
            if (mod.onHypixel()) {
                Minecraft.getMinecraft().thePlayer
                        .addChatMessage(AwayFromAuction.getTranslatedTextComponent("command.joinhypixel.fail"));
                return;
            }
            Minecraft.getMinecraft().thePlayer
                    .addChatMessage(AwayFromAuction.getTranslatedTextComponent("command.joinhypixel.start"));

            GuiYesNo confirmScreen = new GuiYesNo(new YesNoCallback(),
                    AwayFromAuction.getTranslatedTextComponent("gui.joinhypixel.title").getFormattedText(),
                    AwayFromAuction.getTranslatedTextComponent("gui.joinhypixel.body").getFormattedText(), 0);
            Minecraft.getMinecraft().addScheduledTask(() -> {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Minecraft.getMinecraft().displayGuiScreen(confirmScreen);
                    }
                }, 50);
            });
            break;

        case "viewall":
            Auction[] allAuctions = mod.getAuctions();
            AuctionsBookInfo auctionsBookInfo = new AuctionsBookInfo(allAuctions);
            AfAUtils.displayBook(AfAUtils.convertBookInfoToBook(auctionsBookInfo));
            break;

        case "viewbids":
            Auction[] bidAuctions = mod.getBidOnAuctions();
            AuctionsBookInfo bidAuctionsBookInfo = new AuctionsBookInfo(bidAuctions);
            AfAUtils.displayBook(AfAUtils.convertBookInfoToBook(bidAuctionsBookInfo));
            break;

        case "supriseme":
            Minecraft.getMinecraft().thePlayer
                    .addChatMessage(AwayFromAuction.getTranslatedTextComponent("command.supriseme.success"));
            Auction[] auctions = mod.getAuctions();
            if (auctions.length == 0) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(
                        AwayFromAuction.getTranslatedTextComponent("error.notsync", Config.GENERAL_REFRESH_DELAY));
                return;
            }
            Auction randAuction = auctions[new Random().nextInt(auctions.length)];
            args = new String[] { "view", randAuction.getAuctionUUID().toString() };
            // Fall to view case

        case "view":
            if (args.length < 2) {
                Minecraft.getMinecraft().thePlayer
                        .addChatMessage(AwayFromAuction.getTranslatedTextComponent("command.view.usage"));
                return;
            }

            UUID auctionUUID;

            if (args[1].contains("-")) {
                auctionUUID = UUID.fromString(args[1]);
            } else {
                auctionUUID = UUID.fromString(AfAUtils.addHyphens(args[1]));
            }

            Auction auction = mod.getAuction(auctionUUID);
            AuctionBookInfo auctionBookInfo = new AuctionBookInfo(auction);

            ChatComponentText message = AwayFromAuction.getTranslatedTextComponent("command.view.success",
                    auction.getAuctionUUID().toString());
            message.getChatStyle().setUnderlined(true).setChatClickEvent(new ClickEvent(
                    ClickEvent.Action.SUGGEST_COMMAND, "/afa view " + auction.getAuctionUUID().toString()));

            Minecraft.getMinecraft().thePlayer.addChatMessage(message);

            AfAUtils.displayBook(AfAUtils.convertBookInfoToBook(auctionBookInfo));
            break;

        default:
            Minecraft.getMinecraft().thePlayer
                    .addChatMessage(AwayFromAuction.getTranslatedTextComponent("command.usage"));
        }
    }

    private class YesNoCallback implements GuiYesNoCallback {

        @Override
        public void confirmClicked(boolean result, int id) {
            if (result) {
                Minecraft.getMinecraft().addScheduledTask(() -> {

                    // Copied from GuiIngameMenu's disconnect/quit button
                    boolean flag = Minecraft.getMinecraft().isIntegratedServerRunning();
                    boolean flag1 = Minecraft.getMinecraft().func_181540_al();

                    Minecraft.getMinecraft().theWorld.sendQuittingDisconnectingPacket();
                    Minecraft.getMinecraft().loadWorld((WorldClient) null);

                    if (flag) {
                        Minecraft.getMinecraft().displayGuiScreen(new GuiMainMenu());
                    } else if (flag1) {
                        RealmsBridge realmsbridge = new RealmsBridge();
                        realmsbridge.switchToRealms(new GuiMainMenu());
                    } else {
                        Minecraft.getMinecraft().displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
                    }

                    // Connect to Hypixel
                    ServerData hypixelServer = new ServerData("Hypixel", "mc.hypixel.net", false);
                    GuiConnecting connectScreen = new GuiConnecting(Minecraft.getMinecraft().currentScreen,
                            Minecraft.getMinecraft(), hypixelServer);
                    // ConnectingScreen screen = new
                    // ConnectingScreen(Minecraft.getMinecraft().currentScreen,
                    // Minecraft.getMinecraft(), hypixelServer);
                    Minecraft.getMinecraft().displayGuiScreen(connectScreen);
                });
            } else {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    Minecraft.getMinecraft().displayGuiScreen(null);
                });
            }
        }

    }
}
