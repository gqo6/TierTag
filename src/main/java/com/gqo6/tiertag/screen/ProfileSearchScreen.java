package com.gqo6.tiertag.screen;

import com.gqo6.tiertag.TierTagMod;
import com.gqo6.tiertag.data.Kit;
import com.gqo6.tiertag.data.PlayerTiers;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ProfileSearchScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget searchField;
    private final List<Text> resultLines = new ArrayList<>();
    private boolean loading;

    protected ProfileSearchScreen(Screen parent) {
        super(Text.translatable("tiertag.search.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        this.searchField = new TextFieldWidget(this.textRenderer, centerX - 100, 30, 200, 20,
                Text.translatable("tiertag.search.field"));
        this.searchField.setMaxLength(16);
        this.searchField.setChangedListener(text -> { });
        this.addSelectableChild(this.searchField);
        this.addDrawableChild(this.searchField);
        this.setInitialFocus(this.searchField);
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tiertag.search.lookup"),
                        button -> runLookup())
                .dimensions(centerX - 100, 54, 200, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tiertag.config.done"), button -> this.close())
                .dimensions(centerX - 100, this.height - 28, 200, 20).build());
    }

    private void runLookup() {
        String username = this.searchField.getText().trim();
        if (username.isEmpty() || TierTagMod.service() == null) {
            return;
        }
        loading = true;
        resultLines.clear();
        resultLines.add(Text.literal("Looking up " + username + " on MCPVP...").formatted(Formatting.GRAY));
        CompletableFuture<PlayerTiers> future = TierTagMod.service().lookup(username);
        future.whenComplete((tiers, error) -> this.client.execute(() -> showResult(username, tiers, error)));
    }

    private void showResult(String username, PlayerTiers tiers, Throwable error) {
        loading = false;
        resultLines.clear();
        if (error != null) {
            resultLines.add(Text.literal("Lookup failed: " + error.getMessage()).formatted(Formatting.RED));
            return;
        }
        if (tiers == null || !tiers.found()) {
            resultLines.add(Text.literal("No verified MCPVP tiers for " + username).formatted(Formatting.YELLOW));
            return;
        }
        resultLines.add(Text.literal(tiers.username()).formatted(Formatting.AQUA)
                .append(Text.literal(" — overall: ").formatted(Formatting.WHITE))
                .append(Text.literal(tiers.overallTierCode() == null ? "unranked" : tiers.overallTierCode())
                        .formatted(Formatting.GOLD)));
        resultLines.add(Text.literal(""));
        for (Map.Entry<String, String> entry : tiers.kitTiers().entrySet()) {
            resultLines.add(Text.literal("  " + Kit.displayNameFor(entry.getKey())).formatted(Formatting.GRAY)
                    .append(Text.literal("  " + entry.getValue()).formatted(Formatting.WHITE)));
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (input.key() == 257 && this.searchField != null && this.searchField.isFocused()) {
            runLookup();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        int y = 84;
        for (Text line : resultLines) {
            context.drawTextWithShadow(this.textRenderer, line, this.width / 2 - 100, y, 0xFFFFFF);
            y += 12;
            if (y > this.height - 40) {
                break;
            }
        }
        if (loading && resultLines.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("..."), this.width / 2 - 100, 84, 0xAAAAAA);
        }
    }
}
