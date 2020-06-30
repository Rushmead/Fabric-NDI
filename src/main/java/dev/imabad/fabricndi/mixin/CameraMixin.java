package dev.imabad.fabricndi.mixin;

import dev.imabad.fabricndi.CameraEntity;
import dev.imabad.fabricndi.CameraExt;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin implements CameraExt {
    @Shadow private Entity focusedEntity;

    @Shadow protected abstract void setPos(Vec3d pos);

    @Shadow private float cameraY;

    @Shadow private float lastCameraY;

    @Override
    public void setCameraY(float cameraY) {
        this.cameraY = cameraY;
        this.lastCameraY = cameraY;
    }

    @Override
    public float getCameraY() {
        return this.cameraY;
    }

    @Inject(method = "updateEyeHeight()V", at = @At("HEAD"), cancellable = true)
    public void updateEyeHeight(CallbackInfo ci) {
        if(this.focusedEntity != null && this.focusedEntity instanceof CameraEntity){
            ci.cancel();
        }
    }
}
