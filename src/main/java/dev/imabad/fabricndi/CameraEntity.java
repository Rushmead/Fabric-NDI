package dev.imabad.fabricndi;

import com.mojang.authlib.GameProfile;
import com.walker.devolay.DevolayMetadataFrame;
import com.walker.devolay.DevolaySender;
import dev.imabad.fabricndi.screens.NameScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
        if(this.name.getString().equals(text)){
            return;
        }
        this.name = new LiteralText(text);
        DevolaySender devolaySender = new DevolaySender("MC - " + text);
        DevolayMetadataFrame metadataFrame = new DevolayMetadataFrame();
        metadataFrame.setData("<ndi_capabilities ntk_ptz=\"true\"/>");
        devolaySender.addConnectionMetadata(metadataFrame);
        FabricNDI.instance.getCameraManager().cameraControls.get(getUuid()).updateSender(devolaySender);
        FabricNDI.instance.getCameraManager().cameras.get(getUuid()).updateSender(devolaySender);
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
        FabricNDI.instance.getCameraManager().cameraControls.get(getUuid()).end();
        FabricNDI.instance.getCameraManager().cameras.get(getUuid()).end();
        FabricNDI.instance.getCameraManager().cameras.remove(getUuid());
        FabricNDI.instance.getCameraManager().cameraEntities.remove(this);
    }


    @Override
    public boolean interact(PlayerEntity player, Hand hand) {
       MinecraftClient.getInstance().openScreen(new NameScreen(this));
       return true;
    }

    public CompoundTag getTag(){
        CompoundTag tag = new CompoundTag();
        tag.put("pos", this.toListTag(this.getX(), this.getY(), this.getZ()));
        tag.put("rotation", this.toListTag(this.yaw, this.pitch));
        tag.putString("name", this.name.asString());
        tag.putString("uuid", this.getUuidAsString());
        tag.putInt("zoom", this.zoom);
        return tag;
    }

    public void cameraFromTag(CompoundTag tag){
        ListTag pos = tag.getList("pos", 6);
        ListTag rotation = tag.getList("rotation", 5);
        this.resetPosition(pos.getDouble(0), pos.getDouble(1), pos.getDouble(2));
        this.yaw = rotation.getFloat(0);
        this.pitch = rotation.getFloat(1);
        this.prevYaw = this.yaw;
        this.prevPitch = this.pitch;
        this.setHeadYaw(this.yaw);
        this.setYaw(this.yaw);
        this.name = new LiteralText(tag.getString("name"));
        this.zoom = tag.getInt("zoom");
        this.refreshPosition();
    }
}
