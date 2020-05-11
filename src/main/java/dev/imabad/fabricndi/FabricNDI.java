package dev.imabad.fabricndi;

import com.mojang.authlib.GameProfile;
import com.walker.devolay.Devolay;
import dev.imabad.fabricndi.threads.NDIControlThread;
import dev.imabad.fabricndi.threads.NDIThread;
import io.netty.util.internal.ConcurrentSet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FabricNDI implements ModInitializer {

    public static FabricNDI instance;

    private static FabricKeyBinding keyBinding, killAll;
    public ConcurrentSet<CameraEntity> cameraEntities;
    public ConcurrentHashMap<UUID, NDIThread> cameras;
    public ConcurrentHashMap<UUID, NDIControlThread> cameraControls;
    private GameRenderHook gameRenderHook;

    @Override
    public void onInitialize() {
        System.out.println("Starting Fabric NDI, loading NDI libraries.");
        Devolay.loadLibraries();
        instance = this;
        cameras = new ConcurrentHashMap<>();
        cameraControls = new ConcurrentHashMap<>();
        cameraEntities = new ConcurrentSet<>();
        keyBinding = FabricKeyBinding.Builder.create(new Identifier("fabricndi", "newcamera"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "NDI").build();
        killAll = FabricKeyBinding.Builder.create(new Identifier("fabricndi", "killall"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, "NDI").build();
        KeyBindingRegistry.INSTANCE.addCategory("NDI");
        KeyBindingRegistry.INSTANCE.register(keyBinding);
        KeyBindingRegistry.INSTANCE.register(killAll);
        ClientTickCallback.EVENT.register(e -> {
            if(keyBinding.isPressed() && e.world != null && e.player != null){
                UUID uuid = UUID.randomUUID();
                CameraEntity armorStandEntity = new CameraEntity(e.world, new GameProfile(uuid, uuid.toString()));
                armorStandEntity.refreshPositionAndAngles(e.player.getX(), e.player.getY(), e.player.getZ(), e.player.yaw, e.player.pitch);
                e.world.addEntity(armorStandEntity.getEntityId(), armorStandEntity);
                keyBinding.setPressed(false);
                FabricNDI.instance.cameraEntities.add(armorStandEntity);
            } else if(killAll.isPressed() && e.world != null && e.player != null){
                for(Entity ent : cameraEntities){
                    e.world.removeEntity(ent.getEntityId());
                }
            }
        });
    }

    public GameRenderHook getGameRenderHook() {
        if(gameRenderHook == null)
            gameRenderHook = new GameRenderHook("MC - Player");
        return gameRenderHook;
    }
}
