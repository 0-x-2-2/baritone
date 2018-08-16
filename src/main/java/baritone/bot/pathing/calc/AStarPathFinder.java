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

package baritone.bot.pathing.calc;

import baritone.bot.Baritone;
import baritone.bot.chunk.CachedWorldProvider;
import baritone.bot.pathing.calc.openset.BinaryHeapOpenSet;
import baritone.bot.pathing.calc.openset.IOpenSet;
import baritone.bot.pathing.goals.Goal;
import baritone.bot.pathing.movement.ActionCosts;
import baritone.bot.pathing.movement.CalculationContext;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.pathing.movement.MovementHelper;
import baritone.bot.pathing.movement.movements.*;
import baritone.bot.pathing.path.IPath;
import baritone.bot.utils.Helper;
import baritone.bot.utils.pathing.BetterBlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.EmptyChunk;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;

/**
 * The actual A* pathfinding
 *
 * @author leijurv
 */
public class AStarPathFinder extends AbstractNodeCostSearch implements Helper {

    private final Optional<HashSet<BetterBlockPos>> favoredPositions;

    public AStarPathFinder(BlockPos start, Goal goal, Optional<Collection<BetterBlockPos>> favoredPositions) {
        super(start, goal);
        this.favoredPositions = favoredPositions.map(HashSet::new); // <-- okay this is epic
    }

    @Override
    protected Optional<IPath> calculate0() {
        startNode = getNodeAtPosition(start);
        startNode.cost = 0;
        startNode.combinedCost = startNode.estimatedCostToGoal;
        IOpenSet openSet = new BinaryHeapOpenSet();
        openSet.insert(startNode);
        startNode.isOpen = true;
        bestSoFar = new PathNode[COEFFICIENTS.length];//keep track of the best node by the metric of (estimatedCostToGoal + cost / COEFFICIENTS[i])
        double[] bestHeuristicSoFar = new double[COEFFICIENTS.length];
        for (int i = 0; i < bestHeuristicSoFar.length; i++) {
            bestHeuristicSoFar[i] = Double.MAX_VALUE;
        }
        CalculationContext calcContext = new CalculationContext();
        HashSet<BetterBlockPos> favored = favoredPositions.orElse(null);
        currentlyRunning = this;
        long startTime = System.currentTimeMillis();
        boolean slowPath = Baritone.settings().slowPath.get();
        long timeoutTime = startTime + (slowPath ? Baritone.settings().slowPathTimeoutMS : Baritone.settings().pathTimeoutMS).<Long>get();
        long lastPrintout = 0;
        int numNodes = 0;
        int numEmptyChunk = 0;
        boolean favoring = favoredPositions.isPresent(); // grab all settings beforehand so that changing settings during pathing doesn't cause a crash or unpredictable behavior
        boolean cache = Baritone.settings().chuckCaching.get();
        int pathingMaxChunkBorderFetch = Baritone.settings().pathingMaxChunkBorderFetch.get();
        double favorCoeff = Baritone.settings().backtrackCostFavoringCoefficient.get();
        while (!openSet.isEmpty() && numEmptyChunk < pathingMaxChunkBorderFetch && System.currentTimeMillis() < timeoutTime) {
            if (slowPath) {
                try {
                    Thread.sleep(Baritone.settings().slowPathTimeDelayMS.<Long>get());
                } catch (InterruptedException ex) {
                }
            }
            PathNode currentNode = openSet.removeLowest();
            currentNode.isOpen = false;
            mostRecentConsidered = currentNode;
            BetterBlockPos currentNodePos = currentNode.pos;
            numNodes++;
            if (System.currentTimeMillis() > lastPrintout + 1000) {//print once a second
                System.out.println("searching... at " + currentNodePos + ", considered " + numNodes + " nodes so far");
                lastPrintout = System.currentTimeMillis();
            }
            if (goal.isInGoal(currentNodePos)) {
                currentlyRunning = null;
                return Optional.of(new Path(startNode, currentNode, numNodes));
            }
            //long constructStart = System.nanoTime();
            Movement[] possibleMovements = getConnectedPositions(currentNodePos, calcContext);//movement that we could take that start at currentNodePos, in random order
            shuffle(possibleMovements);
            //long constructEnd = System.nanoTime();
            //System.out.println(constructEnd - constructStart);
            for (Movement movementToGetToNeighbor : possibleMovements) {
                if (movementToGetToNeighbor == null) {
                    continue;
                }
                BetterBlockPos dest = (BetterBlockPos) movementToGetToNeighbor.getDest();
                boolean isPositionCached = false;
                if (cache) {
                    if (CachedWorldProvider.INSTANCE.getCurrentWorld() != null) {
                        if (CachedWorldProvider.INSTANCE.getCurrentWorld().getBlockType(dest) != null) {
                            isPositionCached = true;
                        }
                    }
                }
                if (!isPositionCached && Minecraft.getMinecraft().world.getChunk(dest) instanceof EmptyChunk) {
                    numEmptyChunk++;
                    continue;
                }
                //long costStart = System.nanoTime();
                // TODO cache cost
                double actionCost = movementToGetToNeighbor.getCost(calcContext);
                //long costEnd = System.nanoTime();
                //System.out.println(movementToGetToNeighbor.getClass() + "" + (costEnd - costStart));
                if (actionCost >= ActionCosts.COST_INF) {
                    continue;
                }
                if (actionCost <= 0) {
                    throw new IllegalStateException(movementToGetToNeighbor.getClass() + " " + movementToGetToNeighbor + " calculated implausible cost " + actionCost);
                }
                if (favoring && favored.contains(dest)) {
                    actionCost *= favorCoeff;
                }
                PathNode neighbor = getNodeAtPosition(dest);
                double tentativeCost = currentNode.cost + actionCost;
                if (tentativeCost < neighbor.cost) {
                    if (tentativeCost < 0) {
                        throw new IllegalStateException(movementToGetToNeighbor.getClass() + " " + movementToGetToNeighbor + " overflowed into negative " + actionCost + " " + neighbor.cost + " " + tentativeCost);
                    }
                    neighbor.previous = currentNode;
                    neighbor.previousMovement = movementToGetToNeighbor;
                    neighbor.cost = tentativeCost;
                    neighbor.combinedCost = tentativeCost + neighbor.estimatedCostToGoal;
                    if (neighbor.isOpen) {
                        openSet.update(neighbor);
                    } else {
                        openSet.insert(neighbor);//dont double count, dont insert into open set if it's already there
                        neighbor.isOpen = true;
                    }
                    for (int i = 0; i < bestSoFar.length; i++) {
                        double heuristic = neighbor.estimatedCostToGoal + neighbor.cost / COEFFICIENTS[i];
                        if (heuristic < bestHeuristicSoFar[i]) {
                            bestHeuristicSoFar[i] = heuristic;
                            bestSoFar[i] = neighbor;
                        }
                    }
                }
            }
        }
        double bestDist = 0;
        for (int i = 0; i < bestSoFar.length; i++) {
            if (bestSoFar[i] == null) {
                continue;
            }
            double dist = getDistFromStartSq(bestSoFar[i]);
            if (dist > bestDist) {
                bestDist = dist;
            }
            if (dist > MIN_DIST_PATH * MIN_DIST_PATH) { // square the comparison since distFromStartSq is squared
                displayChatMessageRaw("A* cost coefficient " + COEFFICIENTS[i]);
                if (COEFFICIENTS[i] >= 3) {
                    System.out.println("Warning: cost coefficient is greater than three! Probably means that");
                    System.out.println("the path I found is pretty terrible (like sneak-bridging for dozens of blocks)");
                    System.out.println("But I'm going to do it anyway, because yolo");
                }
                System.out.println("Path goes for " + dist + " blocks");
                currentlyRunning = null;
                return Optional.of(new Path(startNode, bestSoFar[i], numNodes));
            }
        }
        displayChatMessageRaw("Even with a cost coefficient of " + COEFFICIENTS[COEFFICIENTS.length - 1] + ", I couldn't get more than " + bestDist + " blocks =(");
        displayChatMessageRaw("No path found =(");
        currentlyRunning = null;
        return Optional.empty();
    }


    private static Movement[] getConnectedPositions(BetterBlockPos pos, CalculationContext calcContext) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        BetterBlockPos east = new BetterBlockPos(x + 1, y, z);
        BetterBlockPos west = new BetterBlockPos(x - 1, y, z);
        BetterBlockPos south = new BetterBlockPos(x, y, z + 1);
        BetterBlockPos north = new BetterBlockPos(x, y, z - 1);
        return new Movement[]{
                new MovementTraverse(pos, east),
                new MovementTraverse(pos, west),
                new MovementTraverse(pos, north),
                new MovementTraverse(pos, south),
                new MovementAscend(pos, new BetterBlockPos(x + 1, y + 1, z)),
                new MovementAscend(pos, new BetterBlockPos(x - 1, y + 1, z)),
                new MovementAscend(pos, new BetterBlockPos(x, y + 1, z + 1)),
                new MovementAscend(pos, new BetterBlockPos(x, y + 1, z - 1)),
                MovementHelper.generateMovementFallOrDescend(pos, east, calcContext),
                MovementHelper.generateMovementFallOrDescend(pos, west, calcContext),
                MovementHelper.generateMovementFallOrDescend(pos, north, calcContext),
                MovementHelper.generateMovementFallOrDescend(pos, south, calcContext),
                new MovementDownward(pos, new BetterBlockPos(x, y - 1, z)),
                new MovementDiagonal(pos, EnumFacing.NORTH, EnumFacing.WEST),
                new MovementDiagonal(pos, EnumFacing.NORTH, EnumFacing.EAST),
                new MovementDiagonal(pos, EnumFacing.SOUTH, EnumFacing.WEST),
                new MovementDiagonal(pos, EnumFacing.SOUTH, EnumFacing.EAST),
                new MovementPillar(pos, new BetterBlockPos(x, y + 1, z))
        };
    }

    private final Random random = new Random();

    private <T> void shuffle(T[] list) {
        int len = list.length;
        for (int i = 0; i < len; i++) {
            int j = random.nextInt(len);
            T t = list[j];
            list[j] = list[i];
            list[i] = t;
        }
    }
}
