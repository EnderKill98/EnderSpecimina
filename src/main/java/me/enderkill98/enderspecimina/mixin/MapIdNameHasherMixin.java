package me.enderkill98.enderspecimina.mixin;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.enderkill98.enderspecimina.Config;
import me.enderkill98.enderspecimina.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.*;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

@Mixin(value = ClientPlayNetworkHandler.class, priority = 900) // Hopefully prevent breaking any packet loggers
public abstract class MapIdNameHasherMixin extends ClientCommonNetworkHandler {

    @Shadow public abstract void onMapUpdate(MapUpdateS2CPacket packet);

    @Unique private static final Logger LOGGER = Mod.getLoggerFor("me.enderkill98.enderspecimina.mixin", "MapIdNameHasherMixin");

    protected MapIdNameHasherMixin(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
        super(client, connection, connectionState);
    }

    @Unique private final BiMap<Integer/*Orig MapId*/, Integer/*Mapped MapId*/> enderspecimina$mapping = HashBiMap.create(); // To detect maps with same name and not have them show up as the same
    @Unique private final HashMap<Integer/*Orig MapId*/, HashSet<Integer/*Mapped MapId*/>> enderspecimina$mappingAdditional = new HashMap<>(); // To detect maps with same name and not have them show up as the same

    @Unique private void enderspecimina$fixMap(ItemStack stack) {
        if(stack == null || stack.isEmpty()) return; // No item
        if (!Config.HANDLER.instance().mapIdNameHashes.isActive(client)) return; // Not applicable

        // Recurse container type items
        if(stack.contains(DataComponentTypes.BUNDLE_CONTENTS)) {
            //LOGGER.info("Mapping bundle contents");
            stack.get(DataComponentTypes.BUNDLE_CONTENTS).stacks.forEach(this::enderspecimina$fixMap); // stream() returns copied Stacks
            return;
        }else if(stack.contains(DataComponentTypes.CONTAINER)) {
            //LOGGER.info("Mapping container contents");
            stack.get(DataComponentTypes.CONTAINER).stacks.forEach(this::enderspecimina$fixMap); // stream() returns copied Stacks
            return;
        }

        if(!stack.contains(DataComponentTypes.MAP_ID)) return; // Not applicable

        final MapIdComponent mapId = stack.get(DataComponentTypes.MAP_ID);
        final String name = stack.contains(DataComponentTypes.CUSTOM_NAME) ? stack.get(DataComponentTypes.CUSTOM_NAME).getString() : null;
        if(name == null) {
            if(enderspecimina$mapping.containsKey(mapId.id())) {
                // Remap unnamed map id to already known mapped map id
                stack.set(DataComponentTypes.MAP_ID, new MapIdComponent(enderspecimina$mapping.get(mapId.id())));
            }
            return; // Can't remap
        }
        final int mappedMapId = name.hashCode();
        if (enderspecimina$mapping.containsValue(mappedMapId)) {
            // Essentially hash collisions
            if (enderspecimina$mapping.inverse().get(mappedMapId) != mapId.id())
                return; // Already seen a map by this name, but this has a different original id. Not remapping this one!
        }else {
            boolean wasAlreadyMapped = enderspecimina$mapping.containsKey(mapId.id());
            if(!wasAlreadyMapped) {
                enderspecimina$mapping.put(mapId.id(), mappedMapId);
                if (client.world.getMapState(new MapIdComponent(mappedMapId)) == null && client.world.getMapState(mapId) != null) {
                    // Swap stored map id
                    try {
                        client.world.putClientsideMapState(new MapIdComponent(mappedMapId), client.world.getMapState(mapId));
                        client.world.mapStates.remove(mapId);
                    }catch (Exception ex) {
                        LOGGER.error("Caught an exception trying to swap existing clientside mapState {} for new mapped one {}. This might be an EnderSpecimina and/or EvMod (compatibility) bug!", mapId, mappedMapId, ex);
                    }
                    if(Config.HANDLER.instance().mapIdNameHashesDebug)
                        LOGGER.info("Moved local mapstate for ID {} to {} after encountering named map item.", mapId.id(), mappedMapId);
                }
            }else {
                boolean added = enderspecimina$mappingAdditional.computeIfAbsent(mapId.id(), k -> new HashSet<>()).add(mappedMapId);
                int firstMappedId = enderspecimina$mapping.get(mapId.id());
                if(added && Config.HANDLER.instance().mapIdNameHashesDebug)
                    LOGGER.info("Detected duplicate mapped id for {} (first got mapped to {}). Remembering as duplicate mapping (to {})", mapId.id(), firstMappedId, mappedMapId);
                // Already seen a map by this name, but this has a different original id. Not remapping this one and re-adding og id!
                if (client.world.getMapState(new MapIdComponent(mappedMapId)) == null && client.world.getMapState(new MapIdComponent(firstMappedId)) != null) {
                    // Restore mapstate for original map (copy)
                    try {
                        client.world.putClientsideMapState(new MapIdComponent(mappedMapId), client.world.getMapState(new MapIdComponent(firstMappedId)));
                    }catch (Exception ex) {
                        LOGGER.error("Caught an exception trying to copy existing clientside mapState of {} to {} due to map with same Id but different name found. This might be an EnderSpecimina and/or EvMod (compatibility) bug!", firstMappedId, mappedMapId, ex);
                    }
                    if(Config.HANDLER.instance().mapIdNameHashesDebug)
                        LOGGER.info("Copied mapstate of original map id {} from first mapped id {} to new mapped id {}", mapId.id(), firstMappedId, mappedMapId);
                }
                //return; // Don't change
            }
        }
        stack.set(DataComponentTypes.MAP_ID, new MapIdComponent(mappedMapId));
    }

    @Inject(method = "onInventory", at = @At("HEAD"))
    public void onInventory(InventoryS2CPacket packet, CallbackInfo info) {
        if(client.isOnThread()) {
            packet.contents().forEach(this::enderspecimina$fixMap);
            enderspecimina$fixMap(packet.cursorStack());
        }
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("HEAD"))
    public void onScreenHandlerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo info) {
        if(client.isOnThread()) enderspecimina$fixMap(packet.getStack());
    }

    @Inject(method = "onSetPlayerInventory", at = @At("HEAD"))
    public void onSetPlayerInventory(SetPlayerInventoryS2CPacket packet, CallbackInfo info) {
        if(client.isOnThread()) enderspecimina$fixMap(packet.contents());
    }

    @Inject(method = "onSetCursorItem", at = @At("HEAD"))
    public void onSetCursorItem(SetCursorItemS2CPacket packet, CallbackInfo info) {
        if(client.isOnThread()) enderspecimina$fixMap(packet.contents());
    }

    @Inject(method = "onEntityEquipmentUpdate", at = @At("HEAD"))
    public void onEntityEquipmentUpdate(EntityEquipmentUpdateS2CPacket packet, CallbackInfo info) {
        if(client.isOnThread()) packet.getEquipmentList().forEach(entry -> enderspecimina$fixMap(entry.getSecond()));
    }

    @Inject(method = "onEntityTrackerUpdate", at = @At("HEAD"))
    public void onEntityTrackerUpdate(EntityTrackerUpdateS2CPacket packet, CallbackInfo info) {
        if(client.isOnThread()) {
            // For example shown item in item frame
            for(DataTracker.SerializedEntry<?> tracked : packet.trackedValues()) {
                if(tracked.value() instanceof ItemStack trackedStack)
                    enderspecimina$fixMap(trackedStack);
            }
        }
    }

    @Unique private boolean enderspecimina$skipOnMapUpdate = false;

    @WrapOperation(method = "onMapUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/MapUpdateS2CPacket;mapId()Lnet/minecraft/component/type/MapIdComponent;"))
    public MapIdComponent mapIdFromOnMapUpdate(MapUpdateS2CPacket instance, Operation<MapIdComponent> original) {
        MapIdComponent origMapId = original.call(instance);
        if(enderspecimina$skipOnMapUpdate) return origMapId;

        if(enderspecimina$mapping.containsKey(origMapId.id())) {
            if(enderspecimina$mappingAdditional.containsKey(origMapId.id())) {
                // Repeat for additionals (same map id, but different name) first. TODO: Do not disconnect both mapstates
                enderspecimina$skipOnMapUpdate = true; // Prevent recursion
                HashSet<Integer> additionals = enderspecimina$mappingAdditional.get(origMapId.id());
                if(Config.HANDLER.instance().mapIdNameHashesDebug)
                    LOGGER.info("Also applying update of MapId {} to {}", origMapId.id(), additionals.stream().map(id -> "" + id).collect(Collectors.joining(", ")));
                for(int addMapId : additionals) {
                    try {
                        this.onMapUpdate(new MapUpdateS2CPacket(new MapIdComponent(addMapId), instance.scale(), instance.locked(), instance.decorations(), instance.updateData()));
                    }catch (Exception ex) {
                        LOGGER.error("Caught exception when copying existing clientside mapState of {} to mapId {}.", origMapId.id(), addMapId, ex);
                    }
                }
                enderspecimina$skipOnMapUpdate = false;
            }
            if(Config.HANDLER.instance().mapIdNameHashesDebug)
                LOGGER.info("Pretended update of MapId {} was for {}", origMapId.id(), enderspecimina$mapping.get(origMapId.id()));
            return new MapIdComponent(enderspecimina$mapping.get(origMapId.id()));
        }
        return origMapId;
    }

}
