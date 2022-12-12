package net.danczer.excavator;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.*;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;

public class ExcavatorMod implements ModInitializer {
    public static final String MOD_ID = "excavator";
    public static final Identifier EXCAVATOR_IDENTIFIER = new Identifier(MOD_ID, "excavator_minecart");

    public static final ExcavatorMinecartItem EXCAVATOR_MINECART_ITEM = Registry.register(
                    Registry.ITEM,
                    EXCAVATOR_IDENTIFIER,
                    new ExcavatorMinecartItem(new FabricItemSettings().group(ItemGroup.TRANSPORTATION)));
    public static final ScreenHandlerType<ExcavatorScreenHandler> EXCAVATOR_SCREEN_HANDLER = Registry.register(
                    Registry.SCREEN_HANDLER,
                    EXCAVATOR_IDENTIFIER,
                    new ScreenHandlerType<>(ExcavatorScreenHandler::new));

    public static EntityType<ExcavatorMinecartEntity> EXCAVATOR_ENTITY = Registry.register(
                    Registry.ENTITY_TYPE,
                    EXCAVATOR_IDENTIFIER,
                    FabricEntityTypeBuilder.create(SpawnGroup.MISC, ExcavatorMinecartEntity::new)
                        .forceTrackedVelocityUpdates(true)
                        .dimensions(EntityDimensions.fixed(0.98f, 0.7f))
                        .trackRangeBlocks(8)
                        .build()
    );

    public static final Item EXCAVATOR_DRILL_STEEL = Registry.register(Registry.ITEM, new Identifier(MOD_ID, "excavator_drill_iron"), new ExcavatorDrill(new FabricItemSettings(), "excavator_drill_iron", (MiningToolItem) Items.IRON_PICKAXE, (MiningToolItem) Items.IRON_SHOVEL));
    public static final Item EXCAVATOR_DRILL_GOLD = Registry.register(Registry.ITEM, new Identifier(MOD_ID, "excavator_drill_gold"), new ExcavatorDrill(new FabricItemSettings(),"excavator_drill_stone", (MiningToolItem) Items.GOLDEN_PICKAXE, (MiningToolItem) Items.GOLDEN_SHOVEL));
    public static final Item EXCAVATOR_DRILL_DIAMOND = Registry.register(Registry.ITEM, new Identifier(MOD_ID, "excavator_drill_diamond"), new ExcavatorDrill(new FabricItemSettings(), "excavator_drill_diamond", (MiningToolItem) Items.DIAMOND_PICKAXE, (MiningToolItem) Items.DIAMOND_SHOVEL));
    public static final Item EXCAVATOR_DRILL_NETHERITE = Registry.register(Registry.ITEM, new Identifier(MOD_ID, "excavator_drill_netherite"), new ExcavatorDrill(new FabricItemSettings(), "excavator_drill_netherite", (MiningToolItem) Items.NETHERITE_PICKAXE, (MiningToolItem) Items.NETHERITE_SHOVEL));

    public static final List<BlockItem> EXCAVATOR_USABLE_RAIL_ITEMS = new ArrayList<>();
    public static final List<BlockItem> EXCAVATOR_USABLE_TORCH_ITEMS = new ArrayList<>();
    public static final List<Block> EXCAVATOR_TORCH_WALL_BLOCKS = new ArrayList<>();
    public static final List<ExcavatorDrill> EXCAVATOR_USABLE_DRILL_ITEMS = new ArrayList<>();

    static {
        EXCAVATOR_USABLE_TORCH_ITEMS.add((BlockItem) Items.TORCH);
        EXCAVATOR_USABLE_TORCH_ITEMS.add((BlockItem)Items.REDSTONE_TORCH);
        EXCAVATOR_USABLE_TORCH_ITEMS.add((BlockItem)Items.SOUL_TORCH);

        EXCAVATOR_TORCH_WALL_BLOCKS.add(Blocks.WALL_TORCH);
        EXCAVATOR_TORCH_WALL_BLOCKS.add(Blocks.REDSTONE_WALL_TORCH);
        EXCAVATOR_TORCH_WALL_BLOCKS.add(Blocks.SOUL_WALL_TORCH);

        EXCAVATOR_USABLE_RAIL_ITEMS.add((BlockItem)Items.RAIL);
        EXCAVATOR_USABLE_RAIL_ITEMS.add((BlockItem)Items.POWERED_RAIL);

        EXCAVATOR_USABLE_DRILL_ITEMS.add((ExcavatorDrill) ExcavatorMod.EXCAVATOR_DRILL_GOLD);
        EXCAVATOR_USABLE_DRILL_ITEMS.add((ExcavatorDrill) ExcavatorMod.EXCAVATOR_DRILL_STEEL);
        EXCAVATOR_USABLE_DRILL_ITEMS.add((ExcavatorDrill) ExcavatorMod.EXCAVATOR_DRILL_DIAMOND);
        EXCAVATOR_USABLE_DRILL_ITEMS.add((ExcavatorDrill) ExcavatorMod.EXCAVATOR_DRILL_NETHERITE);
    }
    @Override
    public void onInitialize() {
    }
}
