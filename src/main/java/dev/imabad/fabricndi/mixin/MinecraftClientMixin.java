package dev.imabad.fabricndi.mixin;

import dev.imabad.fabricndi.FabricNDI;
import dev.imabad.fabricndi.MinecraftClientExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements MinecraftClientExt {

    @Mutable
    @Shadow
    @Final
    private Framebuffer framebuffer;
    @Shadow @Final private Window window;

    @Shadow public abstract float getTickDelta();

    @Shadow public ClientPlayerEntity player;

    @Inject(method = "render(Z)V", at=@At("RETURN"))
    public void render(boolean tick, CallbackInfo info) {
        FabricNDI.instance.getGameRenderHook().render(framebuffer, window, player, getTickDelta());
    }

    @Override
    public void setFramebuffer(Framebuffer fb) {
        framebuffer = fb;
    }
}
