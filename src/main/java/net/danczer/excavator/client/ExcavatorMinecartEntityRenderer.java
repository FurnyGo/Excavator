package net.danczer.excavator.client;

import net.danczer.excavator.ExcavatorMinecartEntity;
import net.danczer.excavator.ExcavatorMod;
import net.minecraft.client.model.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MinecartEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class ExcavatorMinecartEntityRenderer extends MinecartEntityRenderer<ExcavatorMinecartEntity> {
    private static final int DRILL_ROTATION_PER_MIN = 10;
    private static final float DRILL_ROTATION_PERCENT_PER_TICK = DRILL_ROTATION_PER_MIN/(60F*20);
    public static final int DRILL_COUNT = 3;
    public static final float DRILL_ROTATION_SHIFT = 1F / DRILL_COUNT;
    public static final String DRILL = "drill_";
    public static final String BODY = "body";
    public static final String DISPLAY = "display";
    public static final String HAZARD_LAMP = "lamp";
    public static final String SHAFT = "saft_";
    private final ModelPart root;
    public ExcavatorMinecartEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, EntityModelLayers.CHEST_MINECART);

        root = ctx.getPart(ExcavatorClient.MODEL_EXCAVATOR_LAYER);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        modelPartData.addChild(HAZARD_LAMP, ModelPartBuilder.create()
                        .cuboid("lamp", 1,39.5F,-1.5F,1,3,3, Dilation.NONE, 0,0),
                ModelTransform.of(0, 0, 0, 0, 0, 0));

        modelPartData.addChild(DISPLAY, ModelPartBuilder.create()
                        .cuboid("display", 6F,22F,-5,1,8,10, Dilation.NONE, 0,0),
                ModelTransform.of(0, 0, 0, 0, 0, 0));


        modelPartData.addChild(BODY, ModelPartBuilder.create()
                        .cuboid("frame_bottom",-8F,2F,-6F,16,20,12, Dilation.NONE, 22,0)
                        .cuboid("frame_front",-10F, 2F,  -6F, 2, 44, 12, Dilation.NONE, 78,0)
                        .cuboid("frame_back",-8F, 2F,  -3F, 9, 42, 6, Dilation.NONE, 22,32),
                ModelTransform.of(0, 0, 0, 0F, 0, 0));

        var shaftX = 0F;
        for (int i = 0; i < DRILL_COUNT; i++) {
            modelPartData.addChild(SHAFT+i, ModelPartBuilder.create().uv(0, 0)
                            .cuboid("shaft_left",-7F+shaftX, 2F, -5F, 2, 32, 2, Dilation.NONE, 0,18)
                            .cuboid("shaft_right", -7F+shaftX, 2F, 3F, 2, 32, 2, Dilation.NONE, 0,18),
                    ModelTransform.of(0, 0, 0, 0, 0, 0));

            shaftX += 2.5F;
        }

        var drillY = 8F;
        for (int i = 0; i < DRILL_COUNT; i++) {
            modelPartData.addChild(DRILL+i, ModelPartBuilder.create()
                            .cuboid("blade0",-12F, 0F-4F,  -4F, 2, 8, 8, Dilation.NONE,106, 24)
                            .cuboid("blade1",-14F, 1F-4F,  -3F, 2, 6, 6, Dilation.NONE,106, 12)
                            .cuboid("blade2",-16F, 2F-4F,  -2F, 2, 4, 4, Dilation.NONE,106, 4)
                            .cuboid("blade3",-18F, 3F-4F,  -1F, 2, 2, 2, Dilation.NONE,106, 0),
                    ModelTransform.of(0, drillY, 0, 0, 0, 0));
            drillY += 16F;
        }

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void render(ExcavatorMinecartEntity abstractMinecartEntity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light) {
        super.render(abstractMinecartEntity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);

        //TODO mixin MinecartEntityRenderer to get the correct adjusted yaw from super

        var vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getEntitySolid(new Identifier(ExcavatorMod.MOD_ID, "textures/entity/excavator.png")));

        root.getChild(BODY).render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
        root.getChild(DISPLAY).render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
        root.getChild(HAZARD_LAMP).render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV);

        var time = abstractMinecartEntity.getWorld().getTime() + tickDelta;

        for (int i = 0; i < DRILL_COUNT; i++) {
            var drillRotation = (float)Math.toRadians((time * DRILL_ROTATION_PERCENT_PER_TICK + i * DRILL_ROTATION_SHIFT)*360F);

            var drill = root.getChild(DRILL+i);
            drill.setAngles(drillRotation,0,0);
            drill.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1,0,0,1 );

            var shaft = root.getChild(SHAFT+i);
            matrixStack.push();
            matrixStack.translate(0,Math.sin(drillRotation) * 0.5F,0);
            shaft.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
            matrixStack.pop();
        }

    }
}
