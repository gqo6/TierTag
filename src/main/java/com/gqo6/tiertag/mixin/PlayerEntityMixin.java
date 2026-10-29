package com.gqo6.tiertag.mixin;

import com.gqo6.tiertag.TierTagMod;
import com.gqo6.tiertag.config.TierConfig;
import com.gqo6.tiertag.render.TierFormatter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void mcpvp$appendTier(CallbackInfoReturnable<Text> cir) {
        TierConfig config = TierTagMod.config();
        if (config == null || !config.enabled || !config.showAboveHead) {
            return;
        }
        Text original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        PlayerEntity self = (PlayerEntity) (Object) this;
        cir.setReturnValue(TierFormatter.appendTier(self.getUuid(), self.getGameProfile().name(), original));
    }
}
