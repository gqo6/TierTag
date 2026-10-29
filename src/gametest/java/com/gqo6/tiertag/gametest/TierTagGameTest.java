package com.gqo6.tiertag.gametest;

import com.gqo6.tiertag.TierTagMod;
import com.gqo6.tiertag.config.TierConfig;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.nio.file.Path;

public class TierTagGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            TierConfig config = TierTagMod.config();
            config.debugTestTag = true;
            config.enabled = true;
            config.showAboveHead = true;
            config.showInTabList = true;
            config.showIcons = true;
            config.displayMode = "HIGHEST";
        });
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            context.getInput().holdKey(options -> options.playerListKey);
            context.waitTicks(20);
            Path tab = context.takeScreenshot("tier_tag_tablist");
            System.out.println("[TierTaggerGameTest] screenshot: " + tab);
            context.getInput().releaseKey(options -> options.playerListKey);
            context.waitTicks(5);
        }
    }
}
