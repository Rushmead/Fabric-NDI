package dev.imabad.fabricndi;

import com.mojang.authlib.GameProfile;
import com.walker.devolay.DevolaySender;
import dev.imabad.fabricndi.screens.NameScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class CameraEntity extends OtherClientPlayerEntity {

    private int zoom = 0;
    private boolean isLive = false;
    private Text name;

    public CameraEntity(ClientWorld clientWorld, GameProfile gameProfile) {
        super(clientWorld, gameProfile);
        name = new LiteralText(getUuidAsString());
    }

    public int getZoom() {
        return zoom;
    }

    @Override
    public boolean isInvisibleTo(PlayerEntity player) {
        return false;
    }

    @Override
    public boolean collides() {
        return true;
    }

    public boolean isLive() {
        return isLive;
    }

    public void setLive(boolean live) {
        isLive = live;
    }

    @Override
    public boolean isInvisible() {
        return true;
    }

    public void setZoom(int zoom) {
        this.zoom = zoom;
    }

    @Override
    public void setSneaking(boolean sneaking) {

    }

    public void setName(String text){
        this.name = new LiteralText(text);
        DevolaySender devolaySender = new DevolaySender("MC - " + text);
        FabricNDI.instance.cameraControls.get(getUuid()).updateSender(devolaySender);
        FabricNDI.instance.cameras.get(getUuid()).updateSender(devolaySender);
    }

    @Override
    public Text getDisplayName() {
        return name;
    }

    @Override
    public boolean shouldRender(double distance) {
        if(MinecraftClient.getInstance().getCameraEntity() instanceof CameraEntity){
            return false;
        }
        return super.shouldRender(distance);
    }

    @Override
    public boolean isSneaking() {
        return false;
    }

    @Override
    protected Vec3d adjustMovementForSneaking(Vec3d movement, MovementType type) {
        return movement;
    }

    @Override
    public boolean isInSneakingPose() {
        return false;
    }

    @Override
    public void remove() {
        super.remove();
        FabricNDI.instance.cameraControls.get(getUuid()).end();
        FabricNDI.instance.cameras.get(getUuid()).end();
        FabricNDI.instance.cameras.remove(getUuid());
        FabricNDI.instance.cameraEntities.remove(this);
    }


    @Override
    public boolean interact(PlayerEntity player, Hand hand) {
       MinecraftClient.getInstance().openScreen(new NameScreen(this));
       return true;
    }
}
