package dev.imabad.fabricndi.mixin;

import dev.imabad.fabricndi.FabricNDI;
import dev.imabad.fabricndi.MinecraftClientExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements MinecraftClientExt {

    @Mutable
    @Shadow
    @Final
    private Framebuffer framebuffer;
    @Shadow @Final private Window window;

    @Shadow public abstract float getTickDelta();

    @Shadow public ClientPlayerEntity player;

    @Shadow @Final public File runDirectory;

    @Shadow private boolean isIntegratedServerRunning;

    @Shadow private IntegratedServer server;

    @Shadow @Final private LevelStorage levelStorage;

    @Shadow public abstract ClientPlayNetworkHandler getNetworkHandler();

    @Inject(method = "render(Z)V", at=@At("RETURN"))
    public void render(boolean tick, CallbackInfo info) {
        FabricNDI.instance.getGameRenderHook().render(framebuffer, window, player, getTickDelta());
    }

    @Override
    public void setFramebuffer(Framebuffer fb) {
        framebuffer = fb;
    }

    @Inject(method= "joinWorld(Lnet/minecraft/client/world/ClientWorld;)V", at=@At("RETURN"))
    public void joinWorld(ClientWorld clientWorld, CallbackInfo callbackInfo){
        FabricNDI.instance.getCameraManager().load(runDirectory, isIntegratedServerRunning, server, levelStorage, clientWorld, this.getNetworkHandler());
    }

    @Inject(method= "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at=@At("HEAD"))
    public void disconnect(Screen screen, CallbackInfo ci){
        FabricNDI.instance.getCameraManager().save(runDirectory, player, isIntegratedServerRunning, levelStorage, server);
    }
}
