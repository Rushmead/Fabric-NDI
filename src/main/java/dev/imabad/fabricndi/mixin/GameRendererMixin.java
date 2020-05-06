package dev.imabad.fabricndi.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.walker.devolay.DevolaySender;
import dev.imabad.fabricndi.FabricNDI;
import dev.imabad.fabricndi.MinecraftClientExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.UUID;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow @Final private MinecraftClient client;


    @Shadow private float viewDistance;
    private float lastTickDelta;

    @Inject(method = "renderWorld(FJLnet/minecraft/client/util/math/MatrixStack;)V", at = @At("HEAD"))
    private void saveTickDelta(float tickDelta, long limitTime, MatrixStack matrix, CallbackInfo ci) {
        lastTickDelta = tickDelta;
    }

    @Inject(method="updateTargetedEntity", at=@At("HEAD"), cancellable = true)
    private void updateTargetedEntity(float tickDelta, CallbackInfo ci){
        if(client.getCameraEntity() instanceof OtherClientPlayerEntity){
            ci.cancel();
        }
    }

//    @ModifyVariable(method="renderWorld", at=@At(value= "STORE"), ordinal = 0, name="camera")
//    public Camera getCamera(Camera cam){
//        if(client.getCameraEntity() instanceof OtherClientPlayerEntity){
//            Camera camera = new Camera();
//            camera.update(client.world, client.getCameraEntity() == null ? client.player : client.getCameraEntity(), false, false, lastTickDelta);
//            return camera;
//        }
//        return cam;
//    }
    @Inject(method = "bobView(Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At("HEAD"), cancellable = true)
    private void bobView(MatrixStack matrixStack, float f, CallbackInfo ci) {
        if(client.getCameraEntity() instanceof OtherClientPlayerEntity) ci.cancel();
    }

    @Inject(method = "bobViewWhenHurt(Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At("HEAD"), cancellable = true)
    private void bobViewWhenHurt(MatrixStack matrixStack, float f, CallbackInfo ci) {
        if(client.getCameraEntity() instanceof OtherClientPlayerEntity) ci.cancel();
    }

    @Inject(method = "renderHand(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/Camera;F)V", at = @At("HEAD"), cancellable = true)
    private void renderHand(MatrixStack matrixStack, Camera camera, float f, CallbackInfo ci) {
        if(client.getCameraEntity() instanceof OtherClientPlayerEntity) ci.cancel();
    }

    @Redirect(method = "renderWorld(FJLnet/minecraft/client/util/math/MatrixStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F", ordinal = 0))
    private float getNauseaStrength(float delta, float first, float second) {
        return client.getCameraEntity() instanceof OtherClientPlayerEntity ?  0f: MathHelper.lerp(delta, first, second);
    }

    @Inject(method = "method_22973(Lnet/minecraft/client/render/Camera;FZ)Lnet/minecraft/client/util/math/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void getProjectionMatrix(Camera camera, float f, boolean bl, CallbackInfoReturnable<Matrix4f> cir) {
        if(!(client.getCameraEntity() instanceof OtherClientPlayerEntity)) return;

        MatrixStack matrixStack = new MatrixStack();
        matrixStack.peek().getModel().loadIdentity();

        matrixStack.peek().getModel().multiply(Matrix4f.viewboxMatrix(70.0f, 16 / 9f, 0.05f, viewDistance * 4.0f));
        cir.setReturnValue(matrixStack.peek().getModel());
    }
}
