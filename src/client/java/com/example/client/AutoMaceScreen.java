package com.example.automace;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class AutoMaceScreen extends Screen {

    public AutoMaceScreen() {
        super(Text.literal("AutoMace ClickGUI"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("AutoMace: " + (AutoMaceLogic.enabled ? "§aATIVADO" : "§cDESATIVADO")),
            button -> {
                AutoMaceLogic.enabled = !AutoMaceLogic.enabled;
                button.setMessage(Text.literal("AutoMace: " + (AutoMaceLogic.enabled ? "§aATIVADO" : "§cDESATIVADO")));
            })
            .dimensions(centerX - 100, centerY - 25, 200, 20)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Stun Slam (Escudo): " + (AutoMaceLogic.stunSlam ? "§aON" : "§cOFF")),
            button -> {
                AutoMaceLogic.stunSlam = !AutoMaceLogic.stunSlam;
                button.setMessage(Text.literal("Stun Slam (Escudo): " + (AutoMaceLogic.stunSlam ? "§aON" : "§cOFF")));
            })
            .dimensions(centerX - 100, centerY + 5, 200, 20)
            .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}