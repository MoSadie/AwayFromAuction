package io.github.mosadie.awayfromauction;

import java.util.Random;
import java.util.UUID;

import io.github.mosadie.awayfromauction.gui.AuctionBookInfo;
import io.github.mosadie.awayfromauction.util.Auction;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.ConnectingScreen;
import net.minecraft.client.gui.screen.DirtMessageScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.ReadBookScreen;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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
        
        if (event.getMessage().equalsIgnoreCase("/afatest")) { //TODO remove
            AwayFromAuction.getLogger().info("AFATEST command executed.");
            Minecraft.getInstance().player.sendMessage(new StringTextComponent("On Hypixel: " + mod.onHypixel()));
            Auction[] auctions = mod.getAuctions();
            Auction auction = auctions[new Random().nextInt(auctions.length)];
            AuctionBookInfo auctionBookInfo = new AuctionBookInfo(auction);
            
            Minecraft.getInstance().enqueue(() -> {
                Minecraft.getInstance().displayGuiScreen(new ReadBookScreen(auctionBookInfo));
            });
            return;
        }
        
        String[] args = event.getMessage().split(" ");
        
        if (!args[0].equalsIgnoreCase("/afa")) {
            return;
        }
        
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
                            ConnectingScreen screen = new ConnectingScreen(Minecraft.getInstance().currentScreen, Minecraft.getInstance(), "mc.hypixel.net", 25565); //TODO Change to mc.hypixel.net
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
            
            case "supriseme":
            Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.supriseme.success"));
            Auction[] auctions = mod.getAuctions();
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
                auctionUUID = UUID.fromString(addHyphens(args[2]));
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
            
            default:
            Minecraft.getInstance().player.sendMessage(AwayFromAuction.getTranslatedTextComponent("command.usage"));
        }
    }
    
    @SubscribeEvent
    public void onReceiveChat(ClientChatReceivedEvent event) {
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
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGUIOpen(GuiOpenEvent event) {
        AwayFromAuction.getLogger().info("GUI TIME: " + event.getGui().getClass() + " " + event.isCanceled());
    }
    
    private static String addHyphens(String uuid) {
        return uuid.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"); // Shamelessly stolen from StackOverflow  
    }
}
