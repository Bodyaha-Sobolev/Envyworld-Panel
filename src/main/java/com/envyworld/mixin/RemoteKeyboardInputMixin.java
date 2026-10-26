package com.envyworld.mixin;
import net.minecraft.client.player.*;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardInput.class)
public abstract class RemoteKeyboardInputMixin extends ClientInput {
    @Inject(method="tick",at=@At("TAIL"))
    private void envyworld$remote(CallbackInfo ci){var input=com.envyworld.client.RemoteGame.input();if(input!=null){keyPresses=input;moveVector=new Vec2((input.left()?1:0)-(input.right()?1:0),(input.forward()?1:0)-(input.backward()?1:0)).normalized();}}
}
