package com.gqo6.tiertag.screen;

import com.gqo6.tiertag.TierTagMod;
import com.gqo6.tiertag.config.TierConfig;
import com.gqo6.tiertag.data.Kit;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

public class TierConfigScreen extends Screen {
    private final Screen parent;
    private final TierConfig config;

    public TierConfigScreen(Screen parent) {
        super(Text.translatable("tiertag.config.title"));
        this.parent = parent;
        this.config = TierTagMod.config();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 30;
        int columnWidth = 155;
        int leftX = centerX - columnWidth - 5;
        int rightX = centerX + 5;

        this.addToggle(leftX, y, columnWidth, "enabled", config.enabled, value -> config.enabled = value);
        this.addToggle(rightX, y, columnWidth, "icons", config.showIcons, value -> config.showIcons = value);
        y += 24;
        this.addToggle(leftX, y, columnWidth, "above_head", config.showAboveHead, value -> config.showAboveHead = value);
        this.addToggle(rightX, y, columnWidth, "tab_list", config.showInTabList, value -> config.showInTabList = value);
        y += 28;

        List<String> modes = new ArrayList<>();
        modes.add("HIGHEST");
        for (Kit kit : Kit.values()) {
            modes.add(kit.slug());
        }
        String initialMode = config.displayMode == null ? "HIGHEST" : config.displayMode;
        this.addDrawableChild(CyclingButtonWidget.<String>builder(TierConfigScreen::modeLabel, initialMode)
                .values(modes)
                .build(leftX, y, 2 * columnWidth + 10, 20,
                        Text.translatable("tiertag.config.mode"),
                        (button, value) -> config.displayMode = value));
        y += 24;

        this.addSlider(new ConfigSlider(leftX, y, columnWidth, "max_distance", 8, 128,
                config.maxRenderDistance, value -> config.maxRenderDistance = (int) value, true));
        y += 24;
        this.addSlider(new ConfigSlider(leftX, y, columnWidth, "cache_seconds", 60, 3600,
                config.cacheDurationSeconds, value -> config.cacheDurationSeconds = (int) value, true));
        this.addSlider(new ConfigSlider(rightX, y, columnWidth, "request_interval_ms", 0, 2000,
                config.requestIntervalMs, value -> config.requestIntervalMs = (int) value, true));
        y += 28;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tiertag.config.tier_colors"),
                        button -> this.client.setScreen(new TierColorsScreen(this, config)))
                .dimensions(leftX, y, columnWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tiertag.config.profile_search"),
                        button -> this.client.setScreen(new ProfileSearchScreen(this)))
                .dimensions(rightX, y, columnWidth, 20).build());
        y += 28;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("tiertag.config.done"), button -> this.close())
                .dimensions(centerX - 100, this.height - 28, 200, 20).build());
    }

    private void addToggle(int x, int y, int width, String key, boolean initial,
                           Consumer<Boolean> setter) {
        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(initial)
                .build(x, y, width, 20,
                        Text.translatable("tiertag.config." + key),
                        (button, value) -> setter.accept(value)));
    }

    private void addSlider(ConfigSlider slider) {
        this.addDrawableChild(slider);
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
    }

    private static Text modeLabel(String mode) {
        if ("HIGHEST".equalsIgnoreCase(mode)) {
            return Text.literal("Highest Tier");
        }
        return Text.literal(Kit.displayNameFor(mode));
    }

    private static final class ConfigSlider extends SliderWidget {
        private final String labelKey;
        private final double min;
        private final double max;
        private final DoubleConsumer setter;
        private final boolean integer;

        ConfigSlider(int x, int y, int width, String labelKey, double min, double max, double initial,
                     DoubleConsumer setter, boolean integer) {
            super(x, y, width, 20, Text.empty(), Math.min(1.0, Math.max(0.0, (initial - min) / (max - min))));
            this.labelKey = labelKey;
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.integer = integer;
            updateMessage();
        }

        private double currentValue() {
            return min + this.value * (max - min);
        }

        @Override
        protected void updateMessage() {
            double value = currentValue();
            String formatted = integer ? String.valueOf((int) Math.round(value)) : String.format("%.2f", value);
            this.setMessage(Text.translatable("tiertag.config." + labelKey).append(": " + formatted));
        }

        @Override
        protected void applyValue() {
            setter.accept(integer ? Math.round(currentValue()) : currentValue());
        }
    }
}
