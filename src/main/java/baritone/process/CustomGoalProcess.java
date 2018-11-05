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
import baritone.api.pathing.goals.Goal;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.utils.BaritoneProcessHelper;

import java.util.Objects;

/**
 * As set by ExampleBaritoneControl or something idk
 *
 * @author leijurv
 */
public class CustomGoalProcess extends BaritoneProcessHelper implements ICustomGoalProcess {
    private Goal goal;
    private State state;

    public CustomGoalProcess(Baritone baritone) {
        super(baritone, 3);
    }

    @Override
    public void setGoal(Goal goal) {
        this.goal = goal;
        state = State.GOAL_SET;
    }

    @Override
    public void path() {
        if (goal == null) {
            goal = baritone.getPathingBehavior().getGoal();
        }
        state = State.PATH_REQUESTED;
    }

    private enum State {
        NONE,
        GOAL_SET,
        PATH_REQUESTED,
        EXECUTING,

    }

    @Override
    public boolean isActive() {
        return state != State.NONE;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        switch (state) {
            case GOAL_SET:
                if (!baritone.getPathingBehavior().isPathing() && Objects.equals(baritone.getPathingBehavior().getGoal(), goal)) {
                    state = State.NONE;
                }
                return new PathingCommand(goal, PathingCommandType.CANCEL_AND_SET_GOAL);
            case PATH_REQUESTED:
                PathingCommand ret = new PathingCommand(goal, PathingCommandType.SET_GOAL_AND_PATH);
                state = State.EXECUTING;
                return ret;
            case EXECUTING:
                if (calcFailed) {
                    onLostControl();
                }
                if (goal.isInGoal(playerFeet())) {
                    onLostControl(); // we're there xd
                }
                return new PathingCommand(goal, PathingCommandType.SET_GOAL_AND_PATH);
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void onLostControl() {
        state = State.NONE;
        goal = null;
    }

    @Override
    public String displayName() {
        return "Custom Goal " + goal;
    }
}
