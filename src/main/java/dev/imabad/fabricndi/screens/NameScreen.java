package dev.imabad.fabricndi.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.imabad.fabricndi.CameraEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;

public class NameScreen extends Screen {

    private CameraEntity cameraEntity;
    private TextFieldWidget nameField;

    public NameScreen(CameraEntity cameraEntity) {
        super(new LiteralText("Rename Camera"));
        this.cameraEntity = cameraEntity;
    }

    protected void init() {
        super.init();
        this.minecraft.keyboard.enableRepeatEvents(true);
        int i = this.width / 2;
        int j = this.height / 2;
        this.nameField = new TextFieldWidget(this.font, i - 51, j, 103, 12, I18n.translate("container.repair"));
        this.nameField.setText(cameraEntity.getDisplayName().asString());
        this.nameField.setFocusUnlocked(false);
        this.nameField.changeFocus(true);
        this.nameField.setMaxLength(35);
        this.children.add(this.nameField);
        this.setInitialFocus(this.nameField);
    }

    public void resize(MinecraftClient client, int width, int height) {
        String string = this.nameField.getText();
        this.init(client, width, height);
        this.nameField.setText(string);
    }

    public void removed() {
        super.removed();
        this.minecraft.keyboard.enableRepeatEvents(false);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (!this.nameField.getText().isEmpty()) {
                String string = this.nameField.getText();
                cameraEntity.setName(string);
            }
            this.minecraft.player.closeScreen();
        }

        return !this.nameField.keyPressed(keyCode, scanCode, modifiers) && !this.nameField.isActive() ? super.keyPressed(keyCode, scanCode, modifiers) : true;
    }

    protected void drawForeground(int mouseX, int mouseY) {
        RenderSystem.disableBlend();
        this.font.draw(this.title.asFormattedString(), 60.0F, 6.0F, 4210752);
    }

    private void onRenamed(String name) {

    }

    public void render(int mouseX, int mouseY, float delta) {
        this.renderBackground();
        super.render(mouseX, mouseY, delta);
        RenderSystem.disableBlend();
        this.nameField.render(mouseX, mouseY, delta);
    }

    protected void drawBackground(float delta, int mouseX, int mouseY) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
