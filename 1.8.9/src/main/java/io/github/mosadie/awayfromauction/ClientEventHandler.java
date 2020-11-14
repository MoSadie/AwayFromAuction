package io.github.mosadie.awayfromauction;

import io.github.mosadie.awayfromauction.core.AfAUtils;
import io.github.mosadie.awayfromauction.core.Auction;
import io.github.mosadie.awayfromauction.core.AwayFromAuctionCore;
import io.github.mosadie.awayfromauction.event.AuctionEndingSoonEvent;
import io.github.mosadie.awayfromauction.event.AuctionNewBidEvent;
import io.github.mosadie.awayfromauction.event.AuctionOutbidEvent;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.ForgeVersion.CheckResult;
import net.minecraftforge.common.ForgeVersion.Status;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;

public class ClientEventHandler {
    private final AwayFromAuction mod;

    public ClientEventHandler(AwayFromAuction mod) {
        this.mod = mod;
    }

    @SubscribeEvent
    public void onReceiveChat(ClientChatReceivedEvent event) {
        mod.getCore().startSyncThread(); // Ensures the sync thread is created
        String message = event.message.getUnformattedText();

        // Checks to see if message is about a new API key and if we are on Hypixel
        if (message.startsWith("Your new API key is ") && mod.onHypixel()) {
            AwayFromAuction.getLogger().info("API Key message detected!");
            try {
                String key = message.split("Your new API key is ")[1];
                if (AwayFromAuctionCore.validateAPIKey(key)) {
                    if (mod.getCore().setAPIKey(key)) {
                        AwayFromAuction.addChatComponentWithPrefix(
                                AwayFromAuction.getTranslatedTextComponent("autoapikey.success"));
                    }
                }
            } catch (Exception e) {
                AwayFromAuction.getLogger().warn("Exception occurred setting API key: " + e.getLocalizedMessage());
                AwayFromAuction
                        .addChatComponentWithPrefix(AwayFromAuction.getTranslatedTextComponent("autoapikey.fail"));
            }
        }
    }

    @SubscribeEvent
    public void onConnect(ClientConnectedToServerEvent event) {
        // Check for updates, and notify player if update is available.
        CheckResult versionCheck = ForgeVersion
                .getResult(Loader.instance().getIndexedModList().get(AwayFromAuction.MOD_ID));
        if (versionCheck.status == Status.OUTDATED) {
            AwayFromAuction.addChatComponentWithPrefix(AwayFromAuction.getTranslatedTextComponent("notice.outofdate",
                    versionCheck.target.toString(), versionCheck.changes.get(versionCheck.target)));
        }

        mod.getCore().startSyncThread();
    }

    @SubscribeEvent
    public void onDisconnect(ClientDisconnectionFromServerEvent event) {
        mod.getCore().stopSyncThread();
    }

    @SubscribeEvent
    public void onAuctionEndingSoon(AuctionEndingSoonEvent event) {
        if (!Config.GENERAL_ALWAYS_NOTIFY && mod.onHypixel()) {
            return;
        }

        Auction auction = event.getAuction();

        ChatComponentText root = new ChatComponentText("Your auction for ");
        root.getChatStyle().setColor(EnumChatFormatting.WHITE);
        ChatComponentText itemName = new ChatComponentText(auction.getItemName());
        itemName.getChatStyle().setUnderlined(true)
                .setChatClickEvent(
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa view " + auction.getAuctionUUID()))
                .setChatHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Click to view auction!")));
        long time = (auction.getEnd().getTime() - auction.getSyncTimestamp().getTime()) / 1000;
        ChatComponentText endingTime = new ChatComponentText(
                " is ending in about " + time + " second" + (time > 1 ? "s" : "") + "! ");

        IChatComponent hypixelLink = AwayFromAuction.convertTextComponent(AfAUtils.createHypixelLink(mod));

        root.appendSibling(itemName);
        root.appendSibling(endingTime);
        root.appendSibling(hypixelLink);

        AwayFromAuction.addChatComponentWithPrefix(root);
    }

    @SubscribeEvent
    public void onAuctionNewBid(AuctionNewBidEvent event) {
        if (!Config.GENERAL_ALWAYS_NOTIFY && mod.onHypixel()) {
            return;
        }

        Auction auction = event.getAuction();

        ChatComponentText root = new ChatComponentText("There is a new bid on your auction for the ");
        root.getChatStyle().setColor(EnumChatFormatting.WHITE);

        ChatComponentText itemName = new ChatComponentText(auction.getItemName());
        itemName.getChatStyle().setUnderlined(true).setColor(AwayFromAuction.convertTextColor(AfAUtils.getColorFromTier(auction.getTier())))
                .setChatClickEvent(
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa view " + auction.getAuctionUUID()))
                .setChatHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Click to view auction!")));

        int newBid = auction.getHighestBidAmount();
        String otherUser;
        try {
            otherUser = mod.getCore().getCachedPlayerName(event.getBid().getBidderUUID());
            if (otherUser == null) {
                otherUser = "someone";
            }
        } catch (NullPointerException e) {
            AwayFromAuction.getLogger().error("NullPointer encountered getting player name!", e);
            otherUser = "someone"; // For the chat message
        }
        ChatComponentText bidInfo = new ChatComponentText(
                " for " + AfAUtils.formatCoins(newBid) + " coin" + (newBid > 1 ? "s" : "") + " by " + otherUser + "! ");

        IChatComponent hypixelLink = AwayFromAuction.convertTextComponent(AfAUtils.createHypixelLink(mod));

        root.appendSibling(itemName);
        root.appendSibling(bidInfo);
        root.appendSibling(hypixelLink);

        AwayFromAuction.addChatComponentWithPrefix(root);
    }

    @SubscribeEvent
    public void onAuctionOutbid(AuctionOutbidEvent event) {
        if (!Config.GENERAL_ALWAYS_NOTIFY && mod.onHypixel()) {
            return;
        }

        Auction auction = event.getAuction();

        ChatComponentText root = new ChatComponentText("You have been outbid on the auction for ");
        root.getChatStyle().setColor(EnumChatFormatting.WHITE);

        ChatComponentText itemName = new ChatComponentText(auction.getItemName());
        itemName.getChatStyle().setUnderlined(true).setColor(AwayFromAuction.convertTextColor(AfAUtils.getColorFromTier(auction.getTier())))
                .setChatClickEvent(
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afa view " + auction.getAuctionUUID()))
                .setChatHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Click to view auction!")));

        String otherUser = mod.getCore().getCachedPlayerName(auction.getHighestBid().getBidderUUID());
        if (otherUser == null) {
            otherUser = "someone";
        }
        ChatComponentText outbidBy = new ChatComponentText(" by " + AfAUtils.formatCoins(event.getOutbidAmount())
                + " coin" + (event.getOutbidAmount() > 1 ? "s" : "") + " by " + otherUser + "! ");
        IChatComponent hypixelLink = AwayFromAuction.convertTextComponent(AfAUtils.createHypixelLink(mod));
        root.appendSibling(itemName);
        root.appendSibling(outbidBy);
        root.appendSibling(hypixelLink);

        AwayFromAuction.addChatComponentWithPrefix(root);
    }
}
