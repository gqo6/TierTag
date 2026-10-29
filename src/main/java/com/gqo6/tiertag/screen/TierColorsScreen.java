package com.gqo6.tiertag.screen;

import com.gqo6.tiertag.config.TierConfig;
import com.gqo6.tiertag.data.Tier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Locale;

public class TierColorsScreen extends Screen {
    private static final int[] PALETTE = {
            0xFFDA55, 0xFFFFFF, 0xFF5555, 0x55FF55, 0x55FFFF, 0xFF55FF,
            0xFFAA00, 0xAAAAAA, 0x5555FF, 0x9EB3D1, 0xEB9138, 0x9FD27F
    };

    private final Screen parent;
    private final TierConfig config;

    protected TierColorsScreen(Screen parent, TierConfig config) {
        super(Text.translatable("tiertag.colors.title"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int columnWidth = 150;
        int leftX = centerX - columnWidth - 6;
        int rightX = centerX + 6;
        Tier[] tiers = Tier.values();
        for (int i = 0; i < tiers.length; i++) {
            Tier tier = tiers[i];
            int column = i / 10;
            int row = i % 10;
            int x = column == 0 ? leftX : rightX;
            int y = 30 + row * 22;
            if (y > this.height - 56) {
                break;
            }
            this.addDrawableChild(ButtonWidget.builder(Text.literal(tier.name()), button -> cycleColor(tier))
                    .dimensions(x, y, columnWidth, 20).build());
        }
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tiertag.config.reset_defaults"), button -> {
            config.tierColors.clear();
            config.tierColors.putAll(TierConfig.defaultTierColors());
            this.client.setScreen(null);
            this.client.setScreen(this);
        }).dimensions(centerX - 154, this.height - 28, 150, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tiertag.config.done"), button -> this.close())
                .dimensions(centerX + 4, this.height - 28, 150, 20).build());
    }

    private void cycleColor(Tier tier) {
        int current = config.colorFor(tier);
        int index = 0;
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i] == current) {
                index = i;
                break;
            }
        }
        int next = PALETTE[(index + 1) % PALETTE.length];
        config.tierColors.put(tier.name(), String.format("#%06X", next).toUpperCase(Locale.ROOT));
        this.client.setScreen(null);
        this.client.setScreen(this);
    }

    @Override
    public void close() {
        config.save();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        for (Tier tier : Tier.values()) {
            context.fill(0, 0, 0, 0, 0);
        }
        int centerX = this.width / 2;
        for (int i = 0; i < Tier.values().length; i++) {
            Tier tier = Tier.values()[i];
            int column = i / 10;
            int row = i % 10;
            int x = column == 0 ? centerX - 156 : centerX + 6;
            int y = 30 + row * 22;
            if (y > this.height - 56) {
                break;
            }
            context.fill(x + 3, y + 16, x + 13, y + 18, config.colorFor(tier));
        }
    }
}
