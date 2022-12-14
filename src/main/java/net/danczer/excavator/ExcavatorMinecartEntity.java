package net.danczer.excavator;

import com.mojang.logging.LogUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ExcavatorMinecartEntity extends StorageMinecartEntity implements Hopper {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TrackedData<Integer> MINING_STATUS;
    private static final List<TrackedData<Integer>> DRILL_TYPES;

    static {
        MINING_STATUS = DataTracker.registerData(ExcavatorMinecartEntity.class, TrackedDataHandlerRegistry.INTEGER);

        DRILL_TYPES = new ArrayList<>();
        for (int i = 0; i < ExcavatorLogic.DRILL_COUNT; i++) {
            DRILL_TYPES.add(DataTracker.registerData(ExcavatorMinecartEntity.class, TrackedDataHandlerRegistry.INTEGER));
        }
    }

    private static final float CollectBlockWithHardness = 3f;
    private final ExcavatorLogic excavatorLogic = new ExcavatorLogic(this, this);
    private boolean enabled = true;
    private int transferTicker = -1;
    private final BlockPos lastPosition = BlockPos.ORIGIN;
    private final Vec3f[] drillColors = new Vec3f[ExcavatorLogic.DRILL_COUNT];

    public ExcavatorMinecartEntity(EntityType<? extends StorageMinecartEntity> entityType, World world) {
        super(entityType, world);
        LOGGER.debug("ExcavatorMinecartEntity1:"+isForwardFacing());
    }

    private ExcavatorMinecartEntity(World world, Vec3d pos, boolean isForwardFacing) {
        super(ExcavatorMod.EXCAVATOR_ENTITY, pos.x, pos.y, pos.z, world);

        setForwardFacing(isForwardFacing);
        LOGGER.debug("ExcavatorMinecartEntity2:"+isForwardFacing());
    }

    public static ExcavatorMinecartEntity create(World world, Vec3d pos, RailShape railShape, Direction playerFacing){
        var vector = playerFacing.getVector();
        var player2DFacing = Direction.getFacing(vector.getX(), 0, vector.getZ());
        boolean isForwardFacing = false;

        if(railShape.isAscending()){
            if(railShape == RailShape.ASCENDING_EAST){
                isForwardFacing = player2DFacing == Direction.EAST;
            }else if(railShape == RailShape.ASCENDING_WEST){
                isForwardFacing = player2DFacing == Direction.WEST;
            }else if(railShape == RailShape.ASCENDING_SOUTH){
                isForwardFacing = player2DFacing == Direction.SOUTH;
            }else if(railShape == RailShape.ASCENDING_NORTH){
                isForwardFacing = player2DFacing == Direction.NORTH;
            }
        }else{
            if(railShape == RailShape.EAST_WEST){
                isForwardFacing = player2DFacing == Direction.EAST;
            }else if(railShape == RailShape.NORTH_SOUTH){
                isForwardFacing = player2DFacing == Direction.NORTH;
            }if(railShape == RailShape.NORTH_WEST){
                isForwardFacing = player2DFacing == Direction.NORTH;
            }else if(railShape == RailShape.NORTH_EAST){
                isForwardFacing = player2DFacing == Direction.NORTH;
            }else if(railShape == RailShape.SOUTH_EAST){
                isForwardFacing = player2DFacing == Direction.SOUTH;
            }else if(railShape == RailShape.SOUTH_WEST){
                isForwardFacing = player2DFacing == Direction.SOUTH;
            }
        }

        LOGGER.debug("isForwardFacing:"+isForwardFacing+", playerFacing:"+playerFacing+", player2DFacing:"+player2DFacing+", railShape:"+railShape);

        return new ExcavatorMinecartEntity(world, pos, isForwardFacing);
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);

        if(packet instanceof ExcavatorEntitySpawnS2CPacket excavatorPacket){
            setForwardFacing(excavatorPacket.isForwardFacing());
        }
    }

    @Override
    public Packet<?> createSpawnPacket() {
        return new ExcavatorEntitySpawnS2CPacket(this);
    }

    public AbstractMinecartEntity.Type getMinecartType() {
        return null;
    }

    protected Item getItem() {
        return ExcavatorMod.EXCAVATOR_MINECART_ITEM;
    }

    public ItemStack getPickBlockStack()
    {
        return new ItemStack(getItem());
    }

    public BlockState getDefaultContainedBlock() {
        return Blocks.REDSTONE_BLOCK.getDefaultState();
    }

    public int getDefaultBlockOffset() {
        return 1;
    }

    public int size() {
        return ExcavatorScreenHandler.InventorySize;
    }

    public ExcavatorScreenHandler getScreenHandler(int id, PlayerInventory playerInventoryIn) {
        return new ExcavatorScreenHandler(id, playerInventoryIn, this);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public World getWorld() {
        return this.world;
    }

    public double getHopperX() {
        return this.getX();
    }

    public double getHopperY() {
        return this.getY() + 0.5D;
    }

    public double getHopperZ() {
        return this.getZ();
    }

    public void onActivatorRail(int x, int y, int z, boolean powered) {
        boolean bl = !powered;
        if (bl != this.isEnabled()) {
            this.setEnabled(bl);
        }
    }

    public void tick() {
        excavatorTick();
        super.tick();
        hopperTick();
    }

    private void excavatorTick() {
        if (!this.world.isClient && this.isAlive() && this.isEnabled()) {
            excavatorLogic.updateExcavatorToolchain();

            excavatorLogic.tick();

            setMiningStatus();
            setDrillTypes();
        }
    }

    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(MINING_STATUS, 0);

        for (int i = 0; i < ExcavatorLogic.DRILL_COUNT; i++) {
            dataTracker.startTracking(DRILL_TYPES.get(i), -1);
        }
    }

    public ExcavatorLogic.MiningStatus getMiningStatus() {
        return ExcavatorLogic.MiningStatus.Find(dataTracker.get(MINING_STATUS));
    }

    private void setMiningStatus() {
        dataTracker.set(MINING_STATUS, excavatorLogic.miningStatus.Value);
    }

    private ExcavatorDrill[] getDrillTypes() {
        var drills = new ExcavatorDrill[ExcavatorLogic.DRILL_COUNT];

        for (int i = 0; i < ExcavatorLogic.DRILL_COUNT; i++) {
            var drillIndex = dataTracker.get(DRILL_TYPES.get(i));

            if(drillIndex>=0 && drillIndex<ExcavatorMod.EXCAVATOR_USABLE_DRILL_ITEMS.size()){
                drills[i] = ExcavatorMod.EXCAVATOR_USABLE_DRILL_ITEMS.get(drillIndex);
            }else{
                drills[i] = null;
            }
        }

        return drills;
    }

    private void setDrillTypes() {
        for (int i = 0; i < ExcavatorLogic.DRILL_COUNT; i++) {
            var idx = -1;
            if(excavatorLogic.drillTypes[i] != null){
                idx = ExcavatorMod.EXCAVATOR_USABLE_DRILL_ITEMS.indexOf(excavatorLogic.drillTypes[i]);
            }
            getDataTracker().set(DRILL_TYPES.get(i), idx);
        }
    }

    public boolean isForwardFacing(){
        return excavatorLogic.isForwardFacing;
    }

    private void setForwardFacing(boolean isForwardFacing){
        excavatorLogic.isForwardFacing = isForwardFacing;
    }

    public Vec3f[] getDrillColors(){
        var drills = getDrillTypes();

        for (int i = 0; i < ExcavatorLogic.DRILL_COUNT; i++) {
            var drill = drills[i];
            if(drill == null){
                drillColors[i] = null;
            }else{
                drillColors[i] = drill.getDrillColor();
            }
        }

        return drillColors;
    }

    private void hopperTick() {
        if (!this.world.isClient && this.isAlive() && this.isEnabled()) {
            BlockPos blockpos = this.getBlockPos();
            if (blockpos.equals(this.lastPosition)) {
                --this.transferTicker;
            } else {
                this.setTransferTicker(0);
            }

            if (!this.canTransfer()) {
                this.setTransferTicker(0);
                if (this.captureDroppedItems()) {
                    this.setTransferTicker(4);
                    this.markDirty();
                }
            }
        }
    }

    public void setTransferTicker(int transferTickerIn) {
        this.transferTicker = transferTickerIn;
    }

    /**
     * Returns whether the hopper cart can currently transfer an item.
     */
    public boolean canTransfer() {
        return this.transferTicker > 0;
    }

    public boolean captureDroppedItems() {
        List<ItemEntity> list = this.world.getEntitiesByClass(ItemEntity.class, this.getBoundingBox().expand(0.25, 0.0, 0.25), EntityPredicates.VALID_ENTITY);

        for (ItemEntity itemEntity : list) {
            Item item = itemEntity.getStack().getItem();

            //collect only usefull blocks
            if (item instanceof BlockItem blockItem) {
                if (shouldCollectItem(blockItem)) {
                    HopperBlockEntity.extract(this, itemEntity);
                }
            } else {
                HopperBlockEntity.extract(this, itemEntity);
            }
        }

        return false;
    }

    private boolean shouldCollectItem(BlockItem blockItem){
        BlockState blockState = blockItem.getBlock().getDefaultState();

        return blockState.isToolRequired() && blockState.getHardness(world, getBlockPos()) >= CollectBlockWithHardness;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound compound) {
        excavatorLogic.writeNbt(compound);

        compound.putInt("TransferCooldown", this.transferTicker);
        compound.putBoolean("Enabled", this.enabled);

        return super.writeNbt(compound);
    }

    @Override
    public void readNbt(NbtCompound compound) {
        super.readNbt(compound);

        excavatorLogic.readNbt(compound);

        this.transferTicker = compound.getInt("TransferCooldown");
        this.enabled = !compound.contains("Enabled") || compound.getBoolean("Enabled");
    }
}
