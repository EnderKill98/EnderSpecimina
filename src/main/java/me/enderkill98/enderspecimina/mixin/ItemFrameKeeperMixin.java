package me.enderkill98.enderspecimina.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.enderkill98.enderspecimina.Config;
import me.enderkill98.enderspecimina.ItemFrameKeeper;
import me.enderkill98.enderspecimina.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ItemFrameKeeperMixin {

    @Shadow private ClientWorld world;

    @WrapOperation(method = "onEntitiesDestroy", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/EntitiesDestroyS2CPacket;getEntityIds()Lit/unimi/dsi/fastutil/ints/IntList;"))
    public IntList getEntityIdsFromOnEntitiesDestroy(EntitiesDestroyS2CPacket instance, Operation<IntList> original) {
        final ItemFrameKeeper keeper = Mod.itemFrameKeeper;
        if(!Config.HANDLER.instance().itemFrameKeeper) return original.call(instance);
        IntList newIntList = new IntArrayList();
        for(int entityId : instance.getEntityIds()) {
            Entity entity = world.getEntityById(entityId);
            // Filter out all ItemFrames
            if(!(entity instanceof ItemFrameEntity itemFrame)) {
                newIntList.add(entityId);
                continue;
            }

            keeper.addKeptItemFrame(itemFrame);
        }
        return newIntList;
    }

    @Inject(method = "onPlaySound", at = @At("TAIL"))
    public void onPlaySound(PlaySoundS2CPacket packet, CallbackInfo info) {
        if(!Config.HANDLER.instance().itemFrameKeeper) return;

        SoundEvent sound = packet.getSound().value();
        if((sound == SoundEvents.ENTITY_ITEM_FRAME_BREAK || sound == SoundEvents.ENTITY_GLOW_ITEM_FRAME_BREAK)) {
            // Sound is played AFTER despawning! So despawn it later, when receiving the sound
            Mod.itemFrameKeeper.despawnKeptItemFramesAt(world, BlockPos.ofFloored(packet.getX(), packet.getY(), packet.getZ()));
        }
    }


    @Inject(method = "onEntitySpawn", at = @At("TAIL"))
    public void onEntitySpawn(EntitySpawnS2CPacket packet, CallbackInfo info) {
        if(packet.getEntityType() != EntityType.ITEM_FRAME && packet.getEntityType() != EntityType.GLOW_ITEM_FRAME) return;
        if(!Config.HANDLER.instance().itemFrameKeeper) return;
        Mod.itemFrameKeeper.keptItemFrameSpawnedAgain(packet.getEntityId());
    }

    @Inject(method = "onPlayerRespawn", at = @At("TAIL"))
    public void onPlayerRespawn(CallbackInfo info) {
        if(!Config.HANDLER.instance().itemFrameKeeper) return;
        Mod.itemFrameKeeper.despawnKeptItemFrames(MinecraftClient.getInstance(), -1);
    }

    @Inject(method = "onGameJoin", at = @At("TAIL"))
    public void onGameJoin(CallbackInfo info) {
        if(!Config.HANDLER.instance().itemFrameKeeper) return;
        Mod.itemFrameKeeper.despawnKeptItemFrames(MinecraftClient.getInstance(), -1);
    }

}
