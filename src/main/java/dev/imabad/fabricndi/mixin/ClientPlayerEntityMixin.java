package dev.imabad.fabricndi.mixin;

import dev.imabad.fabricndi.CameraEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Shadow @Final protected MinecraftClient client;

    @Inject(method= "isMainPlayer()Z", at =@At("HEAD"), cancellable=true)
    private void isMainPlayer(CallbackInfoReturnable<Boolean> cr){
        if(client.getCameraEntity() instanceof CameraEntity){
            cr.setReturnValue(false);
            cr.cancel();
        }
    }

}
