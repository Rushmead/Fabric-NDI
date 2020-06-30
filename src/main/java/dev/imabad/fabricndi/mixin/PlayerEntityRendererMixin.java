package dev.imabad.fabricndi.mixin;

import dev.imabad.fabricndi.CameraEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    public PlayerEntityRendererMixin(EntityRenderDispatcher dispatcher, PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowSize) {
        super(dispatcher, model, shadowSize);
    }

    @Inject(method= "setModelPose(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)V", at=@At("HEAD"), cancellable = true)
    public void setModelPose(AbstractClientPlayerEntity abstractClientPlayerEntity, CallbackInfo callbackInfo){
        PlayerEntityModel<AbstractClientPlayerEntity> playerEntityModel = this.getModel();
        if(abstractClientPlayerEntity instanceof CameraEntity){
            playerEntityModel.setVisible(false);
            playerEntityModel.head.visible = true;
            playerEntityModel.isSneaking = false;
            playerEntityModel.helmet.visible = true;
            callbackInfo.cancel();
        }
    }

    @Inject(method= "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at=@At("HEAD"))
    public void renderLabelIfPresent(AbstractClientPlayerEntity abstractClientPlayerEntity, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo callbackInfo){
        if(abstractClientPlayerEntity instanceof CameraEntity) {
            CameraEntity cameraEntity = (CameraEntity) abstractClientPlayerEntity;
            if(cameraEntity.isLive()){
                matrixStack.push();
                matrixStack.translate(0, 0.2f, 0);
                super.renderLabelIfPresent(abstractClientPlayerEntity, new LiteralText(Formatting.RED + "● LIVE ●"), matrixStack, vertexConsumerProvider, i);
                matrixStack.pop();
            }
        }
    }
}
