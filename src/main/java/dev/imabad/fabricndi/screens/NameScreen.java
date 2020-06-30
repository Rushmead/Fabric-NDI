package dev.imabad.fabricndi.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.imabad.fabricndi.CameraEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

public class NameScreen extends Screen {

    private CameraEntity cameraEntity;
    private TextFieldWidget nameField;

    public NameScreen(CameraEntity cameraEntity) {
        super(new LiteralText("Edit Camera"));
        this.cameraEntity = cameraEntity;
    }

    protected void init() {
        super.init();
        this.client.keyboard.enableRepeatEvents(true);
        int i = this.width / 2;
        int j = this.height / 2;
        this.nameField = new TextFieldWidget(this.textRenderer, i - 75, j - 10, 150, 20, new TranslatableText("container.repair"));
        this.nameField.setText(cameraEntity.getDisplayName().asString());
        this.nameField.setSelected(true);
        this.nameField.setFocusUnlocked(false);
        this.nameField.changeFocus(true);
        this.nameField.setMaxLength(35);
        this.children.add(this.nameField);
        this.setInitialFocus(this.nameField);
        this.addButton(new ButtonWidget(i - 20, j + 20, 40, 20, new LiteralText("Delete"), this::buttonClick));
    }

    public void buttonClick(ButtonWidget buttonWidget){
        this.cameraEntity.remove();
        this.client.player.closeScreen();
    }

    public void resize(MinecraftClient client, int width, int height) {
        String string = this.nameField.getText();
        this.init(client, width, height);
        this.nameField.setText(string);
    }

    public void removed() {
        super.removed();
        this.client.keyboard.enableRepeatEvents(false);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (!this.nameField.getText().isEmpty()) {
                String string = this.nameField.getText();
                cameraEntity.setName(string);
            }
            this.client.player.closeScreen();
        }

        return !this.nameField.keyPressed(keyCode, scanCode, modifiers) && !this.nameField.isActive() ? super.keyPressed(keyCode, scanCode, modifiers) : true;
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, delta);
        RenderSystem.disableBlend();
        this.nameField.render(matrixStack, mouseX, mouseY, delta);
        this.textRenderer.draw(matrixStack, this.title.asString(), (this.width / 2) - (this.textRenderer.getWidth(this.title.getString()) / 2), (this.height / 2) - 30, 0xffffff);
    }


}
