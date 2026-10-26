package com.envyworld.mixin;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Minecraft.class)
public interface MinecraftRemoteInvoker {
    @Invoker("startAttack") boolean envyworld$attack();
    @Invoker("startUseItem") void envyworld$use();
}
