package me.enderkill98.enderspecimina.mixin.mods.xaeroworldmap;

import com.llamalad7.mixinextras.sugar.Local;
import me.enderkill98.enderspecimina.render.RenderManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;
import xaero.map.gui.ScreenBase;

// Based on MixinGuiMap.showRenderDistanceWorldMap(...) from XaeroPlus (branch 1.20.6)
@Mixin(value = GuiMap.class, remap = false)
public class RenderWorldMapMixin extends ScreenBase {

    @Shadow
    private double cameraX;

    @Shadow private double cameraZ;

    @Shadow private double scale;

    @Shadow private MapProcessor mapProcessor;

    protected RenderWorldMapMixin(Screen parent, Screen escape, Text titleIn) {
        super(parent, escape, titleIn);
    }

    @Inject(method = "render", at = @At(value = "FIELD", target = "Lxaero/map/settings/ModSettings;renderArrow:Z", opcode = Opcodes.GETFIELD, ordinal = 0, remap = false), remap = true)
    public void renderPathLines(DrawContext guiGraphics, int scaledMouseX, int scaledMouseY, float partialTicks, final CallbackInfo ci,
                                @Local(name = "renderTypeBuffers") VertexConsumerProvider.Immediate renderTypeBuffers,
                                @Local(name = "matrixStack") MatrixStack matrixStack,
                                @Local(name = "leftBorder") double leftBorder, @Local(name = "topBorder") double topBorder,
                                @Local(name = "rightBorder") double rightBorder, @Local(name = "bottomBorder") double bottomBorder) {
        VertexConsumer lineBufferBuilder = renderTypeBuffers.getBuffer(xaero.common.graphics.CustomRenderTypes.MAP_LINES);
        final MinecraftClient client = MinecraftClient.getInstance();

        RenderManager.MapWorld mapWorld = null;
        RegistryKey<World> dimId = mapProcessor.getMapWorld().getCurrentDimension().getDimId();
        if(dimId == World.OVERWORLD) mapWorld = RenderManager.MapWorld.Overworld;
        else if(dimId == World.NETHER) mapWorld = RenderManager.MapWorld.Nether;
        else if(dimId == World.END) mapWorld = RenderManager.MapWorld.End;

        final RenderManager.MapInfo mapInfo = new RenderManager.MapInfo(RenderManager.MapType.Worldmap, mapWorld, leftBorder, topBorder, rightBorder, bottomBorder);

        RenderManager.renderMap(client, guiGraphics, textRenderer, matrixStack, renderTypeBuffers, lineBufferBuilder, scale, cameraX, cameraZ, client.getRenderTickCounter().getTickProgress(true), mapInfo);
    }
}
