package dev.imabad.fabricndi;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

public interface MinecraftClientExt {

    void setFramebuffer(Framebuffer fb);

    static MinecraftClientExt from(MinecraftClient self){
        return (MinecraftClientExt)self;
    }

}
