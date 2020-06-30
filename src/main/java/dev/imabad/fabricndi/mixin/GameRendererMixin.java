package dev.imabad.fabricndi.mixin;

import dev.imabad.fabricndi.CameraEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow private float viewDistance;

    @Inject(method = "getBasicProjectionMatrix(Lnet/minecraft/client/render/Camera;FZ)Lnet/minecraft/util/math/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void getProjectionMatrix(Camera camera, float f, boolean bl, CallbackInfoReturnable<Matrix4f> cir) {
        if(!(client.getCameraEntity() instanceof CameraEntity)) return;

        CameraEntity cameraEntity = (CameraEntity) client.getCameraEntity();

        MatrixStack matrixStack = new MatrixStack();
        matrixStack.peek().getModel().loadIdentity();

        matrixStack.peek().getModel().multiply(Matrix4f.viewboxMatrix(70.0f - (cameraEntity.getZoom()), 16 / 9f, 0.05f, viewDistance * 4.0f));
        cir.setReturnValue(matrixStack.peek().getModel());
    }

}
