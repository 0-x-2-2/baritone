/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.pathing.movement;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.*;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.MovementState.MovementTarget;
import baritone.utils.BlockStateInterface;
import baritone.utils.Helper;
import baritone.utils.ToolSet;
import net.minecraft.block.*;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

import static baritone.pathing.movement.Movement.HORIZONTALS_BUT_ALSO_DOWN____SO_EVERY_DIRECTION_EXCEPT_UP;

/**
 * Static helpers for cost calculation
 *
 * @author leijurv
 */
public interface MovementHelper extends ActionCosts, Helper {

    static boolean avoidBreaking(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        Block b = state.getBlock();
        return b == Blocks.ICE // ice becomes water, and water can mess up the path
                || b instanceof BlockSilverfish // obvious reasons
                // call context.get directly with x,y,z. no need to make 5 new BlockPos for no reason
                || bsi.get0(x, y + 1, z).getBlock() instanceof BlockLiquid//don't break anything touching liquid on any side
                || bsi.get0(x + 1, y, z).getBlock() instanceof BlockLiquid
                || bsi.get0(x - 1, y, z).getBlock() instanceof BlockLiquid
                || bsi.get0(x, y, z + 1).getBlock() instanceof BlockLiquid
                || bsi.get0(x, y, z - 1).getBlock() instanceof BlockLiquid;
    }

    static boolean canWalkThrough(IPlayerContext ctx, BetterBlockPos pos) {
        return canWalkThrough(new BlockStateInterface(ctx), pos.x, pos.y, pos.z);
    }

    static boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z) {
        return canWalkThrough(bsi, x, y, z, bsi.get0(x, y, z));
    }

    static boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.AIR) { // early return for most common case
            return true;
        }
        if (block == Blocks.FIRE || block == Blocks.TRIPWIRE || block == Blocks.WEB || block == Blocks.END_PORTAL || block == Blocks.COCOA || block instanceof BlockSkull) {
            return false;
        }
        if (block instanceof BlockDoor || block instanceof BlockFenceGate) {
            // Because there's no nice method in vanilla to check if a door is openable or not, we just have to assume
            // that anything that isn't an iron door isn't openable, ignoring that some doors introduced in mods can't
            // be opened by just interacting.
            return block != Blocks.IRON_DOOR;
        }
        boolean snow = block instanceof BlockSnow;
        boolean trapdoor = block instanceof BlockTrapDoor;
        if (snow || trapdoor) {
            // we've already checked doors and fence gates
            // so the only remaining dynamic isPassables are snow and trapdoor
            // if they're cached as a top block, we don't know their metadata
            // default to true (mostly because it would otherwise make long distance pathing through snowy biomes impossible)
            if (!bsi.worldContainsLoadedChunk(x, z)) {
                return true;
            }
            if (snow) {
                // the check in BlockSnow.isPassable is layers < 5
                // while actually, we want < 3 because 3 or greater makes it impassable in a 2 high ceiling
                if (state.getValue(BlockSnow.LAYERS) >= 3) {
                    return false;
                }
                // ok, it's low enough we could walk through it, but is it supported?
                return canWalkOn(bsi, x, y - 1, z);
            }
            if (trapdoor) {
                return !state.getValue(BlockTrapDoor.OPEN); // see BlockTrapDoor.isPassable
            }
            throw new IllegalStateException();
        }
        if (isFlowing(x, y, z, state, bsi)) {
            return false; // Don't walk through flowing liquids
        }
        if (block instanceof BlockLiquid) {
            if (Baritone.settings().assumeWalkOnWater.value) {
                return false;
            }
            IBlockState up = bsi.get0(x, y + 1, z);
            if (up.getBlock() instanceof BlockLiquid || up.getBlock() instanceof BlockLilyPad) {
                return false;
            }
            return block == Blocks.WATER || block == Blocks.FLOWING_WATER;
        }
        // every block that overrides isPassable with anything more complicated than a "return true;" or "return false;"
        // has already been accounted for above
        // therefore it's safe to not construct a blockpos from our x, y, z ints and instead just pass null
        return block.isPassable(null, null);
    }

    /**
     * canWalkThrough but also won't impede movement at all. so not including doors or fence gates (we'd have to right click),
     * not including water, and not including ladders or vines or cobwebs (they slow us down)
     *
     * @param context Calculation context to provide block state lookup
     * @param x       The block's x position
     * @param y       The block's y position
     * @param z       The block's z position
     * @return Whether or not the block at the specified position
     */
    static boolean fullyPassable(CalculationContext context, int x, int y, int z) {
        return fullyPassable(context.get(x, y, z));
    }

    static boolean fullyPassable(IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.AIR) { // early return for most common case
            return true;
        }
        // exceptions - blocks that are isPassable true, but we can't actually jump through
        if (block == Blocks.FIRE
                || block == Blocks.TRIPWIRE
                || block == Blocks.WEB
                || block == Blocks.VINE
                || block == Blocks.LADDER
                || block == Blocks.COCOA
                || block instanceof BlockDoor
                || block instanceof BlockFenceGate
                || block instanceof BlockSnow
                || block instanceof BlockLiquid
                || block instanceof BlockTrapDoor
                || block instanceof BlockEndPortal
                || block instanceof BlockSkull) {
            return false;
        }
        // door, fence gate, liquid, trapdoor have been accounted for, nothing else uses the world or pos parameters
        return block.isPassable(null, null);
    }

    static boolean isReplacable(int x, int y, int z, IBlockState state, BlockStateInterface bsi) {
        // for MovementTraverse and MovementAscend
        // block double plant defaults to true when the block doesn't match, so don't need to check that case
        // all other overrides just return true or false
        // the only case to deal with is snow
        /*
         *  public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos)
         *     {
         *         return ((Integer)worldIn.getBlockState(pos).getValue(LAYERS)).intValue() == 1;
         *     }
         */
        Block block = state.getBlock();
        if (block == Blocks.AIR || isWater(block)) {
            // early return for common cases hehe
            return true;
        }
        if (block instanceof BlockSnow) {
            // as before, default to true (mostly because it would otherwise make long distance pathing through snowy biomes impossible)
            if (!bsi.worldContainsLoadedChunk(x, z)) {
                return true;
            }
            return state.getValue(BlockSnow.LAYERS) == 1;
        }
        if (block instanceof BlockDoublePlant) {
            BlockDoublePlant.EnumPlantType kek = state.getValue(BlockDoublePlant.VARIANT);
            return kek == BlockDoublePlant.EnumPlantType.FERN || kek == BlockDoublePlant.EnumPlantType.GRASS;
        }
        return state.getMaterial().isReplaceable();
    }

    static boolean isDoorPassable(IPlayerContext ctx, BlockPos doorPos, BlockPos playerPos) {
        if (playerPos.equals(doorPos)) {
            return false;
        }

        IBlockState state = BlockStateInterface.get(ctx, doorPos);
        if (!(state.getBlock() instanceof BlockDoor)) {
            return true;
        }

        return isHorizontalBlockPassable(doorPos, state, playerPos, BlockDoor.OPEN);
    }

    static boolean isGatePassable(IPlayerContext ctx, BlockPos gatePos, BlockPos playerPos) {
        if (playerPos.equals(gatePos)) {
            return false;
        }

        IBlockState state = BlockStateInterface.get(ctx, gatePos);
        if (!(state.getBlock() instanceof BlockFenceGate)) {
            return true;
        }

        return state.getValue(BlockFenceGate.OPEN);
    }

    static boolean isHorizontalBlockPassable(BlockPos blockPos, IBlockState blockState, BlockPos playerPos, PropertyBool propertyOpen) {
        if (playerPos.equals(blockPos)) {
            return false;
        }

        EnumFacing.Axis facing = blockState.getValue(BlockHorizontal.FACING).getAxis();
        boolean open = blockState.getValue(propertyOpen);

        EnumFacing.Axis playerFacing;
        if (playerPos.north().equals(blockPos) || playerPos.south().equals(blockPos)) {
            playerFacing = EnumFacing.Axis.Z;
        } else if (playerPos.east().equals(blockPos) || playerPos.west().equals(blockPos)) {
            playerFacing = EnumFacing.Axis.X;
        } else {
            return true;
        }

        return (facing == playerFacing) == open;
    }

    static boolean avoidWalkingInto(Block block) {
        return block instanceof BlockLiquid
                || block instanceof BlockDynamicLiquid
                || block == Blocks.MAGMA
                || block == Blocks.CACTUS
                || block == Blocks.FIRE
                || block == Blocks.END_PORTAL
                || block == Blocks.WEB;
    }

    /**
     * Can I walk on this block without anything weird happening like me falling
     * through? Includes water because we know that we automatically jump on
     * water
     *
     * @param bsi   Block state provider
     * @param x     The block's x position
     * @param y     The block's y position
     * @param z     The block's z position
     * @param state The state of the block at the specified location
     * @return Whether or not the specified block can be walked on
     */
    static boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.AIR || block == Blocks.MAGMA) {
            // early return for most common case (air)
            // plus magma, which is a normal cube but it hurts you
            return false;
        }
        if (state.isBlockNormalCube()) {
            return true;
        }
        if (block == Blocks.LADDER || (block == Blocks.VINE && Baritone.settings().allowVines.value)) { // TODO reconsider this
            return true;
        }
        if (block == Blocks.FARMLAND || block == Blocks.GRASS_PATH) {
            return true;
        }
        if (block == Blocks.ENDER_CHEST || block == Blocks.CHEST) {
            return true;
        }
        if (isWater(block)) {
            // since this is called literally millions of times per second, the benefit of not allocating millions of useless "pos.up()"
            // BlockPos s that we'd just garbage collect immediately is actually noticeable. I don't even think its a decrease in readability
            Block up = bsi.get0(x, y + 1, z).getBlock();
            if (up == Blocks.WATERLILY || up == Blocks.CARPET) {
                return true;
            }
            if (isFlowing(x, y, z, state, bsi) || block == Blocks.FLOWING_WATER) {
                // the only scenario in which we can walk on flowing water is if it's under still water with jesus off
                return isWater(up) && !Baritone.settings().assumeWalkOnWater.value;
            }
            // if assumeWalkOnWater is on, we can only walk on water if there isn't water above it
            // if assumeWalkOnWater is off, we can only walk on water if there is water above it
            return isWater(up) ^ Baritone.settings().assumeWalkOnWater.value;
        }
        if (block == Blocks.GLASS || block == Blocks.STAINED_GLASS) {
            return true;
        }
        if (block instanceof BlockSlab) {
            if (!Baritone.settings().allowWalkOnBottomSlab.value) {
                if (((BlockSlab) block).isDouble()) {
                    return true;
                }
                return state.getValue(BlockSlab.HALF) != BlockSlab.EnumBlockHalf.BOTTOM;
            }
            return true;
        }
        return block instanceof BlockStairs;
    }

    static boolean canWalkOn(IPlayerContext ctx, BetterBlockPos pos, IBlockState state) {
        return canWalkOn(new BlockStateInterface(ctx), pos.x, pos.y, pos.z, state);
    }

    static boolean canWalkOn(IPlayerContext ctx, BetterBlockPos pos) {
        return canWalkOn(new BlockStateInterface(ctx), pos.x, pos.y, pos.z);
    }

    static boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z) {
        return canWalkOn(bsi, x, y, z, bsi.get0(x, y, z));
    }

    static boolean canPlaceAgainst(BlockStateInterface bsi, int x, int y, int z) {
        return canPlaceAgainst(bsi.get0(x, y, z));
    }

    static boolean canPlaceAgainst(BlockStateInterface bsi, BlockPos pos) {
        return canPlaceAgainst(bsi.get0(pos.getX(), pos.getY(), pos.getZ()));
    }

    static boolean canPlaceAgainst(IPlayerContext ctx, BlockPos pos) {
        return canPlaceAgainst(new BlockStateInterface(ctx), pos);
    }

    static boolean canPlaceAgainst(IBlockState state) {
        // TODO isBlockNormalCube isn't the best check for whether or not we can place a block against it. e.g. glass isn't normalCube but we can place against it
        return state.isBlockNormalCube();
    }

    static double getMiningDurationTicks(CalculationContext context, int x, int y, int z, boolean includeFalling) {
        return getMiningDurationTicks(context, x, y, z, context.get(x, y, z), includeFalling);
    }

    static double getMiningDurationTicks(CalculationContext context, int x, int y, int z, IBlockState state, boolean includeFalling) {
        Block block = state.getBlock();
        if (!canWalkThrough(context.bsi, x, y, z, state)) {
            if (!context.canBreakAt(x, y, z)) {
                return COST_INF;
            }
            if (avoidBreaking(context.bsi, x, y, z, state)) {
                return COST_INF;
            }
            if (block instanceof BlockLiquid) {
                return COST_INF;
            }
            double m = Blocks.CRAFTING_TABLE.equals(block) ? 10 : 1; // TODO see if this is still necessary. it's from MineBot when we wanted to penalize breaking its crafting table
            double strVsBlock = context.toolSet.getStrVsBlock(state);
            if (strVsBlock <= 0) {
                return COST_INF;
            }

            double result = m / strVsBlock;
            result += context.breakBlockAdditionalCost;
            if (includeFalling) {
                IBlockState above = context.get(x, y + 1, z);
                if (above.getBlock() instanceof BlockFalling) {
                    result += getMiningDurationTicks(context, x, y + 1, z, above, true);
                }
            }
            return result;
        }
        return 0; // we won't actually mine it, so don't check fallings above
    }

    static boolean isBottomSlab(IBlockState state) {
        return state.getBlock() instanceof BlockSlab
                && !((BlockSlab) state.getBlock()).isDouble()
                && state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM;
    }

    /**
     * AutoTool for a specific block
     *
     * @param ctx The player context
     * @param b   the blockstate to mine
     */
    static void switchToBestToolFor(IPlayerContext ctx, IBlockState b) {
        switchToBestToolFor(ctx, b, new ToolSet(ctx.player()));
    }

    /**
     * AutoTool for a specific block with precomputed ToolSet data
     *
     * @param ctx The player context
     * @param b   the blockstate to mine
     * @param ts  previously calculated ToolSet
     */
    static void switchToBestToolFor(IPlayerContext ctx, IBlockState b, ToolSet ts) {
        ctx.player().inventory.currentItem = ts.getBestSlot(b.getBlock());
    }

    static boolean throwaway(IPlayerContext ctx, boolean select) {
        EntityPlayerSP p = ctx.player();
        NonNullList<ItemStack> inv = p.inventory.mainInventory;
        for (byte i = 0; i < 9; i++) {
            ItemStack item = inv.get(i);
            // this usage of settings() is okay because it's only called once during pathing
            // (while creating the CalculationContext at the very beginning)
            // and then it's called during execution
            // since this function is never called during cost calculation, we don't need to migrate
            // acceptableThrowawayItems to the CalculationContext
            if (Baritone.settings().acceptableThrowawayItems.value.contains(item.getItem())) {
                if (select) {
                    p.inventory.currentItem = i;
                }
                return true;
            }
        }
        if (Baritone.settings().acceptableThrowawayItems.value.contains(p.inventory.offHandInventory.get(0).getItem())) {
            // main hand takes precedence over off hand
            // that means that if we have block A selected in main hand and block B in off hand, right clicking places block B
            // we've already checked above ^ and the main hand can't possible have an acceptablethrowawayitem
            // so we need to select in the main hand something that doesn't right click
            // so not a shovel, not a hoe, not a block, etc
            for (byte i = 0; i < 9; i++) {
                ItemStack item = inv.get(i);
                if (item.isEmpty() || item.getItem() instanceof ItemPickaxe) {
                    if (select) {
                        p.inventory.currentItem = i;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    static void moveTowards(IPlayerContext ctx, MovementState state, BlockPos pos) {
        state.setTarget(new MovementTarget(
                new Rotation(RotationUtils.calcRotationFromVec3d(ctx.playerHead(),
                        VecUtils.getBlockPosCenter(pos),
                        ctx.playerRotations()).getYaw(), ctx.player().rotationPitch),
                false
        )).setInput(Input.MOVE_FORWARD, true);
    }

    /**
     * Returns whether or not the specified block is
     * water, regardless of whether or not it is flowing.
     *
     * @param b The block
     * @return Whether or not the block is water
     */
    static boolean isWater(Block b) {
        return b == Blocks.FLOWING_WATER || b == Blocks.WATER;
    }

    /**
     * Returns whether or not the block at the specified pos is
     * water, regardless of whether or not it is flowing.
     *
     * @param ctx The player context
     * @param bp  The block pos
     * @return Whether or not the block is water
     */
    static boolean isWater(IPlayerContext ctx, BlockPos bp) {
        return isWater(BlockStateInterface.getBlock(ctx, bp));
    }

    static boolean isLava(Block b) {
        return b == Blocks.FLOWING_LAVA || b == Blocks.LAVA;
    }

    /**
     * Returns whether or not the specified pos has a liquid
     *
     * @param ctx The player context
     * @param p   The pos
     * @return Whether or not the block is a liquid
     */
    static boolean isLiquid(IPlayerContext ctx, BlockPos p) {
        return BlockStateInterface.getBlock(ctx, p) instanceof BlockLiquid;
    }

    static boolean possiblyFlowing(IBlockState state) {
        // Will be IFluidState in 1.13
        return state.getBlock() instanceof BlockLiquid
                && state.getValue(BlockLiquid.LEVEL) != 0;
    }

    static boolean isFlowing(int x, int y, int z, IBlockState state, BlockStateInterface bsi) {
        if (!(state.getBlock() instanceof BlockLiquid)) {
            return false;
        }
        if (state.getValue(BlockLiquid.LEVEL) != 0) {
            return true;
        }
        return possiblyFlowing(bsi.get0(x + 1, y, z))
                || possiblyFlowing(bsi.get0(x - 1, y, z))
                || possiblyFlowing(bsi.get0(x, y, z + 1))
                || possiblyFlowing(bsi.get0(x, y, z - 1));
    }


    static PlaceResult attemptToPlaceABlock(MovementState state, IBaritone baritone, BlockPos placeAt, boolean preferDown) {
        IPlayerContext ctx = baritone.getPlayerContext();
        Optional<Rotation> direct = RotationUtils.reachable(ctx, placeAt); // we assume that if there is a block there, it must be replacable
        boolean found = false;
        if (direct.isPresent()) {
            state.setTarget(new MovementState.MovementTarget(direct.get(), true));
            found = true;
        }
        for (int i = 0; i < 5; i++) {
            BlockPos against1 = placeAt.offset(HORIZONTALS_BUT_ALSO_DOWN____SO_EVERY_DIRECTION_EXCEPT_UP[i]);
            if (MovementHelper.canPlaceAgainst(ctx, against1)) {
                //if (!((Baritone) baritone).getInventoryBehavior().selectThrowawayForLocation(placeAt.getX(), placeAt.getY(), placeAt.getZ())) { // get ready to place a throwaway block
                if (!throwaway(ctx, true)) {
                    Helper.HELPER.logDebug("bb pls get me some blocks. dirt or cobble");
                    state.setStatus(MovementStatus.UNREACHABLE);
                    return PlaceResult.NO_OPTION;
                }
                double faceX = (placeAt.getX() + against1.getX() + 1.0D) * 0.5D;
                double faceY = (placeAt.getY() + against1.getY() + 1.0D) * 0.5D;
                double faceZ = (placeAt.getZ() + against1.getZ() + 1.0D) * 0.5D;
                Rotation place = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), new Vec3d(faceX, faceY, faceZ), ctx.playerRotations());
                RayTraceResult res = RayTraceUtils.rayTraceTowards(ctx.player(), place, ctx.playerController().getBlockReachDistance());
                if (res != null && res.typeOfHit == RayTraceResult.Type.BLOCK && res.getBlockPos().equals(against1) && res.getBlockPos().offset(res.sideHit).equals(placeAt)) {
                    state.setTarget(new MovementState.MovementTarget(place, true));
                    found = true;

                    if (!preferDown) {
                        // if preferDown is true, we want the last option
                        // if preferDown is false, we want the first
                        break;
                    }
                }
            }
        }
        if (ctx.getSelectedBlock().isPresent()) {
            BlockPos selectedBlock = ctx.getSelectedBlock().get();
            EnumFacing side = ctx.objectMouseOver().sideHit;
            // only way for selectedBlock.equals(placeAt) to be true is if it's replacable
            if (selectedBlock.equals(placeAt) || (MovementHelper.canPlaceAgainst(ctx, selectedBlock) && selectedBlock.offset(side).equals(placeAt))) {
                return PlaceResult.READY_TO_PLACE;
            }
        }
        return found ? PlaceResult.ATTEMPTING : PlaceResult.NO_OPTION;
    }

    enum PlaceResult {
        READY_TO_PLACE, ATTEMPTING, NO_OPTION;
    }
}
