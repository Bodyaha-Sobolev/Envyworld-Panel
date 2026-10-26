package com.envyworld.mixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
@Mixin(Minecraft.class)
public abstract class SpectatorKeysMixin {
 @WrapOperation(method="handleKeybinds",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z",ordinal=0))
 private boolean envyworld$hotbarSlots(LocalPlayer player,Operation<Boolean> original){return original.call(player)&&!com.envyworld.client.SpectatorView.active();}
 @Inject(method="startAttack",at=@At("HEAD"),cancellable=true)
 private void envyworld$swingOnly(CallbackInfoReturnable<Boolean> result){if(com.envyworld.client.SpectatorView.active()){var c=Minecraft.getInstance();c.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);if(c.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit)com.envyworld.client.ClientMacros.trigger(com.envyworld.bind.BindTrigger.ATTACK_ENTITY,c.player,hit.getEntity());result.setReturnValue(true);}}
}
