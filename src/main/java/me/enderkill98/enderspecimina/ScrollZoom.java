package me.enderkill98.enderspecimina;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ScrollZoom implements ClientTickEvents.StartTick {

    public int zoomStep = 1;
    public int previousTickZoomStep = 1;

    private boolean active = false;
    public void setActive(boolean active) {
        if(!active) {
            zoomStep = 1;
            previousTickZoomStep = 1;
        }
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * @return Whether to cancel the scroll (not handle elsewhere)
     */
    public boolean onScroll(double vertical) {
        if(!Config.HANDLER.instance().scrollZoom) return false;
        if(!Screen.hasAltDown() || Screen.hasControlDown()) return false;
        //if(Mod.INSTANCE.selection.isActive() && Mod.INSTANCE.selection.isHoldingWand(MinecraftClient.getInstance().player))
        //    return false; // Let Selection handle all scrolls!

        int newZoomStep = zoomStep + new BigDecimal(vertical).setScale(0, RoundingMode.HALF_UP).intValue();
        if(MinecraftClient.getInstance().options.getPerspective().isFirstPerson())
            newZoomStep = Math.max(1, newZoomStep);
        if(newZoomStep == 0)
            newZoomStep += vertical < 0 ? -1 : 1; // Skip 0

        while (getZoomMultiplierFactor(Math.abs(newZoomStep)) < 0 && getZoomMultiplierFactor(Math.abs(zoomStep)) > 0)
            newZoomStep += vertical > 0 ? -1 : 1; // Seems to go infinite. But looks funny, so I'll keep it.
        zoomStep = newZoomStep;
        setActive(zoomStep != 1);

        return true;
    }

    public double getZoomMultiplierFactor() {
        return getZoomMultiplierFactor(zoomStep);
    }

    public double getPreviousZoomMultiplierFactor() {
        return getZoomMultiplierFactor(previousTickZoomStep);
    }

    private double getZoomMultiplierFactor(int zoomStep) {
        int zoomStepAbs = Math.abs(zoomStep);
        int mult = zoomStep < 0 ? -1 : 1;
        return Math.pow(2, (double) (zoomStepAbs-1) * 0.5) * mult;
        //return 1 + ((zoomStep-1) * 1f);
    }

    @Override
    public void onStartTick(MinecraftClient client) {
        if(zoomStep < 0 && client.options.getPerspective().isFirstPerson())
            setActive(false);

        previousTickZoomStep = zoomStep; // Needs to be start tick or will affectively never apply
    }
}
