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

package baritone.pathing.movement.movements;

import baritone.pathing.movement.*;
import baritone.utils.BlockStateInterface;
import baritone.utils.pathing.BetterBlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

public class MovementDownward extends Movement {

    private int numTicks = 0;

    public MovementDownward(BetterBlockPos start, BetterBlockPos end) {
        super(start, end, new BetterBlockPos[]{end});
    }

    @Override
    public void reset() {
        super.reset();
        numTicks = 0;
    }

    @Override
    protected double calculateCost(CalculationContext context) {
        return cost(context, src.x, src.y, src.z);
    }

    public static double cost(CalculationContext context, int x, int y, int z) {
        if (!MovementHelper.canWalkOn(x, y - 2, z)) {
            return COST_INF;
        }
        IBlockState d = BlockStateInterface.get(x, y - 1, z);
        Block td = d.getBlock();
        boolean ladder = td == Blocks.LADDER || td == Blocks.VINE;
        if (ladder) {
            return LADDER_DOWN_ONE_COST;
        } else {
            // we're standing on it, while it might be block falling, it'll be air by the time we get here in the movement
            return FALL_N_BLOCKS_COST[1] + MovementHelper.getMiningDurationTicks(context, x, y - 1, z, d, false);
        }
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        if (playerFeet().equals(dest)) {
            return state.setStatus(MovementStatus.SUCCESS);
        }
        double diffX = player().posX - (dest.getX() + 0.5);
        double diffZ = player().posZ - (dest.getZ() + 0.5);
        double ab = Math.sqrt(diffX * diffX + diffZ * diffZ);

        if (numTicks++ < 10 && ab < 0.2) {
            return state;
        }
        MovementHelper.moveTowards(state, positionsToBreak[0]);
        return state;
    }
}
