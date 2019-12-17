package io.github.mosadie.awayfromauction;

import java.util.Random;
import java.util.UUID;

import io.github.mosadie.awayfromauction.gui.AuctionBookInfo;
import io.github.mosadie.awayfromauction.gui.AuctionSearchBookInfo;
import io.github.mosadie.awayfromauction.gui.AuctionSelectItemBookInfo;
import io.github.mosadie.awayfromauction.gui.AuctionsBookInfo;
import io.github.mosadie.awayfromauction.util.AfAUtils;
import io.github.mosadie.awayfromauction.util.Auction;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.ConnectingScreen;
import net.minecraft.client.gui.screen.DirtMessageScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.ReadBookScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedInEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientEventHandler {
    private final AwayFromAuction mod;
    
    public ClientEventHandler(AwayFromAuction mod) {
        this.mod = mod;
    }
    
    @SubscribeEvent
    public void onSendChat(ClientChatEvent event) {
        if (!event.getMessage().startsWith("/afa")) {
            return;
        }
        
        event.setCanceled(true); // Stop the message being sent to server

        AwayFromAuction.getLogger().debug("Command received! Command: " + event.getMessage());
        
        String[] args = event.getMessage().split(" ");
        
        if (!args[0].equalsIgnoreCase("/afa")) {
            return;
        }

        mod.createSyncThread();
        
        if (args.length < 2) {
            Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.usage"));
            return;
        }
        
        switch (args[1].toLowerCase()) {
            case "key":
            if (args.length < 3) {
                Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.usage"));
                return;
            }
            
            String key = args[2];
            
            if (mod.validateAPIKey(key)) {
                Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("apitest.start"));
                if (mod.testAPIKey(key)) {
                    Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("apitest.succeed"));
                    mod.setAPIKey(key);
                    Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.key.success"));
                    return;
                } else {
                    Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("apitest.fail"));
                    return;
                }
            } else {
                Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.key.fail"));
                return;
            }
            
            case "test":
            Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("apitest.start"));
            if (mod.testAPIKey(Config.HYPIXEL_API_KEY.get())) {
                Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("apitest.succeed"));
            } else {
                Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("apitest.fail"));
            }
            break;

            case "stats":
            Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.stats",
                mod.getAuctions().length,
                mod.getAuctionItems().length,
                mod.getAuctionsByPlayer(Minecraft.getInstance().player.getUniqueID()).length,
                mod.getBidOnAuctions().length,
                AfAUtils.formatCoins(mod.getTotalCoins())));
            break;

            case "searchuser":
            if (args.length != 3) {
                Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.searchuser.help"));
                return;
            }
                UUID userUUID = mod.getPlayerUUID(args[2]);
                if (userUUID == null) {
                    Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.searchuser.notfound"));
                    return;
                }
                Auction[] userAuctions = mod.getAuctionsByPlayer(userUUID);
                AuctionSearchBookInfo searchBookInfo = new AuctionSearchBookInfo(userAuctions, args[2]);
                Minecraft.getInstance().enqueue(() -> {
                    Minecraft.getInstance().displayGuiScreen(new ReadBookScreen(searchBookInfo));
                });
            break;

            case "search":
            if (args.length < 3) {
                Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.search.help"));
                return;
            } 
            String item = args[2];
            for (int i = 3; i < args.length; i++) {
                item += " " + args[i];
            }
            String[] possibleItems = mod.getAuctionItems(item);
            if (possibleItems.length == 0) {
                Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.search.itemnotfound"));
                return;
            } else if (possibleItems.length == 1) {
                Auction[] itemAuctions = mod.getAuctionsByItem(possibleItems[0]);
                AuctionSearchBookInfo itemAuctionSearchBookInfo = new AuctionSearchBookInfo(itemAuctions, item);
                Minecraft.getInstance().enqueue(() -> {
                    Minecraft.getInstance().displayGuiScreen(new ReadBookScreen(itemAuctionSearchBookInfo));
                });
                return;
            } else {
                AuctionSelectItemBookInfo itemSelectBookInfo = new AuctionSelectItemBookInfo(possibleItems, mod);
                Minecraft.getInstance().enqueue(() -> {
                    Minecraft.getInstance().displayGuiScreen(new ReadBookScreen(itemSelectBookInfo));
                });
            }
            break;
            
            case "joinhypixel":
            if (mod.onHypixel()) {
                Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.joinhypixel.fail"));
                return;
            }
            Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.joinhypixel.start"));
            BooleanConsumer consumer = new BooleanConsumer(){
                
                @Override
                public void accept(boolean t) {
                    if (t) {
                        Minecraft.getInstance().enqueue(() -> {

                            //Copied from IngameMenuScreen's disconnect/quit button
                            boolean isIntegratedServer = Minecraft.getInstance().isIntegratedServerRunning();
                            boolean isConnectedToRealms = Minecraft.getInstance().isConnectedToRealms();
                            Minecraft.getInstance().world.sendQuittingDisconnectingPacket();
                            if (isIntegratedServer) {
                                Minecraft.getInstance().func_213231_b(new DirtMessageScreen(new TranslationTextComponent("menu.savingLevel")));
                            } else {
                                Minecraft.getInstance().func_213254_o();
                            }
                            
                            if (isIntegratedServer) {
                                Minecraft.getInstance().displayGuiScreen(new MainMenuScreen());
                            } else if (isConnectedToRealms) {
                                RealmsBridge realmsbridge = new RealmsBridge();
                                realmsbridge.switchToRealms(new MainMenuScreen());
                            } else {
                                Minecraft.getInstance().displayGuiScreen(new MultiplayerScreen(new MainMenuScreen()));
                            }

                            // Connect to Hypixel
                            ServerData hypixelServer = new ServerData("Hypixel","mc.hypixel.net",false);
                            ConnectingScreen screen = new ConnectingScreen(Minecraft.getInstance().currentScreen, Minecraft.getInstance(), hypixelServer);
                            Minecraft.getInstance().displayGuiScreen(screen);
                        });
                    } else {
                        Minecraft.getInstance().enqueue(() -> {
                            Minecraft.getInstance().displayGuiScreen(null);
                        });
                    }
                }
            };
            ConfirmScreen screen = new ConfirmScreen(consumer, AwayFromAuction.getTranslatedTextComponent("gui.joinhypixel.title"), AwayFromAuction.getTranslatedTextComponent("gui.joinhypixel.body"));
            Minecraft.getInstance().enqueue(() -> {
                Minecraft.getInstance().displayGuiScreen(screen);
            });
            break;

            case "viewall":
            Auction[] allAuctions = mod.getAuctions();
            AuctionsBookInfo auctionsBookInfo = new AuctionsBookInfo(allAuctions);
            Minecraft.getInstance().enqueue(() -> {
                Minecraft.getInstance().displayGuiScreen(new ReadBookScreen(auctionsBookInfo));
            });
            break;

            case "viewbids":
            Auction[] bidAuctions = mod.getBidOnAuctions();
            AuctionsBookInfo bidAuctionsBookInfo = new AuctionsBookInfo(bidAuctions);
            Minecraft.getInstance().enqueue(() -> {
                Minecraft.getInstance().displayGuiScreen(new ReadBookScreen(bidAuctionsBookInfo));
            });
            break;
            
            case "supriseme":
            Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.supriseme.success"));
            Auction[] auctions = mod.getAuctions();
            if (auctions.length == 0) {
                Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.supriseme.fail",Config.GENERAL_REFRESH_DELAY));
            }
            Auction randAuction = auctions[new Random().nextInt(auctions.length)];
            args = new String[] {"/afa", "view", randAuction.getAuctionUUID().toString()};
            // Fall to view case
            
            case "view":
            if (args.length < 3) {
                Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.view.usage"));
                return;
            }
            
            UUID auctionUUID;
            
            if (args[2].contains("-")) {
                auctionUUID = UUID.fromString(args[2]);
            } else {
                auctionUUID = UUID.fromString(AfAUtils.addHyphens(args[2]));
            }
            
            Auction auction = mod.getAuction(auctionUUID);
            AuctionBookInfo auctionBookInfo = new AuctionBookInfo(auction);
            
            TranslationTextComponent message = AwayFromAuction.getTranslatedTextComponent("command.view.success", auction.getAuctionUUID().toString());
            message.getStyle().setUnderlined(true).setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/afa view " + auction.getAuctionUUID().toString()));
            
            Minecraft.getInstance().player.sendMessage(message);
            
            Minecraft.getInstance().enqueue(() -> {
                Minecraft.getInstance().displayGuiScreen(new ReadBookScreen(auctionBookInfo));
            });
            break;

            case "forcesync": //TODO Remove debug command
            mod.stopSyncThread();
            mod.createSyncThread();
            Minecraft.getInstance().player.sendMessage(new StringTextComponent("Forcing Sync..."));
            break;
            
            default:
            Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.usage"));
        }
    }
    
    @SubscribeEvent
    public void onReceiveChat(ClientChatReceivedEvent event) {
        mod.createSyncThread();
        String message = event.getMessage().getString();
        
        if (message.startsWith("Your new API key is ") && mod.onHypixel() ) {
            AwayFromAuction.getLogger().info("API Key message autodected!");
            try {
                String key = message.split("Your new API key is ")[1];
                AwayFromAuction.getLogger().debug("API Key: " + key);
                if (mod.validateAPIKey(key)) {
                    if (mod.setAPIKey(key)) {
                        Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("autoapikey.success"));
                    }
                }
            } catch(Exception e) {
                AwayFromAuction.getLogger().warn("Exception occured setting api key: " + e.getLocalizedMessage());
                Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("autoapikey.fail"));
            }
        }
    }

    @SubscribeEvent
    public void onLogin(LoggedInEvent event) {
        mod.createSyncThread();
    }

    @SubscribeEvent
    public void onLogout(LoggedOutEvent event) {
        mod.stopSyncThread();
    }
}
