package me.enderkill98.enderspecimina.mixin;

import me.enderkill98.enderspecimina.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SetCursorItemS2CPacket;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayNetworkHandler.class, priority = 900) // Hopefully prevent breaking any packet loggers
public abstract class FixBundlesPacketMixin extends ClientCommonNetworkHandler {

    protected FixBundlesPacketMixin(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
        super(client, connection, connectionState);
    }

    @Unique private void enderspecimina$fixBundle(ItemStack stack) {
        if(stack == null || stack.isEmpty()) return; // Not applicable
        if (!Config.HANDLER.instance().fix2b2tBundles.isActive(client)) return; // Not enabled

        // Recurse container type items
        if(stack.contains(DataComponentTypes.BUNDLE_CONTENTS)) {
            //LOGGER.info("Fixing bundle contents");
            stack.get(DataComponentTypes.BUNDLE_CONTENTS).stacks.forEach(this::enderspecimina$fixBundle); // stream() returns copied Stacks
        }else if(stack.contains(DataComponentTypes.CONTAINER)) {
            //LOGGER.info("Fixing container contents");
            stack.get(DataComponentTypes.CONTAINER).stacks.forEach(this::enderspecimina$fixBundle); // stream() returns copied Stacks
        }

        if(!stack.contains(DataComponentTypes.BUNDLE_CONTENTS)) return; // Not a bundle
        BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        stack.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(contents.stream().toList().reversed()));
    }

    @Inject(method = "onInventory", at = @At("HEAD"))
    public void onInventory(InventoryS2CPacket packet, CallbackInfo info) {
        if(client.isOnThread()) {
            packet.contents().forEach(this::enderspecimina$fixBundle);
            enderspecimina$fixBundle(packet.cursorStack());
        }
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("HEAD"))
    public void onScreenHandlerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo info) {
        if(client.isOnThread()) enderspecimina$fixBundle(packet.getStack());
    }

    @Inject(method = "onSetPlayerInventory", at = @At("HEAD"))
    public void onSetPlayerInventory(SetPlayerInventoryS2CPacket packet, CallbackInfo info) {
        if(client.isOnThread()) enderspecimina$fixBundle(packet.contents());
    }

    @Inject(method = "onSetCursorItem", at = @At("HEAD"))
    public void onSetCursorItem(SetCursorItemS2CPacket packet, CallbackInfo info) {
        if(client.isOnThread()) enderspecimina$fixBundle(packet.contents());
    }

}
