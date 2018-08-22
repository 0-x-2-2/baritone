/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.chunk;

import baritone.bot.pathing.movement.MovementHelper;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.Helper;
import baritone.bot.utils.pathing.BetterBlockPos;
import baritone.bot.utils.pathing.PathingBlockType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.BitSet;

/**
 * @author Brady
 * @since 8/3/2018 1:09 AM
 */
public final class ChunkPacker implements Helper {

    private ChunkPacker() {}

    public static BitSet createPackedChunk(Chunk chunk) {
        long start = System.currentTimeMillis();
        BitSet bitSet = new BitSet(CachedChunk.SIZE);
        try {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        int index = CachedChunk.getPositionIndex(x, y, z);
                        boolean[] bits = getPathingBlockType(new BlockPos(x, y, z), chunk.getBlockState(x, y, z)).getBits();
                        bitSet.set(index, bits[0]);
                        bitSet.set(index + 1, bits[1]);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        //System.out.println("Chunk packing took " + (end - start) + "ms for " + chunk.x + "," + chunk.z);
        return bitSet;
    }

    public static String[] createPackedOverview(Chunk chunk) {
        long start = System.currentTimeMillis();
        String[] blockNames = new String[256];
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                int height = chunk.getHeightValue(x, z);
                IBlockState blockState = chunk.getBlockState(x, height, z);
                for(int y = height; y > 0; y--) {
                    blockState = chunk.getBlockState(x, y, z);
                    if(blockState.getBlock() != Blocks.AIR) {
                        break;
                    }
                }
                blockNames[z << 4 | x] = blockState.getBlock().getLocalizedName();
            }
        }
        long end = System.currentTimeMillis();
        return blockNames;
    }

    private static PathingBlockType getPathingBlockType(BlockPos pos, IBlockState state) {
        Block block = state.getBlock();

        if (BlockStateInterface.isWater(block)) {
            return PathingBlockType.WATER;
        }

        if (MovementHelper.avoidWalkingInto(block)) {
            return PathingBlockType.AVOID;
        }

        if (block instanceof BlockAir) {
            return PathingBlockType.AIR;
        }

        return PathingBlockType.SOLID;
    }
}
