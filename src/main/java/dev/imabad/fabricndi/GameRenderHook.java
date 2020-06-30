package dev.imabad.fabricndi;

import com.mojang.blaze3d.systems.RenderSystem;
import com.walker.devolay.DevolayMetadataFrame;
import com.walker.devolay.DevolaySender;
import dev.imabad.fabricndi.threads.NDIControlThread;
import dev.imabad.fabricndi.threads.NDIThread;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameRenderHook {

    private NDIThread mainOutput;
    private final DevolaySender mainSender;

    private final ConcurrentHashMap<UUID, PBOManager> entityBuffers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Framebuffer> entityFramebuffers = new ConcurrentHashMap<>();
    private PBOManager pboManager;

    public GameRenderHook(String senderName){
        mainSender = new DevolaySender(senderName);
    }

    public void render(Framebuffer framebuffer, Window window, PlayerEntity player, float tickDelta, boolean isPaused){
        boolean hasResChanged = false;
        if(mainOutput == null){
            pboManager = new PBOManager(window.getWidth(), window.getHeight());
            pboManager.readPixelData(framebuffer);
            mainOutput = new NDIThread(mainSender, pboManager.buffer, window.getWidth(), window.getHeight());
            mainOutput.start();
        } else if (mainOutput.getNeedsFrame().get()) {
            if (mainOutput.width.get() != window.getWidth() || mainOutput.height.get() != window.getHeight()) {
                pboManager.cleanUp();
                pboManager = new PBOManager(window.getWidth(), window.getHeight());
                mainOutput.updateVideoFrame(window.getWidth(), window.getHeight());
                hasResChanged = true;
            }
            if(mainOutput.sender.get().getConnectionCount(0) > 0){
                pboManager.readPixelData(framebuffer);
                mainOutput.setByteBuffer(pboManager.buffer);
            }
        }
        if(player != null && !isPaused){
            List<CameraEntity> needFrames = new ArrayList<>();
            for(CameraEntity e : FabricNDI.instance.getCameraManager().cameraEntities){
                PBOManager pboManager;
                if(!entityBuffers.containsKey(e.getUuid())){
                    pboManager = new PBOManager(window.getWidth(), window.getHeight());
                    entityBuffers.put(e.getUuid(), pboManager);
                } else {
                    pboManager = entityBuffers.get(e.getUuid());
                    if(hasResChanged){
                        pboManager.cleanUp();
                        pboManager = new PBOManager(window.getWidth(), window.getHeight());
                        entityBuffers.put(e.getUuid(), pboManager);
                    }
                }
                NDIThread ndiThread;
                if(!FabricNDI.instance.getCameraManager().cameras.containsKey(e.getUuid())){
                    DevolaySender sender = new DevolaySender("MC - " + e.getDisplayName().getString());
                    DevolayMetadataFrame metadataFrame = new DevolayMetadataFrame();
                    metadataFrame.setData("<ndi_capabilities ntk_ptz=\"true\"/>");
                    sender.addConnectionMetadata(metadataFrame);
                    ndiThread = new NDIThread(sender, pboManager.buffer, window.getWidth(), window.getHeight());
                    FabricNDI.instance.getCameraManager().cameras.put(e.getUuid(), ndiThread);
                    NDIControlThread ndiControlThread = new NDIControlThread(sender, e);
                    FabricNDI.instance.getCameraManager().cameraControls.put(e.getUuid(), ndiControlThread);
                    ndiThread.start();
                    ndiControlThread.start();
                } else {
                    ndiThread = FabricNDI.instance.getCameraManager().cameras.get(e.getUuid());
                    if(hasResChanged){
                        ndiThread.updateVideoFrame(window.getWidth(), window.getHeight());
                    }
                }
                if(e.isAlive() && ndiThread.getNeedsFrame().get() && ndiThread.sender.get().getConnectionCount(0) > 0) {
                    needFrames.add(e);
                }
            }
            if(needFrames.size() > 0){
                MinecraftClient minecraftClient = MinecraftClient.getInstance();
                boolean oldHudHidden = minecraftClient.options.hudHidden;
                Entity oldCam = minecraftClient.cameraEntity;
                int oldPerspective = minecraftClient.options.perspective;
                minecraftClient.options.hudHidden = true;
                minecraftClient.options.perspective = 0;
                Framebuffer oldWindow = minecraftClient.getFramebuffer();
                float prevCameraY = CameraExt.from(minecraftClient.gameRenderer.getCamera()).getCameraY();

                for(Entity e : needFrames){
                    if(e == null || !e.isAlive()){
                        continue;
                    }
                    Framebuffer entityFramebuffer;
                    if(!entityFramebuffers.containsKey(e.getUuid())){
                        entityFramebuffer = new Framebuffer(window.getWidth(), window.getHeight(), true, MinecraftClient.IS_SYSTEM_MAC);;
                        entityFramebuffers.put(e.getUuid(), entityFramebuffer);
                    } else {
                        entityFramebuffer = entityFramebuffers.get(e.getUuid());
                        if(hasResChanged){
                            entityFramebuffer.resize(window.getWidth(), window.getHeight(), MinecraftClient.IS_SYSTEM_MAC);
                        }
                    }
                    entityFramebuffer.beginWrite(true);
                    GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
                    MinecraftClientExt.from(minecraftClient).setFramebuffer(entityFramebuffer);

                    GL11.glMatrixMode(GL11.GL_PROJECTION);
                    RenderSystem.pushMatrix();
                    RenderSystem.loadIdentity();
                    GL11.glMatrixMode(GL11.GL_MODELVIEW);
                    RenderSystem.pushMatrix();
                    RenderSystem.loadIdentity();

                    PBOManager entityBytes = entityBuffers.get(e.getUuid());;
                    minecraftClient.cameraEntity = e;
                    CameraExt.from(minecraftClient.gameRenderer.getCamera()).setCameraY(e.getStandingEyeHeight());
//                    minecraftClient.gameRenderer.getCamera().updateEyeHeight();
                    minecraftClient.gameRenderer.renderWorld(tickDelta, Util.getMeasuringTimeNano(), new MatrixStack());
//                    minecraftClient.gameRenderer.getCamera().updateEyeHeight();
                    entityBytes.readPixelData(entityFramebuffer);
                    FabricNDI.instance.getCameraManager().cameras.get(e.getUuid()).setByteBuffer(entityBytes.buffer);
                    CameraExt.from(minecraftClient.gameRenderer.getCamera()).setCameraY(prevCameraY);

                    GL11.glMatrixMode(GL11.GL_PROJECTION);
                    RenderSystem.popMatrix();
                    GL11.glMatrixMode(GL11.GL_MODELVIEW);
                    RenderSystem.popMatrix();

                }
                MinecraftClientExt.from(minecraftClient).setFramebuffer(oldWindow);
                oldWindow.beginWrite(true);

                minecraftClient.cameraEntity = oldCam;
//                minecraftClient.gameRenderer.getCamera().updateEyeHeight();
                minecraftClient.options.hudHidden = oldHudHidden;
                minecraftClient.options.perspective = oldPerspective;
            }
        }
    }

}
