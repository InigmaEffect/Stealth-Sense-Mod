package com.stealthsense.client;

import com.stealthsense.config.MobDetectionProfile;
import com.stealthsense.config.StealthConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MobTunerScreen extends Screen {
    private final Screen parent;
    private EditBox searchBox;
    private List<MobDetectionProfile> filteredProfiles = new ArrayList<>();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public MobTunerScreen(Screen parent) {
        super(Component.literal("StealthSense - Mob Detection Tuner (#)"));
        this.parent = parent;
        refreshFilter("");
    }

    private void refreshFilter(String query) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        filteredProfiles.clear();
        for (MobDetectionProfile p : StealthConfig.INSTANCE.profiles.values()) {
            if (q.isEmpty() || p.displayName.toLowerCase(Locale.ROOT).contains(q) || p.id.contains(q)) {
                filteredProfiles.add(p);
            }
        }
        selectedIndex = Mth.clamp(selectedIndex, 0, Math.max(0, filteredProfiles.size() - 1));
        scrollOffset = 0;
    }

    @Override
    protected void init() {
        int leftWidth = 175;
        int rightX = 195;
        int panelWidth = Math.max(210, this.width - rightX - 16);

        this.searchBox = new EditBox(this.font, 12, 26, leftWidth, 18, Component.literal("Search Mobs"));
        this.searchBox.setHint(Component.literal("Filter mobs..."));
        this.searchBox.setResponder(q -> {
            refreshFilter(q);
            rebuildWidgets();
        });
        this.addRenderableWidget(this.searchBox);

        int listStartY = 50;
        int rowHeight = 20;
        int maxRows = Math.max(4, (this.height - 85) / rowHeight);

        for (int i = 0; i < maxRows; i++) {
            int idx = scrollOffset + i;
            if (idx >= filteredProfiles.size()) break;
            MobDetectionProfile prof = filteredProfiles.get(idx);
            boolean isSelected = (idx == selectedIndex);
            String label = (isSelected ? "§a> " : "§7") + prof.displayName;
            final int capturedIdx = idx;
            this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                selectedIndex = capturedIdx;
                rebuildWidgets();
            }).bounds(12, listStartY + i * rowHeight, leftWidth, 18).build());
        }

        this.addRenderableWidget(Button.builder(Component.literal("▲"), btn -> {
            if (scrollOffset > 0) {
                scrollOffset--;
                rebuildWidgets();
            }
        }).bounds(12, this.height - 30, 84, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("▼"), btn -> {
            if (scrollOffset + maxRows < filteredProfiles.size()) {
                scrollOffset++;
                rebuildWidgets();
            }
        }).bounds(102, this.height - 30, 85, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Mod: " + (StealthConfig.INSTANCE.modEnabled ? "§aON" : "§cOFF")),
                btn -> {
                    StealthConfig.INSTANCE.modEnabled = !StealthConfig.INSTANCE.modEnabled;
                    rebuildWidgets();
                }).bounds(rightX, 12, 85, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Debug HUD: " + (StealthConfig.INSTANCE.showDebugActionbar ? "§aON" : "§7OFF")),
                btn -> {
                    StealthConfig.INSTANCE.showDebugActionbar = !StealthConfig.INSTANCE.showDebugActionbar;
                    rebuildWidgets();
                }).bounds(rightX + 90, 12, 110, 20).build());

        if (!filteredProfiles.isEmpty()) {
            MobDetectionProfile p = filteredProfiles.get(selectedIndex);
            int y = 44;
            int step = 24;

            this.addRenderableWidget(new ParamSlider(rightX, y, panelWidth, 20,
                    "Sight Distance", p.maxRange, 4.0, 128.0, " blocks",
                    val -> p.maxRange = Math.round(val)));
            y += step;

            this.addRenderableWidget(new ParamSlider(rightX, y, panelWidth, 20,
                    "FOV Cone", p.fovDegrees, 20.0, 360.0, "°",
                    val -> p.fovDegrees = Math.round(val)));
            y += step;

            this.addRenderableWidget(new ParamSlider(rightX, y, panelWidth, 20,
                    "Clarity Required", p.clarityThreshold, 0.0, 0.95, "",
                    val -> p.clarityThreshold = Math.round(val * 100.0) / 100.0));
            y += step;

            this.addRenderableWidget(new ParamSlider(rightX, y, panelWidth, 20,
                    "Speed-Blur Factor", p.speedBlurFactor, 0.0, 2.5, "x",
                    val -> p.speedBlurFactor = Math.round(val * 100.0) / 100.0));
            y += step;

            this.addRenderableWidget(new ParamSlider(rightX, y, panelWidth, 20,
                    "Sneak Visibility", p.sneakMultiplier, 0.05, 1.0, "x",
                    val -> p.sneakMultiplier = Math.round(val * 100.0) / 100.0));
            y += step;

            this.addRenderableWidget(new ParamSlider(rightX, y, panelWidth, 20,
                    "Intelligence", p.intelligence, 0.0, 1.0, "",
                    val -> p.intelligence = Math.round(val * 100.0) / 100.0));
            y += step;

            this.addRenderableWidget(new ParamSlider(rightX, y, panelWidth, 20,
                    "Alert Memory", p.memoryTicks, 20.0, 600.0, " ticks",
                    val -> p.memoryTicks = (int) Math.round(val)));
        }

        this.addRenderableWidget(Button.builder(Component.literal("§aSave & Close"), btn -> {
            StealthConfig.save();
            this.onClose();
        }).bounds(rightX, this.height - 30, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("§eReset Defaults"), btn -> {
            StealthConfig.resetDefaults();
            refreshFilter(searchBox.getValue());
            rebuildWidgets();
        }).bounds(rightX + 106, this.height - 30, 100, 20).build());
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if (event.codepoint() == '#' && !this.searchBox.isFocused()) {
            StealthConfig.save();
            this.onClose();
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawString(this.font, "§lStealthSense Tuner", 12, 10, 0xFFFFFF);
        if (!filteredProfiles.isEmpty()) {
            MobDetectionProfile active = filteredProfiles.get(selectedIndex);
            graphics.drawString(this.font, "Editing: §b" + active.displayName + " §8(" + active.id + ")", 195, 34, 0xDDDDDD);
        }
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        StealthConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private interface DoubleConsumer {
        void accept(double v);
    }

    private static class ParamSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final String suffix;
        private final DoubleConsumer onChange;

        public ParamSlider(int x, int y, int w, int h, String label, double current,
                           double min, double max, String suffix, DoubleConsumer onChange) {
            super(x, y, w, h, Component.empty(), Mth.clamp((current - min) / (max - min), 0.0, 1.0));
            this.label = label;
            this.min = min;
            this.max = max;
            this.suffix = suffix;
            this.onChange = onChange;
            updateMessage();
        }

        private double getScaledValue() {
            return min + this.value * (max - min);
        }

        @Override
        protected void updateMessage() {
            double v = getScaledValue();
            String formatted = (max - min >= 10)
                    ? String.format(Locale.ROOT, "%.0f", v)
                    : String.format(Locale.ROOT, "%.2f", v);
            setMessage(Component.literal(label + ": §e" + formatted + suffix));
        }

        @Override
        protected void applyValue() {
            onChange.accept(getScaledValue());
        }
    }
}
