package net.danczer.excavator;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class ExcavatorMiningBlock {

    private final static float MaxMiningHardness = 50f; //Obsidian
    private BlockPos miningPos;
    private int miningBlockTick = 0;
    private int previousMiningBlockTick = 0;

    World world;
    boolean isExcavatorLevel;
    Direction miningDir;

    public ExcavatorLogic.MiningStatus miningStatus;
    private final int id;

    public ExcavatorMiningBlock(int id){

        this.id = id;
    }
    public void readNbt(NbtCompound compound) {
        long miningPos = compound.getLong("miningPos_"+id);

        if (miningPos == 0) {
            this.miningPos = null;
        } else {
            this.miningPos = BlockPos.fromLong(miningPos);
        }

        miningBlockTick = compound.getInt("miningTimerTick_"+id);
    }

    public void writeNbt(NbtCompound compound) {
        compound.putLong("miningPos_"+id, miningPos == null ? 0 : miningPos.asLong());
        compound.putInt("miningTimerTick_"+id, miningBlockTick);
    }

    public void setup(BlockPos miningPos, boolean isExcavatorLevel, World world, Direction miningDir){
        if(this.miningPos != null && !this.miningPos.equals(miningPos)){
            reset(world);
        }

        this.miningPos = miningPos;
        this.isExcavatorLevel = isExcavatorLevel;
        this.world = world;
        this.miningDir = miningDir;
    }

    public void tick(ExcavatorDrill drill){
        miningStatus = checkMiningStatus();

        if(miningStatus == ExcavatorLogic.MiningStatus.Mining){
            miningTick(world, drill);
        }
    }

    private ExcavatorLogic.MiningStatus checkMiningStatus() {
        if(isAir(miningPos) || isRailTrack(miningPos)) return ExcavatorLogic.MiningStatus.Idle;
        if(isStopSign(miningPos)) return ExcavatorLogic.MiningStatus.ManualStop;
        if(isStopSign(miningPos.subtract(miningDir.getVector()))) return ExcavatorLogic.MiningStatus.ManualStop;

        var miningPosBehind = miningPos.add(miningDir.getVector());
        var miningPosLeft = miningPos.add(miningDir.rotateYCounterclockwise().getVector());
        var miningPosRight = miningPos.add(miningDir.rotateYClockwise().getVector());
        var miningPosUp = miningPos.up();
        var miningPosDown = miningPos.down();

        if ((miningStatus = checkFluidHazard(miningPos)) != ExcavatorLogic.MiningStatus.Mining) return miningStatus;
        if ((miningStatus = checkFluidHazard(miningPosBehind)) != ExcavatorLogic.MiningStatus.Mining) return miningStatus;
        if ((miningStatus = checkFluidHazard(miningPosLeft)) != ExcavatorLogic.MiningStatus.Mining) return miningStatus;
        if ((miningStatus = checkFluidHazard(miningPosRight)) != ExcavatorLogic.MiningStatus.Mining) return miningStatus;
        if ((miningStatus = checkFluidHazard(miningPosUp)) != ExcavatorLogic.MiningStatus.Mining) return miningStatus;
        if ((miningStatus = checkFluidHazard(miningPosDown)) != ExcavatorLogic.MiningStatus.Mining) return miningStatus;

        if(isExcavatorLevel){
            if (isAir(miningPosBehind)) return ExcavatorLogic.MiningStatus.HazardCliff;
            if (isAir(miningPosDown)) return ExcavatorLogic.MiningStatus.HazardCliff;
        }

        return ExcavatorLogic.MiningStatus.Mining;
    }

    private ExcavatorLogic.MiningStatus checkFluidHazard(BlockPos pos) {
        FluidState fLuidState = world.getBlockState(pos).getFluidState();

        if (!fLuidState.isEmpty()) {
            if (fLuidState.isIn(FluidTags.LAVA)) {
                return ExcavatorLogic.MiningStatus.HazardLava;
            } else if (fLuidState.isIn(FluidTags.WATER)) {
                return ExcavatorLogic.MiningStatus.HazardWater;
            } else {
                return ExcavatorLogic.MiningStatus.HazardUnknownFluid;
            }
        } else {
            return ExcavatorLogic.MiningStatus.Mining;
        }
    }

    private boolean isRailTrack(BlockPos targetPos) {
        BlockState blockState = world.getBlockState(targetPos);

        return blockState.isIn(BlockTags.RAILS);
    }

    private boolean isStopSign(BlockPos blockPos) {
        BlockState blockState = world.getBlockState(blockPos);
        return blockState.isIn(BlockTags.SIGNS) || blockState.isIn(BlockTags.WALL_SIGNS) || blockState.isIn(BlockTags.STANDING_SIGNS);
    }

    private boolean isAir(BlockPos pos) {
        return world.getBlockState(pos).isAir();
    }

    private ExcavatorLogic.MiningStatus miningTick(World world, ExcavatorDrill drill){
        BlockState blockState = world.getBlockState(miningPos);

        float blockHardness = blockState.getHardness(world, miningPos);

        boolean mineAllowed = blockHardness >= 0f && blockHardness < MaxMiningHardness;

        boolean byHand = !blockState.isToolRequired();

        MiningToolItem pickaxeType = drill.getPickAxe();
        MiningToolItem shovelType = drill.getShovel();

        boolean isPickAxe = pickaxeType.isSuitableFor(blockState);
        boolean isShovel = shovelType.isSuitableFor(blockState);

        float pickAxeSpeed = pickaxeType.getMiningSpeedMultiplier(new ItemStack(pickaxeType), blockState);
        float shovelSpeed = shovelType.getMiningSpeedMultiplier(new ItemStack(shovelType), blockState);

        if(isPickAxe && isShovel){
            if(pickAxeSpeed > shovelSpeed){
                isShovel = false;
            }
        }

        if (mineAllowed && (byHand || isPickAxe || isShovel)) {
            miningBlockTick++;

            float miningSpeed = 1.5f;

            if (isPickAxe) {
                world.playSound(0.0, 0.0, 0.0, SoundEvents.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 1.0F, 1.0F, true);
                miningSpeed /= pickAxeSpeed;
            } else if(isShovel){
                world.playSound(0.0, 0.0, 0.0, SoundEvents.ITEM_SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1.0F, 1.0F, true);
                miningSpeed /= shovelSpeed;
            }else{
                world.playSound(0.0, 0.0, 0.0, SoundEvents.ITEM_SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1.0F, 1.0F, true);
            }

            float timeToBreakTheBlock = blockHardness * miningSpeed * ExcavatorMod.TICK_PER_SECOND;

            if (miningBlockTick > previousMiningBlockTick + 5) {
                int progress = Math.min((int) ((miningBlockTick / timeToBreakTheBlock)*10f), 10);

                world.setBlockBreakingInfo(0, miningPos, progress);
                previousMiningBlockTick = miningBlockTick;
            }

            if (miningBlockTick > timeToBreakTheBlock) {
                world.removeBlock(miningPos, true);
                previousMiningBlockTick = 0;
                return ExcavatorLogic.MiningStatus.Idle;
            } else {
                return ExcavatorLogic.MiningStatus.Mining;
            }
        } else {
            return ExcavatorLogic.MiningStatus.UnableToMine;
        }
    }

    public ExcavatorLogic.MiningStatus getStatus(){
        return miningStatus;
    }

    public void reset(World world){
        if (miningPos != null) {
            world.setBlockBreakingInfo(0, miningPos, -1);
        }
        miningPos = null;
        miningBlockTick = 0;
        previousMiningBlockTick = 0;
    }
}
