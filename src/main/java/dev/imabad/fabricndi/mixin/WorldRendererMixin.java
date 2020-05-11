package dev.imabad.fabricndi.mixin;

import dev.imabad.fabricndi.CameraEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Shadow @Final protected MinecraftClient client;

    @Inject(method= "canDrawEntityOutlines", at =@At("HEAD"), cancellable=true)
    private void canDrawEntityOutlines(CallbackInfoReturnable<Boolean> cr){
        if(client.getCameraEntity() instanceof CameraEntity){
            cr.setReturnValue(false);
            cr.cancel();
        }
    }

    @Redirect(method="render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;getFocusedEntity()Lnet/minecraft/entity/Entity;", ordinal = 3))
    private Entity getFocusedEntity(Camera camera){
        if(client.getCameraEntity() instanceof CameraEntity){
            return client.player;
        }
        return camera.getFocusedEntity();
    }

}
