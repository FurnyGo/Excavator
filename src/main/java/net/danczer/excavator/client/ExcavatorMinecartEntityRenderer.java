package net.danczer.excavator.client;

import net.danczer.excavator.ExcavatorMinecartEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MinecartEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3f;

public class ExcavatorMinecartEntityRenderer extends MinecartEntityRenderer<ExcavatorMinecartEntity> {
    public ExcavatorMinecartEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, EntityModelLayers.CHEST_MINECART);
    }

    @Override
    public void render(ExcavatorMinecartEntity abstractMinecartEntity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light) {
        super.render(abstractMinecartEntity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);

        int lightAbove = WorldRenderer.getLightmapCoordinates(abstractMinecartEntity.getWorld(), abstractMinecartEntity.getBlockPos().up());

        //show facing
        matrixStack.push();

        Vec3f translate = new Vec3f(abstractMinecartEntity.isForwardFacing() ? 1f : -1f, 1.25f, 0f);
        translate.rotate(Vec3f.POSITIVE_Y.getDegreesQuaternion(-yaw));
        matrixStack.translate(translate.getX(), translate.getY(), translate.getZ());
        MinecraftClient.getInstance().getItemRenderer().renderItem(Items.SOUL_TORCH.getDefaultStack(), ModelTransformation.Mode.GROUND, lightAbove, 0, matrixStack, vertexConsumerProvider, 0);
        matrixStack.pop();
    }
}
