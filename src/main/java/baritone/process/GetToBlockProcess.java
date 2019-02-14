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
import baritone.api.pathing.goals.*;
import baritone.api.process.IGetToBlockProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class GetToBlockProcess extends BaritoneProcessHelper implements IGetToBlockProcess {

    private Block gettingTo;
    private List<BlockPos> knownLocations;
    private List<BlockPos> blacklist; // locations we failed to calc to
    private BlockPos start;

    private int tickCount = 0;

    public GetToBlockProcess(Baritone baritone) {
        super(baritone, 2);
    }

    @Override
    public void getToBlock(Block block) {
        onLostControl();
        gettingTo = block;
        start = ctx.playerFeet();
        blacklist = new ArrayList<>();
        rescan(new ArrayList<>(), new CalculationContext(baritone));
    }

    @Override
    public boolean isActive() {
        return gettingTo != null;
    }

    @Override
    public synchronized PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (knownLocations == null) {
            rescan(new ArrayList<>(), new CalculationContext(baritone));
        }
        if (knownLocations.isEmpty()) {
            if (Baritone.settings().exploreForBlocks.get() && !calcFailed) {
                return new PathingCommand(new GoalRunAway(1, start) {
                    @Override
                    public boolean isInGoal(int x, int y, int z) {
                        return false;
                    }
                }, PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH);
            }
            logDirect("No known locations of " + gettingTo + ", canceling GetToBlock");
            if (isSafeToCancel) {
                onLostControl();
            }
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }
        Goal goal = new GoalComposite(knownLocations.stream().map(this::createGoal).toArray(Goal[]::new));
        if (calcFailed) {
            if (Baritone.settings().blacklistOnGetToBlockFailure.get()) {
                logDirect("Unable to find any path to " + gettingTo + ", blacklisting presumably unreachable closest instances");
                blacklistClosest();
                return onTick(false, isSafeToCancel); // gamer moment
            } else {
                logDirect("Unable to find any path to " + gettingTo + ", canceling GetToBlock");
                if (isSafeToCancel) {
                    onLostControl();
                }
                return new PathingCommand(goal, PathingCommandType.CANCEL_AND_SET_GOAL);
            }
        }
        int mineGoalUpdateInterval = Baritone.settings().mineGoalUpdateInterval.get();
        if (mineGoalUpdateInterval != 0 && tickCount++ % mineGoalUpdateInterval == 0) { // big brain
            List<BlockPos> current = new ArrayList<>(knownLocations);
            CalculationContext context = new CalculationContext(baritone, true);
            Baritone.getExecutor().execute(() -> rescan(current, context));
        }
        if (goal.isInGoal(ctx.playerFeet()) && isSafeToCancel) {
            // we're there
            if (rightClickOnArrival(gettingTo)) {
                if (rightClick()) {
                    onLostControl();
                    return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
                }
            } else {
                onLostControl();
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
            }
        }
        return new PathingCommand(goal, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
    }

    // blacklist the closest block and its adjacent blocks
    public synchronized void blacklistClosest() {
        List<BlockPos> newBlacklist = new ArrayList<>();
        knownLocations.stream().min(Comparator.comparingDouble(ctx.player()::getDistanceSq)).ifPresent(newBlacklist::add);
        outer:
        while (true) {
            for (BlockPos known : knownLocations) {
                for (BlockPos blacklist : newBlacklist) {
                    if (areAdjacent(known, blacklist)) { // directly adjacent
                        newBlacklist.add(known);
                        knownLocations.remove(known);
                        continue outer;
                    }
                }
            }
            if (true) {
                break; // codacy gets mad if i just end on a break LOL
            }
        }
        logDebug("Blacklisting unreachable locations " + newBlacklist);
        blacklist.addAll(newBlacklist);
    }

    // safer than direct double comparison from distanceSq
    private boolean areAdjacent(BlockPos posA, BlockPos posB) {
        int diffX = Math.abs(posA.getX() - posB.getX());
        int diffY = Math.abs(posA.getY() - posB.getY());
        int diffZ = Math.abs(posA.getZ() - posB.getZ());
        return (diffX + diffY + diffZ) == 1;
    }

    @Override
    public synchronized void onLostControl() {
        gettingTo = null;
        knownLocations = null;
        start = null;
        blacklist = null;
        baritone.getInputOverrideHandler().clearAllKeys();
    }

    @Override
    public String displayName() {
        return "Get To Block " + gettingTo;
    }

    private synchronized void rescan(List<BlockPos> known, CalculationContext context) {
        List<BlockPos> positions = MineProcess.searchWorld(context, Collections.singletonList(gettingTo), 64, known);
        positions.removeIf(blacklist::contains);
        knownLocations = positions;
    }

    private Goal createGoal(BlockPos pos) {
        return walkIntoInsteadOfAdjacent(gettingTo) ? new GoalTwoBlocks(pos) : new GoalGetToBlock(pos);
    }

    private boolean rightClick() {
        for (BlockPos pos : knownLocations) {
            Optional<Rotation> reachable = RotationUtils.reachable(ctx.player(), pos, ctx.playerController().getBlockReachDistance());
            if (reachable.isPresent()) {
                baritone.getLookBehavior().updateTarget(reachable.get(), true);
                if (knownLocations.contains(ctx.getSelectedBlock().orElse(null))) {
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true); // TODO find some way to right click even if we're in an ESC menu
                    System.out.println(ctx.player().openContainer);
                    if (!(ctx.player().openContainer instanceof ContainerPlayer)) {
                        return true;
                    }
                }
                return false; // trying to right click, will do it next tick or so
            }
        }
        logDirect("Arrived but failed to right click open");
        return true;
    }

    private boolean walkIntoInsteadOfAdjacent(Block block) {
        if (!Baritone.settings().enterPortal.get()) {
            return false;
        }
        return block == Blocks.PORTAL;
    }

    private boolean rightClickOnArrival(Block block) {
        if (!Baritone.settings().rightClickContainerOnArrival.get()) {
            return false;
        }
        return block == Blocks.CRAFTING_TABLE || block == Blocks.FURNACE || block == Blocks.ENDER_CHEST || block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST;
    }
}
