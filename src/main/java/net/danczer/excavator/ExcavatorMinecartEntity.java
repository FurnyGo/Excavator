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
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;

import java.util.List;

public class ExcavatorMinecartEntity extends StorageMinecartEntity implements Hopper {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TrackedData<Integer> MINING_STATUS;

    static {
        MINING_STATUS = DataTracker.registerData(ExcavatorMinecartEntity.class, TrackedDataHandlerRegistry.INTEGER);
    }
    private static final float CollectBlockWithHardness = 3f;
    private static final float MinecartPushForce = 0.005f;

    private final ExcavationLogic excavationLogic = new ExcavationLogic(this, this);
    private boolean enabled = true;
    private int transferTicker = -1;
    private final BlockPos lastPosition = BlockPos.ORIGIN;

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

    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(MINING_STATUS, 0);
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
            excavationLogic.updateExcavatorToolchain();

            excavationLogic.tick();

            setMiningStatus(excavationLogic.miningStatus);
        }

        if (this.random.nextInt(4) == 0) {
            showMiningStatus();
        }
    }

    private void showMiningStatus() {
        ExcavationLogic.MiningStatus miningStatus = getMiningStatus();

        ParticleEffect particleType = null;

        switch (miningStatus) {
            case Mining:
                particleType = ParticleTypes.LARGE_SMOKE;
                break;
            case HazardCliff:
                particleType = ParticleTypes.ENTITY_EFFECT;
                break;
            case HazardLava:
                particleType = ParticleTypes.FALLING_LAVA;
                break;
            case HazardWater:
                particleType = ParticleTypes.FALLING_WATER;
                break;
            case HazardUnknownFluid:
                particleType = ParticleTypes.BUBBLE;
                break;
            case MissingToolchain:
                particleType = ParticleTypes.WITCH;
                break;
            case InventoryIsFull:
                particleType = ParticleTypes.FALLING_HONEY;
                break;
            case EmergencyStop:
                particleType = ParticleTypes.COMPOSTER;
                break;
            case Rolling:
            default:
                break;
        }

        if (particleType != null) {
            this.world.addParticle(particleType, this.getHopperX(), this.getHopperY() + 0.8D, this.getHopperZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    private ExcavationLogic.MiningStatus getMiningStatus() {
        return ExcavationLogic.MiningStatus.Find(getDataTracker().get(MINING_STATUS));
    }

    private void setMiningStatus(ExcavationLogic.MiningStatus miningStatus) {
        getDataTracker().set(MINING_STATUS, miningStatus.Value);
    }

    public boolean isForwardFacing(){
        return excavationLogic.isForwardFacing;
    }

    private void setForwardFacing(boolean isForwardFacing){
        excavationLogic.isForwardFacing = isForwardFacing;
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
        excavationLogic.writeNbt(compound);

        compound.putInt("TransferCooldown", this.transferTicker);
        compound.putBoolean("Enabled", this.enabled);

        return super.writeNbt(compound);
    }

    @Override
    public void readNbt(NbtCompound compound) {
        super.readNbt(compound);

        excavationLogic.readNbt(compound);

        this.transferTicker = compound.getInt("TransferCooldown");
        this.enabled = !compound.contains("Enabled") || compound.getBoolean("Enabled");
    }
}
