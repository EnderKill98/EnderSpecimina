package me.enderkill98.enderspecimina.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.enderkill98.enderspecimina.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public abstract class ScrollZoomCameraMixin {

    @WrapOperation(
        method = "update",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F")
    )
    public float clipToSpaceFromUpdate(Camera instance, float desiredCameraDistance, Operation<Float> original) {
        if (!Mod.scrollZoom.isActive() || Mod.scrollZoom.zoomStep >= 0) {
            return original.call(instance, desiredCameraDistance);
        }

        float interpolatedExtraDist;
        if (Mod.scrollZoom.previousTickZoomStep <= 1) {
            double previousFactor = Mod.scrollZoom.getPreviousZoomMultiplierFactor();
            if (Mod.scrollZoom.previousTickZoomStep == 1) {
                previousFactor = 0;
            }
            // 1.21.5: use MinecraftClient.getInstance().getTickDelta()
            interpolatedExtraDist = (float) MathHelper.lerp(
                MinecraftClient.getInstance().getTickDelta(),
                -previousFactor,
                -Mod.scrollZoom.getZoomMultiplierFactor()
            );
        } else {
            interpolatedExtraDist = (float) -Mod.scrollZoom.getZoomMultiplierFactor();
        }

        // Ignore walls. Huge distances could cause issues anyway
        return desiredCameraDistance + interpolatedExtraDist;
    }
}
