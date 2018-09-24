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

package baritone.utils;

import baritone.Baritone;
import baritone.api.cache.ICachedRegion;
import baritone.cache.CachedRegion;
import baritone.cache.WorldData;
import baritone.cache.WorldProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

/**
 * Wraps get for chuck caching capability
 *
 * @author leijurv
 */
public class BlockStateInterface implements Helper {

    private static Chunk prev = null;
    private static ICachedRegion prevCached = null;

    private static IBlockState AIR = Blocks.AIR.getDefaultState();

    public static IBlockState get(BlockPos pos) {
        return get(pos.getX(), pos.getY(), pos.getZ());
    }

    public static IBlockState get(int x, int y, int z) {

        // Invalid vertical position
        if (y < 0 || y >= 256) {
            return AIR;
        }

        if (!Baritone.settings().pathThroughCachedOnly.get()) {
            Chunk cached = prev;
            // there's great cache locality in block state lookups
            // generally it's within each movement
            // if it's the same chunk as last time
            // we can just skip the mc.world.getChunk lookup
            // which is a Long2ObjectOpenHashMap.get
            // see issue #113
            if (cached != null && cached.x == x >> 4 && cached.z == z >> 4) {
                return cached.getBlockState(x, y, z);
            }
            Chunk chunk = mc.world.getChunk(x >> 4, z >> 4);
            if (chunk.isLoaded()) {
                prev = chunk;
                return chunk.getBlockState(x, y, z);
            }
        }
        // same idea here, skip the Long2ObjectOpenHashMap.get if at all possible
        // except here, it's 512x512 tiles instead of 16x16, so even better repetition
        ICachedRegion cached = prevCached;
        if (cached == null || cached.getX() != x >> 9 || cached.getZ() != z >> 9) {
            WorldData world = WorldProvider.INSTANCE.getCurrentWorld();
            if (world == null) {
                return AIR;
            }
            ICachedRegion region = world.getCachedWorld().getRegion(x >> 9, z >> 9);
            if (region == null) {
                return AIR;
            }
            prevCached = region;
            cached = region;
        }
        IBlockState type = cached.getBlock(x & 511, y, z & 511);
        if (type == null) {
            return AIR;
        }
        return type;
    }

    public static boolean isLoaded(int x, int z) {
        Chunk prevChunk = prev;
        if (prevChunk != null && prevChunk.x == x >> 4 && prevChunk.z == z >> 4) {
            return true;
        }
        prevChunk = mc.world.getChunk(x >> 4, z >> 4);
        if (prevChunk.isLoaded()) {
            prev = prevChunk;
            return true;
        }
        ICachedRegion prevRegion = prevCached;
        if (prevRegion != null && prevRegion.getX() == x >> 9 && prevRegion.getZ() == z >> 9) {
            return prevRegion.isCached(x & 511, z & 511);
        }
        WorldData world = WorldProvider.INSTANCE.getCurrentWorld();
        if (world == null) {
            return false;
        }
        prevRegion = world.getCachedWorld().getRegion(x >> 9, z >> 9);
        if (prevRegion == null) {
            return false;
        }
        prevCached = prevRegion;
        return prevRegion.isCached(x & 511, z & 511);
    }

    public static void clearCachedChunk() {
        prev = null;
        prevCached = null;
    }

    public static Block getBlock(BlockPos pos) {
        return get(pos).getBlock();
    }

    public static Block getBlock(int x, int y, int z) {
        return get(x, y, z).getBlock();
    }

    /**
     * Returns whether or not the specified block is
     * water, regardless of whether or not it is flowing.
     *
     * @param b The block
     * @return Whether or not the block is water
     */
    public static boolean isWater(Block b) {
        return b == Blocks.FLOWING_WATER || b == Blocks.WATER;
    }

    /**
     * Returns whether or not the block at the specified pos is
     * water, regardless of whether or not it is flowing.
     *
     * @param bp The block pos
     * @return Whether or not the block is water
     */
    public static boolean isWater(BlockPos bp) {
        return isWater(BlockStateInterface.getBlock(bp));
    }

    public static boolean isLava(Block b) {
        return b == Blocks.FLOWING_LAVA || b == Blocks.LAVA;
    }

    /**
     * Returns whether or not the specified pos has a liquid
     *
     * @param p The pos
     * @return Whether or not the block is a liquid
     */
    public static boolean isLiquid(BlockPos p) {
        return BlockStateInterface.getBlock(p) instanceof BlockLiquid;
    }

    public static boolean isFlowing(IBlockState state) {
        // Will be IFluidState in 1.13
        return state.getBlock() instanceof BlockLiquid
                && state.getPropertyKeys().contains(BlockLiquid.LEVEL)
                && state.getValue(BlockLiquid.LEVEL) != 0;
    }
}
