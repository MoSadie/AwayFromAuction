package io.github.mosadie.awayfromauction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import io.github.mosadie.awayfromauction.AwayFromAuction.BazaarProduct;
import io.github.mosadie.awayfromauction.gui.AuctionBookInfo;
import io.github.mosadie.awayfromauction.gui.AuctionSearchBookInfo;
import io.github.mosadie.awayfromauction.gui.AuctionSelectItemBookInfo;
import io.github.mosadie.awayfromauction.gui.AuctionsBookInfo;
import io.github.mosadie.awayfromauction.gui.BazaarBookInfo;
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
import net.minecraft.item.ItemStack;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

public class AfACommand extends CommandBase {

    private final AwayFromAuction mod;

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
        return StatCollector.translateToLocalFormatted(AwayFromAuction.MOD_ID + ".command.usage");
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {

        mod.createSyncThread();

        if (args.length < 1) {
            AwayFromAuction.addChatComponentWithPrefix(AwayFromAuction.getTranslatedTextComponent("command.usage"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "key":
                if (args.length < 2) {
                    AwayFromAuction
                            .addChatComponentWithPrefix(AwayFromAuction.getTranslatedTextComponent("command.usage"));
                    return;
                }

                String key = args[1];

                if (mod.validateAPIKey(key)) {
                    AwayFromAuction
                            .addChatComponentWithPrefix(AwayFromAuction.getTranslatedTextComponent("apitest.start"));
                    if (mod.testAPIKey(key)) {
                        AwayFromAuction.addChatComponentWithPrefix(
                                AwayFromAuction.getTranslatedTextComponent("apitest.succeed"));
                        mod.setAPIKey(key);
                        AwayFromAuction.addChatComponentWithPrefix(
                                AwayFromAuction.getTranslatedTextComponent("command.key.success"));
                        return;
                    } else {
                        AwayFromAuction
                                .addChatComponentWithPrefix(AwayFromAuction.getTranslatedTextComponent("apitest.fail"));
                        return;
                    }
                } else {
                    AwayFromAuction
                            .addChatComponentWithPrefix(AwayFromAuction.getTranslatedTextComponent("command.key.fail"));
                    return;
                }

            case "test":
                AwayFromAuction.addChatComponentWithPrefix(AwayFromAuction.getTranslatedTextComponent("apitest.start"));
                if (mod.testAPIKey(Config.HYPIXEL_API_KEY)) {
                    AwayFromAuction
                            .addChatComponentWithPrefix(AwayFromAuction.getTranslatedTextComponent("apitest.succeed"));
                } else {
                    AwayFromAuction
                            .addChatComponentWithPrefix(AwayFromAuction.getTranslatedTextComponent("apitest.fail"));
                }
                break;

            case "stats":
                AwayFromAuction.addChatComponentWithPrefix(AwayFromAuction.getTranslatedTextComponent("command.stats",
                        mod.getAuctions().length, mod.getAuctionItems().length,
                        mod.getAuctionsByPlayer(Minecraft.getMinecraft().thePlayer.getUniqueID()).length,
                        mod.getBidOnAuctions().length, AfAUtils.formatCoins(mod.getTotalCoins())));
                break;

            case "mine":
            case "me":
            case "myauctions":
            case "viewmine":
            case "viewme":
                args = new String[] { args[0], Minecraft.getMinecraft().thePlayer.getUniqueID().toString() };
                // Fall to the searchuser command

            case "searchuser":
            case "usersearch":
                if (args.length != 2) {
                    AwayFromAuction.addChatComponentWithPrefix(
                            AwayFromAuction.getTranslatedTextComponent("command.searchuser.help"));
                    return;
                }
                UUID userUUID;
                try {
                    // Try to resolve UUID from String.
                    String uuidString = args[1];
                    if (!uuidString.contains("-")) {
                        uuidString = AfAUtils.addHyphens(uuidString);
                    }
                    userUUID = UUID.fromString(uuidString);
                } catch (IllegalArgumentException e) {
                    // It's fine, we fall back to username lookup.
                    userUUID = mod.getPlayerUUID(args[1]);
                }

                if (userUUID == null) {
                    AwayFromAuction.addChatComponentWithPrefix(
                            AwayFromAuction.getTranslatedTextComponent("command.searchuser.notfound"));
                    return;
                }
                Auction[] userAuctions = mod.getAuctionsByPlayer(userUUID);
                AuctionSearchBookInfo searchBookInfo = new AuctionSearchBookInfo(userAuctions, args[1]);
                ItemStack searchBookItemStack = AfAUtils.convertBookInfoToBook(searchBookInfo);
                AfAUtils.displayBook(searchBookItemStack);
                break;

            case "search":
            case "searchitem":
            case "itemsearch":
                if (args.length < 2) {
                    AwayFromAuction.addChatComponentWithPrefix(
                            AwayFromAuction.getTranslatedTextComponent("command.search.help"));
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
                        AwayFromAuction.addChatComponentWithPrefix(
                                AwayFromAuction.getTranslatedTextComponent("command.search.itemnotfound"));
                        return;
                    } else if (possibleItems.length == 1) {
                        itemAuctions = mod.getAuctionsByItem(possibleItems[0]);
                    } else {
                        AuctionSelectItemBookInfo itemSelectBookInfo = new AuctionSelectItemBookInfo(possibleItems,
                                mod);
                        AfAUtils.displayBook(AfAUtils.convertBookInfoToBook(itemSelectBookInfo));
                        return;
                    }
                }
                AuctionSearchBookInfo itemAuctionSearchBookInfo = new AuctionSearchBookInfo(itemAuctions, item);
                AwayFromAuction.getLogger().debug("Start BookItemStack");
                ItemStack book = AfAUtils.convertBookInfoToBook(itemAuctionSearchBookInfo);
                AwayFromAuction.getLogger().debug("End BookItemStack. Begin Display");
                AfAUtils.displayBook(book);
                AwayFromAuction.getLogger().debug("End Display");
                break;

            case "joinhypixel":
                if (AfAUtils.onHypixel()) {
                    AwayFromAuction.addChatComponentWithPrefix(
                            AwayFromAuction.getTranslatedTextComponent("command.joinhypixel.fail"));
                    return;
                }
                AwayFromAuction.addChatComponentWithPrefix(
                        AwayFromAuction.getTranslatedTextComponent("command.joinhypixel.start"));

                GuiYesNo confirmScreen = new GuiYesNo(new JoinHypixelYesNoCallback(),
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

            // case "viewall":
            // Auction[] allAuctions = mod.getAuctions();
            // AuctionsBookInfo auctionsBookInfo = new AuctionsBookInfo(allAuctions);
            // AfAUtils.displayBook(AfAUtils.convertBookInfoToBook(auctionsBookInfo));
            // break;

            case "viewbids":
                Auction[] bidAuctions = mod.getBidOnAuctions();
                AuctionsBookInfo bidAuctionsBookInfo = new AuctionsBookInfo(bidAuctions);
                AfAUtils.displayBook(AfAUtils.convertBookInfoToBook(bidAuctionsBookInfo));
                break;

            case "supriseme":
                AwayFromAuction.addChatComponentWithPrefix(
                        AwayFromAuction.getTranslatedTextComponent("command.supriseme.success"));
                Auction[] auctions = mod.getAuctions();
                if (auctions.length == 0) {
                    AwayFromAuction.addChatComponentWithPrefix(
                            AwayFromAuction.getTranslatedTextComponent("error.notsync", Config.GENERAL_REFRESH_DELAY));
                    return;
                }
                Auction randAuction = auctions[new Random().nextInt(auctions.length)];
                args = new String[] { "view", randAuction.getAuctionUUID().toString() };
                // Fall to view case

            case "view":
                if (args.length < 2) {
                    AwayFromAuction.addChatComponentWithPrefix(
                            AwayFromAuction.getTranslatedTextComponent("command.view.usage"));
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
                message.getChatStyle().setColor(EnumChatFormatting.WHITE).setChatClickEvent(new ClickEvent(
                        ClickEvent.Action.SUGGEST_COMMAND, "/afa view " + auction.getAuctionUUID().toString()));

                AwayFromAuction.addChatComponentWithPrefix(message);

                AfAUtils.displayBook(AfAUtils.convertBookInfoToBook(auctionBookInfo));
                break;

            case "bazaar":
                List<BazaarProduct> products;
                if (args.length == 1) { // Show book of all items.
                    products = mod.getBazaarProducts();
                } else if (args.length > 1) { // Show book of filtered items.
                    String filter = args[1];
                    for (int i = 2; i < args.length; i++) {
                        filter += " " + args[i];
                    }
                    products = mod.getBazaarProducts(filter);
                } else { // Something went very wrong. Error out using an empty product list.
                    products = new ArrayList<>();
                }

                if (products.size() == 0) {
                    ChatComponentText bazaarNoItemMessage = AwayFromAuction
                            .getTranslatedTextComponent("command.bazaar.noitemsfound");
                    AwayFromAuction.addChatComponentWithPrefix(bazaarNoItemMessage);
                } else {
                    BazaarBookInfo bazaarBookInfo = new BazaarBookInfo(products.toArray(new BazaarProduct[] {}));
                    AfAUtils.displayBook(AfAUtils.convertBookInfoToBook(bazaarBookInfo));
                }
                break;

            default:
                AwayFromAuction.addChatComponentWithPrefix(AwayFromAuction.getTranslatedTextComponent("command.usage"));
        }
    }

    private class JoinHypixelYesNoCallback implements GuiYesNoCallback {

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
