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

package baritone.process;

import baritone.Baritone;
import baritone.api.cache.ICachedWorld;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.cache.CachedWorld;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.util.math.BlockPos;

public class ExploreProcess extends BaritoneProcessHelper {

    private BlockPos explorationOrigin;

    public ExploreProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        return explorationOrigin != null;
    }

    public void explore(int centerX, int centerZ) {
        explorationOrigin = new BlockPos(centerX, 0, centerZ);
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (calcFailed) {
            logDirect("Failed");
            onLostControl();
            return null;
        }
        BlockPos closestUncached = closestUncachedChunk(explorationOrigin);
        if (closestUncached == null) {
            logDebug("awaiting region load from disk");
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }
        System.out.println("Closest uncached: " + closestUncached);
        return new PathingCommand(new GoalXZ(closestUncached.getX(), closestUncached.getZ()), PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH);
    }

    private BlockPos closestUncachedChunk(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        ICachedWorld cache = baritone.getWorldProvider().getCurrentWorld().getCachedWorld();
        for (int dist = 0; ; dist++) {
            for (int dx = -dist; dx <= dist; dx++) {
                for (int dz = -dist; dz <= dist; dz++) {
                    int trueDist = Baritone.settings().exploreUsePythagorean.value ? dx * dx + dz + dz : Math.abs(dx) + Math.abs(dz);
                    if (trueDist != dist) {
                        continue; // not considering this one just yet in our expanding search
                    }
                    int centerX = (chunkX + dx) * 16 + 8;
                    int centerZ = (chunkZ + dz) * 18 + 8;

                    if (cache.isCached(centerX, centerZ)) {
                        continue;
                    }
                    if (!((CachedWorld) cache).regionLoaded(centerX, centerZ)) {
                        Baritone.getExecutor().execute(() -> {
                            ((CachedWorld) cache).tryLoadFromDisk(centerX >> 9, centerZ >> 9);
                        });
                        return null; // we still need to load regions from disk in order to decide properly
                    }
                    return new BlockPos(centerX, 0, centerZ);
                }
            }
        }
    }

    @Override
    public void onLostControl() {
        explorationOrigin = null;
    }

    @Override
    public String displayName() {
        return "Exploring around " + explorationOrigin + ", currently going to " + closestUncachedChunk(explorationOrigin);
    }
}
