package dev.imabad.fabricndi;

import com.mojang.authlib.GameProfile;
import com.walker.devolay.*;
import io.netty.util.internal.ConcurrentSet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class FabricNDI implements ModInitializer {

    public static FabricNDI instance;

    private static FabricKeyBinding keyBinding, killAll;
    public DevolaySender sender;
    public ConcurrentSet<Entity> cameraEntities;
    public ConcurrentHashMap<UUID, NDIThread> cameras;
    public NDIThread thread;

    @Override
    public void onInitialize() {
        Devolay.loadLibraries();
        instance = this;
        cameras = new ConcurrentHashMap<>();
        cameraEntities = new ConcurrentSet<>();
        System.out.println("Starting Fabric NDI");
        sender = new DevolaySender("Minecraft");
        keyBinding = FabricKeyBinding.Builder.create(new Identifier("fabricndi", "newcamera"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "NDI").build();
        killAll = FabricKeyBinding.Builder.create(new Identifier("fabricndi", "killall"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, "NDI").build();
        KeyBindingRegistry.INSTANCE.addCategory("NDI");
        KeyBindingRegistry.INSTANCE.register(keyBinding);
        KeyBindingRegistry.INSTANCE.register(killAll);
        ClientTickCallback.EVENT.register(e -> {
            if(keyBinding.isPressed() && e.world != null && e.player != null){
                UUID uuid = UUID.randomUUID();
                OtherClientPlayerEntity armorStandEntity = new OtherClientPlayerEntity(e.world, new GameProfile(uuid, uuid.toString()));
                armorStandEntity.refreshPositionAndAngles(e.player.getX(), e.player.getY(), e.player.getZ(), 0, 0);
                e.world.addEntity(armorStandEntity.getEntityId(), armorStandEntity);
                keyBinding.setPressed(false);
                FabricNDI.instance.cameraEntities.add(armorStandEntity);
            } else if(killAll.isPressed() && e.world != null && e.player != null){
                for(Entity ent :cameraEntities){
                    ent.kill();
                    cameras.remove(ent.getUuid());
                    cameraEntities.remove(ent);
                    e.world.removeEntity(ent.getEntityId());
                }
            }
        });
    }

    public static class NDICaptureThread extends Thread {

        public boolean running = true;
        private DevolaySender sender;
        private Entity entity;

        public NDICaptureThread(DevolaySender sender, Entity entity){
            this.sender = sender;
            this.entity = entity;
        }

        @Override
        public void interrupt() {
            super.interrupt();
            running = false;
        }

        @Override
        public void run() {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = null;
            try {
                db = documentBuilderFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            while(running){
                DevolayMetadataFrame metadataFrame = new DevolayMetadataFrame();
                if (sender.receiveCapture(metadataFrame, 1000) == DevolayFrameType.METADATA) {
                    try {
                        Document doc = db.parse(new InputSource(new StringReader(metadataFrame.getData())));
                        String type = doc.getFirstChild().getNodeName();
                        if(type.equals("ntk_ptz_pan_tilt_speed")){
                            Element element = (Element) doc.getFirstChild();
                            float panSpeed = Float.parseFloat(element.getAttribute("pan_speed"));
                            float tiltSpeed = Float.parseFloat(element.getAttribute("tilt_speed"));
                            System.out.println("Pan speed is " + panSpeed + " and tilt speed is " + tiltSpeed);
                            float tilt = 15 * tiltSpeed;
                            float pan = 15 * panSpeed;
                            System.out.println("Pan is " + pan + " and tilt is " + tilt);
                            float pitch = entity.pitch - tilt;
                            float yaw = entity.yaw - pan;
                            System.out.println("Yaw is " + yaw + " and pitch is " + pitch);
                            entity.setHeadYaw(yaw);
                            entity.updatePositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), yaw, pitch);
                        }
                    } catch (SAXException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static class NDIThread extends Thread {

        private DevolaySender sender;
        private AtomicReference<ByteBuffer> byteBuffer;
        private AtomicReference<DevolayVideoFrame> videoFrame;
        private AtomicBoolean needsFrame, hasFlipped;
        public AtomicInteger width, height;
        public boolean running = true;

        public NDIThread(DevolaySender sender, ByteBuffer image, int width, int height){
            this.sender = sender;
            byteBuffer = new AtomicReference<>(image);
            videoFrame = new AtomicReference<>();
            this.width = new AtomicInteger(width);
            this.height = new AtomicInteger(height);
            needsFrame = new AtomicBoolean(true);
            hasFlipped = new AtomicBoolean(false);
            DevolayVideoFrame videoFrame1 = new DevolayVideoFrame();
            videoFrame1.setResolution(width, height);
            videoFrame1.setFourCCType(DevolayFrameFourCCType.RGBA);
            videoFrame1.setLineStride(width * 4);
            videoFrame1.setFrameRate(30, 1);
            videoFrame.set(videoFrame1);
        }

        public void updateVideoFrame(int width, int height){
            DevolayVideoFrame videoFrame1 = new DevolayVideoFrame();
            videoFrame1.setResolution(width, height);
            videoFrame1.setFourCCType(DevolayFrameFourCCType.RGBA);
            videoFrame1.setLineStride(width * 4);
            videoFrame1.setFrameRate(30, 1);
            videoFrame.set(videoFrame1);
            this.width.set(width);
            this.height.set(height);
        }

        public void setByteBuffer(ByteBuffer buffer){
            this.byteBuffer.set(buffer);
            this.needsFrame.set(false);
            this.hasFlipped.set(false);
        }

        public AtomicBoolean getNeedsFrame(){
            return needsFrame;
        }

        @Override
        public void interrupt() {
            super.interrupt();
            this.running = false;
        }

        @Override
        public void run() {
            int frameCounter = 0;
            long fpsPeriod = System.currentTimeMillis();
            ByteBuffer output = ByteBuffer.allocateDirect(width.get() * height.get() * 4);
            // Run for one minute
            while(running) {
                if(needsFrame.get()){
                    continue;
                }
                DevolayVideoFrame frame = videoFrame.get();
                ByteBuffer buffer = byteBuffer.get();
                int width = this.width.get();
                int height = this.height.get();
                if(buffer.capacity() > output.capacity()){
                    output = ByteBuffer.allocateDirect(width * height * 4);
                }
                if(frame.getXResolution() != width || frame.getYResolution() != height){
                    updateVideoFrame(width, height);
                    frame = videoFrame.get();
                }
//                if(buffer.capacity() < width * height * 4){
//                    continue;
//                }
//                for(int y = 0; y < height; y++){
//                    for(int x = 0; x < width; x++){
//                        int pixelIndex = y * (width * 4) + (x * 4);
//                        for(int z = 0; z < 4; z++){
//                            byte original = buffer.get(pixelIndex + z);
//                            int index = (((height - y) * (width * 4)) - width * 4) + (x * 4) + z;
//                            if(z == 3){
//                                original = (byte) 255;
//                            }
//                            output.put(index, original);
//                        }
//                    }
//                }
                frame.setData(buffer);
                this.sender.sendVideoFrameAsync(frame);
                needsFrame.set(true);
                // Give an FPS message every 30 frames submitted
                if(frameCounter % 30 == 29) {
                    long timeSpent = System.currentTimeMillis() - fpsPeriod;
                    System.out.println("Sent 30 frames. Average FPS: " + 30f / (timeSpent / 1000f));
                    fpsPeriod = System.currentTimeMillis();
                }

                frameCounter++;
            }
        }
    }
}
