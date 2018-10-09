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

package baritone.api.pathing.calc;

import baritone.api.Settings;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.movement.IMovement;
import baritone.api.utils.BetterBlockPos;

import java.util.List;

/**
 * @author leijurv, Brady
 */
public interface IPath {

    /**
     * Ordered list of movements to carry out.
     * movements.get(i).getSrc() should equal positions.get(i)
     * movements.get(i).getDest() should equal positions.get(i+1)
     * movements.size() should equal positions.size()-1
     *
     * @return All of the movements to carry out
     */
    List<IMovement> movements();

    /**
     * All positions along the way.
     * Should begin with the same as getSrc and end with the same as getDest
     *
     * @return All of the positions along this path
     */
    List<BetterBlockPos> positions();

    /**
     * This path is actually going to be executed in the world. Do whatever additional processing is required.
     * (as opposed to Path objects that are just constructed every frame for rendering)
     */
    default void postProcess() {}

    /**
     * Returns the number of positions in this path. Equivalent to {@code positions().size()}.
     *
     * @return Number of positions in this path
     */
    default int length() {
        return positions().size();
    }

    /**
     * @return The goal that this path was calculated towards
     */
    Goal getGoal();

    /**
     * Returns the number of nodes that were considered during calculation before
     * this path was found.
     *
     * @return The number of nodes that were considered before finding this path
     */
    int getNumNodesConsidered();

    /**
     * Returns the start position of this path. This is the first element in the
     * {@link List} that is returned by {@link IPath#positions()}.
     *
     * @return The start position of this path
     */
    default BetterBlockPos getSrc() {
        return positions().get(0);
    }

    /**
     * Returns the end position of this path. This is the last element in the
     * {@link List} that is returned by {@link IPath#positions()}.
     *
     * @return The end position of this path.
     */
    default BetterBlockPos getDest() {
        List<BetterBlockPos> pos = positions();
        return pos.get(pos.size() - 1);
    }

    /**
     * Returns the estimated number of ticks to complete the path from the given node index.
     *
     * @param pathPosition The index of the node we're calculating from
     * @return The estimated number of ticks remaining frm the given position
     */
    default double ticksRemainingFrom(int pathPosition) {
        double sum = 0;
        //this is fast because we aren't requesting recalculation, it's just cached
        for (int i = pathPosition; i < movements().size(); i++) {
            sum += movements().get(i).getCost();
        }
        return sum;
    }

    /**
     * Cuts off this path at the loaded chunk border, and returns the resulting path. Default
     * implementation just returns this path, without the intended functionality.
     *
     * @return The result of this cut-off operation
     */
    default IPath cutoffAtLoadedChunks() {
        return this;
    }

    /**
     * Cuts off this path using the min length and cutoff factor settings, and returns the resulting path.
     * Default implementation just returns this path, without the intended functionality.
     *
     * @see Settings#pathCutoffMinimumLength
     * @see Settings#pathCutoffFactor
     *
     * @return The result of this cut-off operation
     */
    default IPath staticCutoff(Goal destination) {
        return this;
    }
}
