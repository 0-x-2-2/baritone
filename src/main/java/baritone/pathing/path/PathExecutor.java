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

package baritone.pathing.path;

import baritone.Baritone;
import baritone.api.event.events.TickEvent;
import baritone.pathing.movement.*;
import baritone.pathing.movement.movements.MovementDescend;
import baritone.pathing.movement.movements.MovementDiagonal;
import baritone.pathing.movement.movements.MovementFall;
import baritone.pathing.movement.movements.MovementTraverse;
import baritone.utils.BlockStateInterface;
import baritone.utils.Helper;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static baritone.pathing.movement.MovementState.MovementStatus.*;

/**
 * Behavior to execute a precomputed path. Does not (yet) deal with path segmentation or stitching
 * or cutting (jumping onto the next path if it starts with a backtrack of this path's ending)
 *
 * @author leijurv
 */
public class PathExecutor implements Helper {
    private static final double MAX_MAX_DIST_FROM_PATH = 3;
    private static final double MAX_DIST_FROM_PATH = 2;
    private static final double MAX_TICKS_AWAY = 200; // ten seconds. ok to decrease this, but it must be at least 110, see issue #102
    private final IPath path;
    private int pathPosition;
    private int ticksAway;
    private int ticksOnCurrent;
    private Double currentMovementInitialCostEstimate;
    private Integer costEstimateIndex;
    private boolean failed;
    private boolean recalcBP = true;
    private HashSet<BlockPos> toBreak = new HashSet<>();
    private HashSet<BlockPos> toPlace = new HashSet<>();
    private HashSet<BlockPos> toWalkInto = new HashSet<>();

    public PathExecutor(IPath path) {
        this.path = path;
        this.pathPosition = 0;
    }

    /**
     * Tick this executor
     *
     * @param event
     * @return True if a movement just finished (and the player is therefore in a "stable" state, like,
     * not sneaking out over lava), false otherwise
     */
    public boolean onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            throw new IllegalStateException();
        }
        if (pathPosition >= path.length()) {
            //stop bugging me, I'm done
            //TODO Baritone.INSTANCE.behaviors.remove(this)
            return true;
        }
        BlockPos whereShouldIBe = path.positions().get(pathPosition);
        EntityPlayerSP thePlayer = mc.player;
        BlockPos whereAmI = playerFeet();
        if (pathPosition == path.length() - 1) {
            pathPosition++;
            return true;
        }
        if (!whereShouldIBe.equals(whereAmI)) {
            //System.out.println("Should be at " + whereShouldIBe + " actually am at " + whereAmI);
            if (!Blocks.AIR.equals(BlockStateInterface.getBlock(whereAmI.down()))) {//do not skip if standing on air, because our position isn't stable to skip
                for (int i = 0; i < pathPosition - 1 && i < path.length(); i++) {//this happens for example when you lag out and get teleported back a couple blocks
                    if (whereAmI.equals(path.positions().get(i))) {
                        logDebug("Skipping back " + (pathPosition - i) + " steps, to " + i);
                        int previousPos = pathPosition;
                        pathPosition = Math.max(i - 1, 0); // previous step might not actually be done
                        for (int j = pathPosition; j <= previousPos; j++) {
                            path.movements().get(j).reset();
                        }
                        Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
                        return false;
                    }
                }
                for (int i = pathPosition + 2; i < path.length(); i++) { //dont check pathPosition+1. the movement tells us when it's done (e.g. sneak placing)
                    if (whereAmI.equals(path.positions().get(i))) {
                        if (i - pathPosition > 2) {
                            logDebug("Skipping forward " + (i - pathPosition) + " steps, to " + i);
                        }
                        System.out.println("Double skip sundae");
                        pathPosition = i - 1;
                        Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
                        return false;
                    }
                }
            }
        }
        Tuple<Double, BlockPos> status = path.closestPathPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
        double distanceFromPath = status.getFirst();
        if (distanceFromPath > MAX_DIST_FROM_PATH) {
            ticksAway++;
            System.out.println("FAR AWAY FROM PATH FOR " + ticksAway + " TICKS. Current distance: " + distanceFromPath + ". Threshold: " + MAX_DIST_FROM_PATH);
            if (ticksAway > MAX_TICKS_AWAY) {
                logDebug("Too far away from path for too long, cancelling path");
                System.out.println("Too many ticks");
                pathPosition = path.length() + 3;
                Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
                failed = true;
                return false;
            }
        } else {
            ticksAway = 0;
        }
        if (distanceFromPath > MAX_MAX_DIST_FROM_PATH) {
            if (!(path.movements().get(pathPosition) instanceof MovementFall)) { // might be midair
                if (pathPosition == 0 || !(path.movements().get(pathPosition - 1) instanceof MovementFall)) { // might have overshot the landing
                    logDebug("too far from path");
                    pathPosition = path.length() + 3;
                    Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
                    failed = true;
                    return false;
                }
            }
        }
        //this commented block is literally cursed.
        /*Out.log(actions.get(pathPosition));
        if (pathPosition < actions.size() - 1) {//if there are two ActionBridges in a row and they are at right angles, walk diagonally. This makes it so you walk at 45 degrees along a zigzag path instead of doing inefficient zigging and zagging
            if ((actions.get(pathPosition) instanceof ActionBridge) && (actions.get(pathPosition + 1) instanceof ActionBridge)) {
                ActionBridge curr = (ActionBridge) actions.get(pathPosition);
                ActionBridge next = (ActionBridge) actions.get(pathPosition + 1);
                if (curr.dx() != next.dx() || curr.dz() != next.dz()) {//two movement are not parallel, so this is a right angle
                    if (curr.amIGood() && next.amIGood()) {//nothing in the way
                        BlockPos cornerToCut1 = new BlockPos(next.to.getX() - next.from.getX() + curr.from.getX(), next.to.getY(), next.to.getZ() - next.from.getZ() + curr.from.getZ());
                        BlockPos cornerToCut2 = cornerToCut1.up();
                        //Block corner1 = Baritone.get(cornerToCut1).getBlock();
                        //Block corner2 = Baritone.get(cornerToCut2).getBlock();
                        //Out.gui("Cutting conner " + cornerToCut1 + " " + corner1, Out.Mode.Debug);
                        if (!Action.avoidWalkingInto(cornerToCut1) && !Action.avoidWalkingInto(cornerToCut2)) {
                            double x = (next.from.getX() + next.to.getX() + 1.0D) * 0.5D;
                            double z = (next.from.getZ() + next.to.getZ() + 1.0D) * 0.5D;
                            MovementManager.clearMovement();
                            if (!MovementManager.forward && curr.oneInTen != null && curr.oneInTen) {
                                MovementManager.clearMovement();
                                MovementManager.forward = LookManager.lookAtCoords(x, 0, z, false);
                            } else {
                                MovementManager.moveTowardsCoords(x, 0, z);
                            }
                            if (MovementManager.forward && !MovementManager.backward) {
                                thePlayer.setSprinting(true);
                            }
                            return false;
                        }
                    }
                }
            }
        }*/
        long start = System.nanoTime() / 1000000L;
        for (int i = pathPosition - 10; i < pathPosition + 10; i++) {
            if (i >= 0 && i < path.movements().size()) {
                Movement m = path.movements().get(i);
                HashSet<BlockPos> prevBreak = new HashSet<>(m.toBreak());
                HashSet<BlockPos> prevPlace = new HashSet<>(m.toPlace());
                HashSet<BlockPos> prevWalkInto = new HashSet<>(m.toWalkInto());
                m.toBreakCached = null;
                m.toPlaceCached = null;
                m.toWalkIntoCached = null;
                if (!prevBreak.equals(new HashSet<>(m.toBreak()))) {
                    recalcBP = true;
                }
                if (!prevPlace.equals(new HashSet<>(m.toPlace()))) {
                    recalcBP = true;
                }
                if (!prevWalkInto.equals(new HashSet<>(m.toWalkInto()))) {
                    recalcBP = true;
                }
            }
        }
        if (recalcBP) {
            HashSet<BlockPos> newBreak = new HashSet<>();
            HashSet<BlockPos> newPlace = new HashSet<>();
            HashSet<BlockPos> newWalkInto = new HashSet<>();
            for (int i = pathPosition; i < path.movements().size(); i++) {
                newBreak.addAll(path.movements().get(i).toBreak());
                newPlace.addAll(path.movements().get(i).toPlace());
                newWalkInto.addAll(path.movements().get(i).toWalkInto());
            }
            toBreak = newBreak;
            toPlace = newPlace;
            toWalkInto = newWalkInto;
            recalcBP = false;
        }
        long end = System.nanoTime() / 1000000L;
        if (end - start > 0) {
            //logDebug("Recalculating break and place took " + (end - start) + "ms");
        }
        Movement movement = path.movements().get(pathPosition);
        if (costEstimateIndex == null || costEstimateIndex != pathPosition) {
            costEstimateIndex = pathPosition;
            // do this only once, when the movement starts, and deliberately get the cost as cached when this path was calculated, not the cost as it is right now
            currentMovementInitialCostEstimate = movement.getCost(null);
            for (int i = 1; i < Baritone.settings().costVerificationLookahead.get() && pathPosition + i < path.length() - 1; i++) {
                if (path.movements().get(pathPosition + i).calculateCostWithoutCaching() >= ActionCosts.COST_INF) {
                    logDebug("Something has changed in the world and a future movement has become impossible. Cancelling.");
                    pathPosition = path.length() + 3;
                    failed = true;
                    Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
                    return true;
                }
            }
        }
        double currentCost = movement.recalculateCost();
        if (currentCost >= ActionCosts.COST_INF) {
            logDebug("Something has changed in the world and this movement has become impossible. Cancelling.");
            pathPosition = path.length() + 3;
            failed = true;
            Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
            return true;
        }
        MovementState.MovementStatus movementStatus = movement.update();
        if (movementStatus == UNREACHABLE || movementStatus == FAILED) {
            logDebug("Movement returns status " + movementStatus);
            pathPosition = path.length() + 3;
            failed = true;
            Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
            return true;
        }
        if (movementStatus == SUCCESS) {
            //System.out.println("Movement done, next path");
            pathPosition++;
            ticksOnCurrent = 0;
            Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
            onTick(event);
            return true;
        } else {
            sprintIfRequested();
            ticksOnCurrent++;
            if (ticksOnCurrent > currentMovementInitialCostEstimate + Baritone.settings().movementTimeoutTicks.get()) {
                // only fail if the total time has exceeded the initial estimate
                // as you break the blocks required, the remaining cost goes down, to the point where
                // ticksOnCurrent is greater than recalculateCost + 100
                // this is why we cache cost at the beginning, and don't recalculate for this comparison every tick
                logDebug("This movement has taken too long (" + ticksOnCurrent + " ticks, expected " + currentMovementInitialCostEstimate + "). Cancelling.");
                movement.cancel();
                Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
                pathPosition = path.length() + 3;
                failed = true;
                return true;
            }
        }
        return false; // movement is in progress
    }

    private void sprintIfRequested() {
        if (!new CalculationContext().canSprint()) {
            player().setSprinting(false);
            return;
        }
        if (Baritone.INSTANCE.getInputOverrideHandler().isInputForcedDown(mc.gameSettings.keyBindSprint)) {
            if (!player().isSprinting()) {
                player().setSprinting(true);
            }
            return;
        }
        Movement movement = path.movements().get(pathPosition);
        if (movement instanceof MovementDescend && pathPosition < path.length() - 2) {
            Movement next = path.movements().get(pathPosition + 1);
            if (next instanceof MovementDescend) {
                if (next.getDirection().equals(movement.getDirection())) {
                    if (playerFeet().equals(movement.getDest())) {
                        pathPosition++;
                        Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
                    }
                    if (!player().isSprinting()) {
                        player().setSprinting(true);
                    }
                    return;
                }
            }
            if (next instanceof MovementTraverse) {
                if (next.getDirection().down().equals(movement.getDirection()) && MovementHelper.canWalkOn(next.getDest().down())) {
                    if (playerFeet().equals(movement.getDest())) {
                        pathPosition++;
                        Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
                    }
                    if (!player().isSprinting()) {
                        player().setSprinting(true);
                    }
                    return;
                }
            }
            if (next instanceof MovementDiagonal && Baritone.settings().allowOvershootDiagonalDescend.get()) {
                if (playerFeet().equals(movement.getDest())) {
                    pathPosition++;
                    Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
                }
                if (!player().isSprinting()) {
                    player().setSprinting(true);
                }
                return;
            }
            //logDebug("Turning off sprinting " + movement + " " + next + " " + movement.getDirection() + " " + next.getDirection().down() + " " + next.getDirection().down().equals(movement.getDirection()));
        }
        player().setSprinting(false);
    }

    public int getPosition() {
        return pathPosition;
    }

    public IPath getPath() {
        return path;
    }

    public boolean failed() {
        return failed;
    }

    public boolean finished() {
        return pathPosition >= path.length();
    }

    public Set<BlockPos> toBreak() {
        return Collections.unmodifiableSet(toBreak);
    }

    public Set<BlockPos> toPlace() {
        return Collections.unmodifiableSet(toPlace);
    }

    public Set<BlockPos> toWalkInto() {
        return Collections.unmodifiableSet(toWalkInto);
    }
}
