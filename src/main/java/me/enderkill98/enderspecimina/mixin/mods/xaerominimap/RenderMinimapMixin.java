package me.enderkill98.enderspecimina.mixin.mods.xaerominimap;

import com.llamalad7.mixinextras.sugar.Local;
import me.enderkill98.enderspecimina.render.RenderManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.graphics.CustomVertexConsumers;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.hud.minimap.module.MinimapSession;

// Based on MixinMinimapFBORenderer.showRenderDistanceWorldMap(...) from XaeroPlus (branch 1.20.6)
@Mixin(value = MinimapFBORenderer.class, remap = false)
public class RenderMinimapMixin {

    @Inject(method = "renderChunksToFBO", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V",
            ordinal = 0
    ), remap = true)
    public void drawRenderDistanceSquare(MinimapSession minimapSession, DrawContext guiGraphics, MinimapProcessor minimap, Vec3d renderPos, RegistryKey<World> mapDimension, double mapDimensionScale, int viewW, float partial, int level, boolean useWorldMap, boolean lockedNorth, int shape, double ps, double pc, boolean cave, CustomVertexConsumers cvc, final CallbackInfo ci,
                                         @Local(name = "xFloored") int xFloored,
                                         @Local(name = "zFloored") int zFloored,
                                         @Local(name = "renderTypeBuffers") VertexConsumerProvider.Immediate renderTypeBuffers,
                                         @Local(name = "matrixStack") MatrixStack matrixStack,
                                         @Local(name = "radiusBlocks") double radiusBlocks
    ) {
        final VertexConsumer lineBufferBuilder = renderTypeBuffers.getBuffer(xaero.common.graphics.CustomRenderTypes.MAP_LINES);
        final MinecraftClient client = MinecraftClient.getInstance();

        RenderManager.MapWorld mapWorld = null;
        if(mapDimension == World.OVERWORLD) mapWorld = RenderManager.MapWorld.Overworld;
        else if(mapDimension == World.NETHER) mapWorld = RenderManager.MapWorld.Nether;
        else if(mapDimension == World.END) mapWorld = RenderManager.MapWorld.End;

        final RenderManager.MapInfo mapInfo = new RenderManager.MapInfo(RenderManager.MapType.Minimap, mapWorld, xFloored - radiusBlocks, zFloored - radiusBlocks, xFloored + radiusBlocks, zFloored + radiusBlocks);

        RenderManager.renderMap(client, guiGraphics, null, matrixStack, renderTypeBuffers, lineBufferBuilder, mapDimensionScale, (double) xFloored, (double) zFloored, client.getRenderTickCounter().getTickProgress(true), mapInfo);
    }

}
