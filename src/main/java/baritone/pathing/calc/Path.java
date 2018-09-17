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

package baritone.pathing.calc;

import baritone.pathing.goals.Goal;
import baritone.pathing.movement.Movement;
import baritone.pathing.path.IPath;
import baritone.utils.pathing.BetterBlockPos;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A node based implementation of IPath
 *
 * @author leijurv
 */
class Path implements IPath {

    /**
     * The start position of this path
     */
    private final BetterBlockPos start;

    /**
     * The end position of this path
     */
    private final BetterBlockPos end;

    /**
     * The blocks on the path. Guaranteed that path.get(0) equals start and
     * path.get(path.size()-1) equals end
     */
    private final List<BetterBlockPos> path;

    private final List<Movement> movements;

    private final Goal goal;

    private final int numNodes;

    private volatile boolean verified;

    Path(PathNode start, PathNode end, int numNodes, Goal goal) {
        this.start = start.pos;
        this.end = end.pos;
        this.numNodes = numNodes;
        this.path = new ArrayList<>();
        this.movements = new ArrayList<>();
        this.goal = goal;
        assemblePath(start, end);
    }

    @Override
    public Goal getGoal() {
        return goal;
    }

    /**
     * Assembles this path given the start and end nodes.
     *
     * @param start The start node
     * @param end   The end node
     */
    private void assemblePath(PathNode start, PathNode end) {
        if (!path.isEmpty() || !movements.isEmpty()) {
            throw new IllegalStateException();
        }
        PathNode current = end;
        LinkedList<BetterBlockPos> tempPath = new LinkedList<>(); // Repeatedly inserting to the beginning of an arraylist is O(n^2)
        LinkedList<Movement> tempMovements = new LinkedList<>(); // Instead, do it into a linked list, then convert at the end
        while (!current.equals(start)) {
            tempPath.addFirst(current.pos);
            tempMovements.addFirst(current.previousMovement);
            current = current.previous;
        }
        tempPath.addFirst(start.pos);
        // Can't directly convert from the PathNode pseudo linked list to an array because we don't know how long it is
        // inserting into a LinkedList<E> keeps track of length, then when we addall (which calls .toArray) it's able
        // to performantly do that conversion since it knows the length.
        path.addAll(tempPath);
        movements.addAll(tempMovements);
    }

    /**
     * Performs a series of checks to ensure that the assembly of the path went as expected.
     */
    private void sanityCheck() {
        if (!start.equals(path.get(0))) {
            throw new IllegalStateException("Start node does not equal first path element");
        }
        if (!end.equals(path.get(path.size() - 1))) {
            throw new IllegalStateException("End node does not equal last path element");
        }
        if (path.size() != movements.size() + 1) {
            throw new IllegalStateException("Size of path array is unexpected");
        }
        for (int i = 0; i < path.size() - 1; i++) {
            BlockPos src = path.get(i);
            BlockPos dest = path.get(i + 1);
            Movement movement = movements.get(i);
            if (!src.equals(movement.getSrc())) {
                throw new IllegalStateException("Path source is not equal to the movement source");
            }
            if (!dest.equals(movement.getDest())) {
                throw new IllegalStateException("Path destination is not equal to the movement destination");
            }
        }
    }

    @Override
    public void postprocess() {
        if (verified) {
            throw new IllegalStateException();
        }
        verified = true;
        // more post processing here
        movements.forEach(Movement::checkLoadedChunk);
        sanityCheck();
    }

    @Override
    public List<Movement> movements() {
        if (!verified) {
            throw new IllegalStateException();
        }
        return Collections.unmodifiableList(movements);
    }

    @Override
    public List<BetterBlockPos> positions() {
        return Collections.unmodifiableList(path);
    }

    @Override
    public int getNumNodesConsidered() {
        return numNodes;
    }

    @Override
    public BetterBlockPos getSrc() {
        return start;
    }

    @Override
    public BetterBlockPos getDest() {
        return end;
    }
}
