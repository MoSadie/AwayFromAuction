package io.github.mosadie.awayfromauction;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
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
        mod.createSyncThread();
        String message = event.message.getUnformattedText();
        
        // Checks to see if message is about a new API key
        if (message.startsWith("Your new API key is ") && mod.onHypixel() ) {
            AwayFromAuction.getLogger().info("API Key message autodected!");
            try {
                String key = message.split("Your new API key is ")[1];
                if (mod.validateAPIKey(key)) {
                    if (mod.setAPIKey(key)) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(AwayFromAuction.getTranslatedTextComponent("autoapikey.success"));
                    }
                }
            } catch(Exception e) {
                AwayFromAuction.getLogger().warn("Exception occured setting API key: " + e.getLocalizedMessage());
                Minecraft.getMinecraft().thePlayer.addChatMessage(AwayFromAuction.getTranslatedTextComponent("autoapikey.fail"));
            }
        }
    }
    
    @SubscribeEvent
    public void onConnect(ClientConnectedToServerEvent event) {//WorldEvent.Load event) {
        mod.createSyncThread();
    }
    
    @SubscribeEvent
    public void onDisconnect(ClientDisconnectionFromServerEvent event) {//WorldEvent.Unload event) {
        mod.stopSyncThread();
    }
}
