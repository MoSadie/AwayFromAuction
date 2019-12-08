package io.github.mosadie.awayfromauction.data;

import io.github.mosadie.awayfromauction.AwayFromAuction;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;

public class GeneratorLanguage extends LanguageProvider {

    public GeneratorLanguage(DataGenerator gen) {
        super(gen, AwayFromAuction.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        addPrefixed("autoapikey.success", "[AfA] Sucessfully grabbed your Hypixel API key from chat!");
        addPrefixed("command.usage", "[AfA] Usage: \n/afa key <key>: to set your Hypixel API key.\n/afa test: Test your API key.\n/afa view <uuid>: Shows info on an auction.\n/afa viewall: View all auctions.\n/afa supriseme: Shows a random auction.\n/afa search [Item Name]: Search the auction house for a specific item.\n/afa searchuser [Username]: Search the auction house for auctions by a specific user.\n/afa joinhypixel: Promps you to join the Hypixel server.");
        addPrefixed("command.key.success", "[AfA] Sucessfully saved your Hypixel API key!");
        addPrefixed("command.key.fail", "[AfA] Failed to save your Hypixel API key! Please try again!");
        addPrefixed("command.view.usage", "[AfA] /afa view <auction uuid>: Pulls up detailed information on a specific auction. Use /afa search or /afa searchuser to search the auction house.");
        addPrefixed("command.view.success", "[AfA] Viewing auction %s. Click this message to view again.");
        addPrefixed("command.supriseme.success","[AfA] Viewing a random auction, please wait...");
        addPrefixed("command.supriseme.fail","[AfA] Auctions not synced yet! Please wait about %d seconds and try again.");
        addPrefixed("command.search.help", "[AfA] Usage: /afa search [Item Name]");
        addPrefixed("command.search.itemnotfound", "[AfA] No item by that name could be found!");
        addPrefixed("command.searchuser.help", "[AfA] Usage: /afa searchuser [Username]");
        addPrefixed("command.searchuser.notfound", "[AfA] No user by that username could be found!");
        addPrefixed("command.stats", "[AfA] Current Auction House Stats:\n* Number of currently active auctions: %d\n* Number of unique items up for auction (incl. reforges): %d\n* Number of active auctions you own: %d\n* Number of active auctions you've bid on: %d\n* Number of coins currently in bids in the auction house: %d");
        addPrefixed("apitest.start", "[AfA] Testing your Hypixel API key..");
        addPrefixed("apitest.succeed", "[AfA] Your Hypixel API key works!");
        addPrefixed("apitest.fail", "[AfA] You Hypixel API key didn't work. Make sure you set it correctly, and try again!");
        addPrefixed("command.joinhypixel.start", "[AfA] Now connecting to Hypixel...");
        addPrefixed("command.joinhypixel.fail", "[AfA] Didn't connect to Hypixel! Already connected!");
        addPrefixed("gui.joinhypixel.title", "WARNING: About to change servers!");
        addPrefixed("gui.joinhypixel.body", "You are about to leave your current server/world and join mc.hypixel.net.\n\nAre you sure you want to do this?");
    }

    private void addPrefixed(String key, String text) {
        add(String.format("%s.%s", AwayFromAuction.MOD_ID, key), text);
    }
    
}