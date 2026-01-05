package me.enderkill98.enderspecimina;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class ItemFrameKeeper implements ClientTickEvents.EndTick {

    private final HashMap<BlockPos, Long> allowDespawningUntil = new HashMap<>();
    private final HashSet<ItemFrameEntity> keptItemFrames = new HashSet<>();

    public void onSetActive(boolean active) {
        if(!active)
            despawnKeptItemFrames(MinecraftClient.getInstance(), -1);
        allowDespawningUntil.clear();
    }

    @Override
    public void onEndTick(MinecraftClient client) {
        if(!Config.HANDLER.instance().itemFrameKeeper) return;
        if(client.player == null || client.world == null) {
            keptItemFrames.clear();
            allowDespawningUntil.clear();
            return;
        }

        // Cleanup
        ArrayList<BlockPos> remove = new ArrayList<>();
        for(Map.Entry<BlockPos, Long> entry : allowDespawningUntil.entrySet()) {
            if(System.currentTimeMillis() < entry.getValue())
                remove.add(entry.getKey());
        }
        remove.forEach(allowDespawningUntil::remove);

        despawnKeptItemFrames(client, Config.HANDLER.instance().itemFrameKeeperRadius);
    }

    public void despawnKeptItemFrames(MinecraftClient client, int radius) {
        if(client.player == null || client.world == null) {
            keptItemFrames.clear();
            return;
        }

        double radiusSqrt = radius * radius;

        Vec3d ownPos2D = client.player.getPos().multiply(1, 0, 1);
        HashSet<ItemFrameEntity> removeFrames = new HashSet<>();
        for(ItemFrameEntity keptFrame : keptItemFrames) {
            if(keptFrame.getPos().multiply(1, 0, 1).squaredDistanceTo(ownPos2D) > radiusSqrt) {
                removeFrames.add(keptFrame);
            }
        }
        if(!removeFrames.isEmpty()) {
            removeFrames.stream().mapToInt(Entity::getId).forEach((entityId) -> client.world.removeEntity(entityId, Entity.RemovalReason.DISCARDED));
            keptItemFrames.removeAll(removeFrames);
            //logger.info("Despawned " + removeFrames.size() + " ItemFrames, which should have despawned a while ago already.");
        }
    }

    public void addKeptItemFrame(ItemFrameEntity itemFrame) {
        keptItemFrames.add(itemFrame);
    }

    public void keptItemFrameSpawnedAgain(int entityId) {
        keptItemFrames.removeIf((frame) -> frame.getId() == entityId);
    }

    public void onDamageSoundAt(BlockPos pos) {
        allowDespawningUntil.put(pos, System.currentTimeMillis() + 250);
    }

    public boolean isAllowedToDespawn(BlockPos pos) {
        return System.currentTimeMillis() > allowDespawningUntil.getOrDefault(pos, Long.MAX_VALUE);
    }

    public void despawnKeptItemFramesAt(ClientWorld world, BlockPos pos) {
        List<ItemFrameEntity> itemFramesAtPos = keptItemFrames.stream().filter((frame) -> frame.getBlockPos().equals(pos)).toList();
        for(ItemFrameEntity itemFrame : itemFramesAtPos) {
            world.removeEntity(itemFrame.getId(), Entity.RemovalReason.DISCARDED);
            keptItemFrames.remove(itemFrame);
        }
    }

}
