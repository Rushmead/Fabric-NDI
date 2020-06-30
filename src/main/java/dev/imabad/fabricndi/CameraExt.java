package dev.imabad.fabricndi;

import net.minecraft.client.render.Camera;

public interface CameraExt {

    void setCameraY(float cameraY);

    float getCameraY();

    static CameraExt from(Camera self){
        return (CameraExt) self;
    }

}
