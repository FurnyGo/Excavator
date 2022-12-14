package net.danczer.excavator;

import net.minecraft.block.*;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ExcavatorLogic {
    public static final int DRILL_COUNT = 3;
    private static final Logger LOGGER = LogManager.getLogger();

    public enum MiningStatus {
        Idle(0),
        Mining(1),
        MissingTool(2),
        InventoryIsFull(3),
        HazardCliff(4),
        HazardLava(5),
        HazardWater(6),
        HazardUnknownFluid(7),
        UnableToMine(8),
        ManualStop(9);

        public final int Value;

        MiningStatus(int value) {
            Value = value;
        }

        public static MiningStatus Find(int value) {
            return switch (value) {
                case 1 -> MiningStatus.Mining;
                case 2 -> MiningStatus.MissingTool;
                case 3 -> MiningStatus.InventoryIsFull;
                case 4 -> MiningStatus.HazardCliff;
                case 5 -> MiningStatus.HazardLava;
                case 6 -> MiningStatus.HazardWater;
                case 7 -> MiningStatus.HazardUnknownFluid;
                case 8 -> MiningStatus.UnableToMine;
                case 9 -> MiningStatus.ManualStop;
                default -> MiningStatus.Idle;
            };
        }
    }

    private final static int TorchPlacementDistance = 6;

    private final World world;

    private final Inventory excavatorInventory;
    private final AbstractMinecartEntity minecartEntity;

    private BlockPos lastTorchPos;
    private Direction miningDir;

    public boolean isForwardFacing;
    private BlockItem railType;
    private BlockItem torchType;
    public final ExcavatorDrill[] drillTypes = new ExcavatorDrill[DRILL_COUNT];
    private final ExcavatorMiningBlock[] miningArea = new ExcavatorMiningBlock[DRILL_COUNT];
    public MiningStatus miningStatus = MiningStatus.Idle;

    public ExcavatorLogic(AbstractMinecartEntity minecartEntity, Inventory inventory) {
        this.minecartEntity = minecartEntity;
        this.excavatorInventory = inventory;
        this.world = minecartEntity.world;

        if(isItemListContainsNull(ExcavatorMod.EXCAVATOR_USABLE_TORCH_ITEMS)){
            throw new NullPointerException("Invalid Torch in the usable list for excavator!");
        }

        if(isItemListContainsNull(ExcavatorMod.EXCAVATOR_USABLE_RAIL_ITEMS)){
            throw new NullPointerException("Invalid Rail in the usable list for excavator!");
        }

        if(isItemListContainsNull(ExcavatorMod.EXCAVATOR_USABLE_DRILL_ITEMS)){
            throw new NullPointerException("Invalid Pickaxe in the usable list for excavator!");
        }

        for (int i = 0; i < DRILL_COUNT; i++) {
            miningArea[i] = new ExcavatorMiningBlock(i);
        }
    }

    private <T extends Item> boolean isItemListContainsNull(List<T> list) {
        for (Object obj: list) {
            if(obj == null) return true;
        }

        return false;
    }

    public void updateExcavatorToolchain() {

        int latestTorchItemIdx = Integer.MAX_VALUE;
        int latestRailItemIdx = Integer.MAX_VALUE;
        int drillTypeIdx = 0;

        torchType = null;
        railType = null;
        for (int i = 0; i < DRILL_COUNT; i++) {
            drillTypes[i] = null;
        }

        for (int i = 0; i < excavatorInventory.size(); i++) {
            ItemStack itemStack = excavatorInventory.getStack(i);
            Item item = itemStack.getItem();

            if(itemStack.isEmpty()) continue;

            if (item instanceof BlockItem) {
                int idx;

                if((idx = ExcavatorMod.EXCAVATOR_USABLE_TORCH_ITEMS.indexOf(item)) >=0 && latestTorchItemIdx > idx){
                    latestTorchItemIdx = idx;
                    torchType = (BlockItem) item;
                }

                if((idx = ExcavatorMod.EXCAVATOR_USABLE_RAIL_ITEMS.indexOf(item)) >=0 && latestRailItemIdx > idx){
                    latestRailItemIdx = idx;
                    railType = (BlockItem) item;
                }
            }else if (item instanceof ExcavatorDrill drill) {
                if(drillTypeIdx < DRILL_COUNT) {
                    drillTypes[drillTypeIdx] = drill;
                    drillTypeIdx++;
                }
            }
        }
    }

    public void readNbt(NbtCompound compound) {
        long torchPos = compound.getLong("lastTorchPos");

        if (torchPos == 0) {
            lastTorchPos = null;
        } else {
            lastTorchPos = BlockPos.fromLong(torchPos);
        }

        int dirIndex = compound.getInt("miningDir");

        if (dirIndex == 0) {
            miningDir = null;
        } else {
            miningDir = Direction.byId(dirIndex);
        }
        isForwardFacing = compound.getBoolean("excavatorFacing");

        for (int i = 0; i < DRILL_COUNT; i++) {
            miningArea[i].readNbt(compound);
        }
    }

    public void writeNbt(NbtCompound compound) {
        compound.putBoolean("excavatorFacing", isForwardFacing);
        compound.putLong("lastTorchPos", lastTorchPos == null ? 0 : lastTorchPos.asLong());
        compound.putInt("miningDir", miningDir == null ? 0 : miningDir.getId());
        for (int i = 0; i < DRILL_COUNT; i++) {
            miningArea[i].writeNbt(compound);
        }
    }

    public Vec3d getFacingDir() {
        if (miningDir == null) return Vec3d.ZERO;

        switch (miningDir) {
            case NORTH:
            case SOUTH:
            case WEST:
            case EAST:
                Vec3d vec = new Vec3d(miningDir.getUnitVector());
                if(!isForwardFacing){
                    vec = vec.multiply(-1);
                }
                vec.normalize();
                return vec;
            case DOWN:
            case UP:
            default:
                return Vec3d.ZERO;
        }
    }

    public boolean isToolchainSet(){
        if(railType == null) return false;

        for (int i = 0; i < DRILL_COUNT; i++) {
            if(drillTypes[i] == null) return false;
        }

        return true;
    }

    public void tick() {
        if (!isToolchainSet()) {
            resetMining();
            miningStatus = MiningStatus.MissingTool;
            return;
        }

        if(isInventoryFull()){
            resetMining();
            miningStatus = MiningStatus.InventoryIsFull;
            return;
        }

        BlockPos minecartPos = minecartEntity.getBlockPos();

        BlockPos miningPos = getMiningPos(minecartPos);

        if (miningPos == null) { //TODO OFF rail status
            resetMining();
            miningStatus = MiningStatus.ManualStop;
        } else {
            for (int i = 0; i < DRILL_COUNT; i++) {
                miningArea[i].setup(miningPos.up(i), i == 0, world, miningDir);
            }
            for (int i = 0; i < DRILL_COUNT; i++) {
                miningArea[i].tick(drillTypes[i]);
            }

            miningStatus = MiningStatus.Idle;
            for (int i = 0; i < DRILL_COUNT; i++) {
                var status = miningArea[i].getStatus();

                if(status.Value > miningStatus.Value)
                {
                    miningStatus = status;
                }
            }

            createRail(miningPos);
            createTorch(miningPos.offset(Direction.UP, 2));
        }
    }

    public boolean isInventoryFull() {
        for (int i = 0; i < excavatorInventory.size(); i++) {
            ItemStack itemStack = excavatorInventory.getStack(i);
            Item item = itemStack.getItem();

            if (isToolchainItem(item)) continue;

            if (itemStack.isEmpty() || itemStack.getCount() < itemStack.getMaxCount()) return false;
        }

        return true;
    }

    private boolean isToolchainItem(Item item) {
        if (item instanceof BlockItem) {
            return ExcavatorMod.EXCAVATOR_USABLE_TORCH_ITEMS.contains(item) || ExcavatorMod.EXCAVATOR_USABLE_RAIL_ITEMS.contains(item);
        }else if (item instanceof ExcavatorDrill) {
            return ExcavatorMod.EXCAVATOR_USABLE_DRILL_ITEMS.contains(item);
        }

        return false;
    }

    private BlockPos getMiningPos(BlockPos pos) {
        RailShape railShape;
        RailShape railShapeAtExcavator = getRailShape(pos);
        RailShape railShapeUnderExcavator = getRailShape(pos.down(1));

        if(railShapeUnderExcavator != null){
            railShape = railShapeUnderExcavator;
        }else{
            railShape = railShapeAtExcavator;
        }

        if(railShape == null){
            return null;
        }

        if(railShape.isAscending()){
            if(railShape == RailShape.ASCENDING_EAST){
                miningDir = isForwardFacing? Direction.EAST : Direction.WEST;
            }else if(railShape == RailShape.ASCENDING_WEST){
                miningDir = isForwardFacing? Direction.WEST : Direction.EAST;
            }else if(railShape == RailShape.ASCENDING_SOUTH){
                miningDir = isForwardFacing? Direction.SOUTH : Direction.NORTH;
            }else if(railShape == RailShape.ASCENDING_NORTH){
                miningDir = isForwardFacing? Direction.NORTH : Direction.SOUTH;
            }
        }else{
            if(railShape == RailShape.EAST_WEST){
                miningDir = isForwardFacing? Direction.EAST : Direction.WEST;
            }else if(railShape == RailShape.NORTH_SOUTH){
                miningDir = isForwardFacing? Direction.NORTH : Direction.SOUTH;
            }if(railShape == RailShape.NORTH_WEST){
                miningDir = isForwardFacing? Direction.NORTH : Direction.WEST;
            }else if(railShape == RailShape.NORTH_EAST){
                miningDir = isForwardFacing? Direction.NORTH : Direction.EAST;
            }else if(railShape == RailShape.SOUTH_EAST){
                miningDir = isForwardFacing? Direction.SOUTH : Direction.EAST;
            }else if(railShape == RailShape.SOUTH_WEST){
                miningDir = isForwardFacing? Direction.SOUTH : Direction.WEST;
            }
        }

        BlockPos resultPos = pos.offset(miningDir);

        if (railShape.isAscending()) {
            resultPos = resultPos.up();
        }

        //LOGGER.debug("excavatorFacing:"+ isForwardFacing +", miningDir:"+miningDir+", railShape:"+railShape+", railShapeAtExcavator:"+railShapeAtExcavator + ", railShapeUnderExcavator:"+railShapeUnderExcavator + ", pos:"+pos+", resultPos:"+resultPos);

        return resultPos;
    }

    private RailShape getRailShape(BlockPos pos){
        if (!isRailTrack(pos)) return null;

        BlockState bs = world.getBlockState(pos);

        AbstractRailBlock railBlock = (AbstractRailBlock) bs.getBlock();

        return bs.get(railBlock.getShapeProperty());
    }

    private boolean isRailTrack(BlockPos targetPos) {
        BlockState blockState = world.getBlockState(targetPos);

        return blockState.isIn(BlockTags.RAILS);
    }

    private void resetMining() {
        miningStatus = MiningStatus.Idle;

        for (int i = 0; i < DRILL_COUNT; i++) {
            miningArea[i].reset(world);
        }
    }

    private void createRail(BlockPos blockPos) {
        if(!isAir(blockPos)) return;
        if (isRailTrack(blockPos) || isRailTrack(blockPos.offset(Direction.DOWN, 1)));

        if (railType != null) {
            if (reduceInventoryItem(railType)) {
                world.setBlockState(blockPos, railType.getBlock().getDefaultState().rotate(getRailRotation()));
            }
        }
    }

    private BlockRotation getRailRotation() {
       return BlockRotation.NONE;
    }

    private void createTorch(BlockPos blockPos) {
        if (torchType == null) return; //optional

        if(!isAir(blockPos)) return;

        BlockState targetBlockState = world.getBlockState(blockPos);

        //find existing torch
        for (BlockItem torch : ExcavatorMod.EXCAVATOR_USABLE_TORCH_ITEMS) {
            Block wallBlock = getTorchWallBlock(torch.getBlock());
            if(targetBlockState.isOf(wallBlock)){
                lastTorchPos = blockPos;
                return;
            }
        }

        if (lastTorchPos != null && lastTorchPos.isWithinDistance(new Vec3i(blockPos.getX(), blockPos.getY(), blockPos.getZ()), TorchPlacementDistance))
            return;

        Direction torchDir = null;
        if (!isAir(blockPos.add(miningDir.rotateYClockwise().getVector()))) {
            torchDir = miningDir.rotateYCounterclockwise();
        } else if (!isAir(blockPos.add(miningDir.rotateYCounterclockwise().getVector()))) {
            torchDir = miningDir.rotateYClockwise();
        }

        //place torch
        if (torchDir != null && reduceInventoryItem(torchType)) {
            Block wallBlock = getTorchWallBlock(torchType.getBlock());
            BlockState wallBlockState = wallBlock.getDefaultState();

            if(wallBlockState.contains(Properties.HORIZONTAL_FACING)){
                world.setBlockState(blockPos, wallBlockState.with(Properties.HORIZONTAL_FACING, torchDir));
            }else{
                world.setBlockState(blockPos, wallBlockState);
            }

            lastTorchPos = blockPos;
        }
    }

    private boolean isAir(BlockPos pos) {
        return world.getBlockState(pos).isAir();
    }

    private Block getTorchWallBlock(Block block){
        for (Block wallBlock: ExcavatorMod.EXCAVATOR_TORCH_WALL_BLOCKS) {
            if(block.getClass().isAssignableFrom(wallBlock.getClass())){
                return wallBlock;
            }
        }

        return block;
    }

    private boolean reduceInventoryItem(Item item) {
        for (int i = 0; i < excavatorInventory.size(); i++) {
            ItemStack itemStack = excavatorInventory.getStack(i);

            if (!itemStack.isEmpty() && itemStack.getItem() == item) {
                itemStack.split(1);
                return true;
            }
        }

        return false;
    }
}