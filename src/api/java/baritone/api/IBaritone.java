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

package baritone.api;

import baritone.api.behavior.ILookBehavior;
import baritone.api.behavior.IMemoryBehavior;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.cache.IWorldProvider;
import baritone.api.event.listener.IGameEventListener;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.IFollowProcess;
import baritone.api.process.IGetToBlockProcess;
import baritone.api.process.IMineProcess;
import baritone.api.utils.IInputOverrideHandler;
import baritone.api.utils.IPlayerContext;

/**
 * @author Brady
 * @since 9/29/2018
 */
public interface IBaritone {

    /**
     * @return The {@link IFollowProcess} instance
     * @see IFollowProcess
     */
    IFollowProcess getFollowProcess();

    /**
     * @return The {@link ILookBehavior} instance
     * @see ILookBehavior
     */
    ILookBehavior getLookBehavior();

    /**
     * @return The {@link IMemoryBehavior} instance
     * @see IMemoryBehavior
     */
    IMemoryBehavior getMemoryBehavior();

    /**
     * @return The {@link IMineProcess} instance
     * @see IMineProcess
     */
    IMineProcess getMineProcess();

    /**
     * @return The {@link IPathingBehavior} instance
     * @see IPathingBehavior
     */
    IPathingBehavior getPathingBehavior();

    /**
     * @return The {@link IWorldProvider} instance
     * @see IWorldProvider
     */
    IWorldProvider getWorldProvider();

    IInputOverrideHandler getInputOverrideHandler();

    ICustomGoalProcess getCustomGoalProcess();

    IGetToBlockProcess getGetToBlockProcess();

    IPlayerContext getPlayerContext();

    /**
     * Registers a {@link IGameEventListener} with Baritone's "event bus".
     *
     * @param listener The listener
     */
    void registerEventListener(IGameEventListener listener);
}
