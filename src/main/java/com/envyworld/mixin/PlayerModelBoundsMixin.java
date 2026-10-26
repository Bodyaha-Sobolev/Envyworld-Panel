package com.envyworld.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelFeatureRenderer.class)
public abstract class PlayerModelBoundsMixin {
    @Shadow @Final private PoseStack poseStack;
    @Inject(method="renderModel",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V",shift=At.Shift.AFTER),require=0)
    private void envyworld$modelBounds(SubmitNodeStorage.ModelSubmit<?> submit,RenderType type,VertexConsumer vertices,OutlineBufferSource outlines,MultiBufferSource.BufferSource buffers,CallbackInfo ci){
        if(submit.model() instanceof HumanoidModel<?> model&&submit.state() instanceof AvatarRenderState state)
            com.envyworld.client.PlayerModelBounds.capture(model,state,poseStack);
    }
}
