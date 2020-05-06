package dev.imabad.fabricndi.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.walker.devolay.DevolayMetadataFrame;
import com.walker.devolay.DevolaySender;
import dev.imabad.fabricndi.FabricNDI;
import dev.imabad.fabricndi.MinecraftClientExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements MinecraftClientExt {

    @Mutable
    @Shadow
    @Final
    private Framebuffer framebuffer;

    private ByteBuffer bytes;
    @Shadow @Final private Window window;

    @Shadow public abstract float getTickDelta();

    @Shadow @Final public GameRenderer gameRenderer;
    @Shadow public ClientPlayerEntity player;
    private ConcurrentHashMap<UUID, ByteBuffer> entityBuffers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<UUID, Framebuffer> entityFramebuffers = new ConcurrentHashMap<>();

    private Framebuffer entityFramebuffer;

    @Inject(method = "render(Z)V", at=@At("RETURN"))
    public void render(boolean tick, CallbackInfo info) {
        if(FabricNDI.instance.thread == null){
            if (bytes == null) {
                bytes = ByteBuffer.allocateDirect(window.getWidth() * window.getHeight() * 4);
            }
            RenderSystem.bindTexture(framebuffer.colorAttachment);
            GL11.glGetTexImage(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, bytes);
            FabricNDI.instance.thread = new FabricNDI.NDIThread(FabricNDI.instance.sender, bytes, window.getWidth(), window.getHeight());
            FabricNDI.instance.thread.start();
        } else if (FabricNDI.instance.thread.getNeedsFrame().get()) {
            if (bytes == null) {
                bytes = ByteBuffer.allocateDirect(window.getWidth() * window.getHeight() * 4);
            }
            if (FabricNDI.instance.thread.width.get() != window.getWidth() || FabricNDI.instance.thread.height.get() != window.getHeight()) {
                bytes = ByteBuffer.allocateDirect(window.getWidth() * window.getHeight() * 4);
                FabricNDI.instance.thread.updateVideoFrame(window.getWidth(), window.getHeight());
            }
            RenderSystem.bindTexture(framebuffer.colorAttachment);
            GL11.glGetTexImage(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, bytes);
            FabricNDI.instance.thread.setByteBuffer(bytes);
        }
        if(player != null){
            List<Entity> needFrames = new ArrayList<>();
            for(Entity e : FabricNDI.instance.cameraEntities){
                ByteBuffer entityBytes;
                if(!entityBuffers.containsKey(e.getUuid())){
                    entityBytes = ByteBuffer.allocateDirect(window.getWidth() * window.getHeight() * 4);
                    entityBuffers.put(e.getUuid(), entityBytes);
                } else {
                    entityBytes = entityBuffers.get(e.getUuid());
                }
                FabricNDI.NDIThread ndiThread;
                if(!FabricNDI.instance.cameras.containsKey(e.getUuid())){
                    DevolaySender sender = new DevolaySender(e.getUuidAsString());
                    DevolayMetadataFrame metadataFrame = new DevolayMetadataFrame();
                    metadataFrame.setData("<ndi_capabilities ntk_ptz=\"true\"/>");
                    sender.addConnectionMetadata(metadataFrame);
                    ndiThread = new FabricNDI.NDIThread(sender, entityBytes, MinecraftClient.getInstance().getWindow().getWidth(), MinecraftClient.getInstance().getWindow().getHeight());
                    FabricNDI.instance.cameras.put(e.getUuid(), ndiThread);
                    FabricNDI.NDICaptureThread ndiCaptureThread = new FabricNDI.NDICaptureThread(sender, e);
                    ndiThread.start();
                    ndiCaptureThread.start();
                } else {
                    ndiThread = FabricNDI.instance.cameras.get(e.getUuid());
                }
                if(ndiThread.getNeedsFrame().get()) {
                   needFrames.add(e);
                }
            }
            if(needFrames.size() > 0){
                MinecraftClient minecraftClient = MinecraftClient.getInstance();
                if(entityFramebuffer == null){
                    entityFramebuffer = new Framebuffer(minecraftClient.getWindow().getWidth(), minecraftClient.getWindow().getHeight(), true, MinecraftClient.IS_SYSTEM_MAC);;
                }
                boolean oldHudHidden = minecraftClient.options.hudHidden;
                Entity oldCam = minecraftClient.cameraEntity;
                int oldPerspective = minecraftClient.options.perspective;
                minecraftClient.options.hudHidden = true;
                minecraftClient.options.perspective = 0;
                Framebuffer window = minecraftClient.getFramebuffer();
                entityFramebuffer.beginWrite(true);
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
                MinecraftClientExt.from(minecraftClient).setFramebuffer(entityFramebuffer);

                GL11.glMatrixMode(GL11.GL_PROJECTION);
                RenderSystem.pushMatrix();
                RenderSystem.loadIdentity();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                RenderSystem.pushMatrix();
                RenderSystem.loadIdentity();

                for(Entity e : needFrames){
                    ByteBuffer entityBytes = entityBuffers.get(e.getUuid());;
                    minecraftClient.cameraEntity = e;
                    minecraftClient.gameRenderer.renderWorld(this.getTickDelta(), Util.getMeasuringTimeNano(), new MatrixStack());

                    RenderSystem.bindTexture(entityFramebuffer.colorAttachment);
                    GL11.glGetTexImage(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, entityBytes);
                    FabricNDI.instance.cameras.get(e.getUuid()).setByteBuffer(entityBytes);
                }

                GL11.glMatrixMode(GL11.GL_PROJECTION);
                RenderSystem.popMatrix();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                RenderSystem.popMatrix();

                MinecraftClientExt.from(minecraftClient).setFramebuffer(window);
                window.beginWrite(true);

                minecraftClient.cameraEntity = oldCam;
                minecraftClient.options.hudHidden = oldHudHidden;
                minecraftClient.options.perspective = oldPerspective;
            }
        }
    }

    @Override
    public void setFramebuffer(Framebuffer fb) {
        framebuffer = fb;
    }
}
