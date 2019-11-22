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
        addPrefixed("command.usage", "[AfA] Usage: \n/afa key <key>: to set your Hypixel API key.\n/afa test: Test your API key.\n/afa view <uuid>: Shows info on an auction.\n/afa supriseme: Shows a random auction.");
        addPrefixed("command.key.success", "[AfA] Sucessfully saved your Hypixel API key!");
        addPrefixed("command.key.fail", "[AfA] Failed to save your Hypixel API key! Please try again!");
        addPrefixed("command.view.usage", "[AfA] /afa view <auction uuid>: Pulls up detailed information on a specific auction.");
        addPrefixed("command.view.success", "[AfA] Viewing info on auction %s. Click this message to view again.");
        addPrefixed("command.supriseme.success","[AfA] Viewing a random auction, please wait...");
        addPrefixed("apitest.start", "[AfA] Testing your Hypixel API key..");
        addPrefixed("apitest.succeed", "[AfA] Your Hypixel API key works!");
        addPrefixed("apitest.fail", "[AfA] You Hypixel API key didn't work. Make sure you set it correctly, and try again!");
        addPrefixed("command.joinhypixel.start", "[AfA] Now connecting to Hypixel...");
        addPrefixed("command.joinhypixel.fail", "[AfA] Didn't connect to Hypixel! Already connected!s");
        addPrefixed("gui.joinhypixel.title", "WARNING: About to change servers!");
        addPrefixed("gui.joinhypixel.body", "You are about to leave your current server/world and join mc.hypixel.net.\n\nAre you sure you want to do this?");
    }

    private void addPrefixed(String key, String text) {
        add(String.format("%s.%s", AwayFromAuction.MOD_ID, key), text);
    }
    
}