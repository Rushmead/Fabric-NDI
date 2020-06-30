package dev.imabad.fabricndi;

import com.mojang.authlib.GameProfile;
import com.walker.devolay.Devolay;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class FabricNDI implements ModInitializer {

    public static FabricNDI instance;

    private static FabricKeyBinding keyBinding, killAll;
    private GameRenderHook gameRenderHook;
    private CameraManager cameraManager;

    @Override
    public void onInitialize() {
        System.out.println("Starting Fabric NDI, loading NDI libraries.");
        Devolay.loadLibraries();
        instance = this;
        keyBinding = FabricKeyBinding.Builder.create(new Identifier("fabricndi", "newcamera"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "NDI").build();
        killAll = FabricKeyBinding.Builder.create(new Identifier("fabricndi", "killall"), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, "NDI").build();
        KeyBindingRegistry.INSTANCE.addCategory("NDI");
        KeyBindingRegistry.INSTANCE.register(keyBinding);
        KeyBindingRegistry.INSTANCE.register(killAll);
        ClientTickCallback.EVENT.register(e -> {
            if(keyBinding.isPressed() && e.world != null && e.player != null){
                UUID uuid = UUID.randomUUID();
                CameraEntity armorStandEntity = new CameraEntity(e.world, new GameProfile(uuid, uuid.toString()));
                armorStandEntity.resetPosition(e.player.getX(), e.player.getY(), e.player.getZ());
                armorStandEntity.updateTrackedPosition(e.player.getX(), e.player.getY(), e.player.getZ());
                armorStandEntity.updatePositionAndAngles(e.player.getX(), e.player.getY(), e.player.getZ(), e.player.yaw, e.player.pitch);
                armorStandEntity.setHeadYaw(e.player.headYaw);
                e.world.addEntity(armorStandEntity.getEntityId(), armorStandEntity);
                keyBinding.setPressed(false);
                cameraManager.cameraEntities.add(armorStandEntity);
            } else if(killAll.isPressed() && e.world != null && e.player != null){
                for(Entity ent : cameraManager.cameraEntities){
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

    public CameraManager getCameraManager() {
        if(cameraManager == null)
            cameraManager = new CameraManager();
        return cameraManager;
    }
}
