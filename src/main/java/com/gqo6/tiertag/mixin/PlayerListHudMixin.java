package com.gqo6.tiertag.mixin;

import com.gqo6.tiertag.TierTagMod;
import com.gqo6.tiertag.config.TierConfig;
import com.gqo6.tiertag.render.TierFormatter;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Inject(method = "getPlayerName(Lnet/minecraft/client/network/PlayerListEntry;)Lnet/minecraft/text/Text;",
            at = @At("RETURN"), cancellable = true)
    private void mcpvp$appendTier(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        TierConfig config = TierTagMod.config();
        if (config == null || !config.enabled || !config.showInTabList || entry == null) {
            return;
        }
        Text original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        cir.setReturnValue(TierFormatter.appendTier(
                entry.getProfile().id(), entry.getProfile().name(), original));
    }
}
