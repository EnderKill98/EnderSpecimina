package me.enderkill98.enderspecimina.mixin;

import me.enderkill98.enderspecimina.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.BannerItem;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class FixGhostItemsMixin<T extends ScreenHandler> {

    @Shadow @Final protected T handler;

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    public void mouseDragged(CallbackInfoReturnable<Boolean> cir) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if(!Config.HANDLER.instance().fix2b2tGhostItems.isActive(client)) return;

        if(client.player != null && client.player.isCreative()) return; // Breaks creative middle-click drag (on other servers)
        ItemStack cursorStack = handler.getCursorStack();
        if(cursorStack == null || handler.getCursorStack().isEmpty()) return;
        if(!cursorStack.isStackable() || cursorStack.getItem() instanceof FilledMapItem || cursorStack.getItem() instanceof BannerItem)
            cir.setReturnValue(true); // Prevent initiating a drag
    }

}
