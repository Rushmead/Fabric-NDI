package dev.imabad.fabricndi;

import com.mojang.authlib.GameProfile;
import dev.imabad.fabricndi.threads.NDIControlThread;
import dev.imabad.fabricndi.threads.NDIThread;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CameraManager {


    public ConcurrentSet<CameraEntity> cameraEntities;
    public ConcurrentHashMap<UUID, NDIThread> cameras;
    public ConcurrentHashMap<UUID, NDIControlThread> cameraControls;

    public CameraManager() {
        cameras = new ConcurrentHashMap<>();
        cameraControls = new ConcurrentHashMap<>();
        cameraEntities = new ConcurrentSet<>();
    }

    public void save(File runDirectory, ClientPlayerEntity player, boolean isIntegratedServer, LevelStorage levelStorage, IntegratedServer server){
        if(isIntegratedServer && server == null){
            return;
        } else if(player == null || player.networkHandler == null){
            return;
        }
        CompoundTag allEntities = new CompoundTag();
        cameraEntities.forEach(cameraEntity -> {
            CompoundTag compoundTag = cameraEntity.getTag();
            allEntities.put(cameraEntity.getUuidAsString(), compoundTag);
        });
        File serversFolder = new File(runDirectory, "cameras");
        File saveFile;
        if(!isIntegratedServer){
            if(!serversFolder.exists()){
                serversFolder.mkdir();
            }
            String ip = player.networkHandler.getConnection().getAddress().toString() ;
            ip = ip.substring(ip.indexOf("/") + 1);
            ip = ip.substring(0, ip.indexOf(":"));
            saveFile = new File(serversFolder, ip + ".dat");
        } else {
            saveFile = new File(server.getSavePath(WorldSavePath.ROOT).toFile(), "cameras.dat");
        }
        try {
            NbtIo.writeCompressed(allEntities, new FileOutputStream(saveFile));
        } catch (Exception exception) {
            System.out.println("Error occurred whilst saving cameras " + exception);
        }
    }

    public void load(File runDirectory, boolean isIntegratedServerRunning, IntegratedServer server, LevelStorage levelStorage, ClientWorld world, ClientPlayNetworkHandler clientPlayNetworkHandler){
        File serversFolder = new File(runDirectory, "cameras");
        File saveFile = new File(serversFolder, "temp.dat");
        if(!isIntegratedServerRunning && clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection() != null){
            if(serversFolder.exists()){
                String ip = clientPlayNetworkHandler.getConnection().getAddress().toString() ;
                ip = ip.substring(ip.indexOf("/") + 1);
                ip = ip.substring(0, ip.indexOf(":"));
                saveFile = new File(serversFolder, ip + ".dat");
            }
        } else if (server != null){
            saveFile = new File(server.getSavePath(WorldSavePath.ROOT).toFile(), "cameras.dat");
        }
        CompoundTag compoundTag = null;

        try {
            if (saveFile.exists() && saveFile.isFile()) {
                compoundTag = NbtIo.readCompressed(new FileInputStream(saveFile));
            }
        } catch (Exception exception) {
            System.out.println("Error occurred whilst loading cameras " + exception);
        }
        if(compoundTag != null) {
            CompoundTag finalCompoundTag = compoundTag;
            compoundTag.getKeys().forEach(s -> {
                CompoundTag compoundTag1 = (CompoundTag) finalCompoundTag.get(s);
                CameraEntity cameraEntity = new CameraEntity(world, new GameProfile(UUID.fromString(compoundTag1.getString("uuid")), compoundTag1.getString("name")));
                cameraEntity.cameraFromTag(compoundTag1);
                world.addEntity(cameraEntity.getEntityId(), cameraEntity);
                cameraEntities.add(cameraEntity);
            });
        }
    }
}
