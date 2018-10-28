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

package baritone.behavior;

import baritone.Baritone;
import baritone.api.behavior.IBehavior;

/**
 * A type of game event listener that can be toggled.
 *
 * @author Brady
 * @since 8/1/2018 6:29 PM
 */
public class Behavior implements IBehavior {

    public final Baritone baritone;

    protected Behavior(Baritone baritone) {
        this.baritone = baritone;
        baritone.registerBehavior(this);
    }

    /**
     * Whether or not this behavior is enabled
     */
    private boolean enabled = true;

    /**
     * Toggles the enabled state of this {@link Behavior}.
     *
     * @return The new state.
     */
    @Override
    public final boolean toggle() {
        return this.setEnabled(!this.isEnabled());
    }

    /**
     * Sets the enabled state of this {@link Behavior}.
     *
     * @return The new state.
     */
    @Override
    public final boolean setEnabled(boolean enabled) {
        if (enabled == this.enabled) {
            return this.enabled;
        }
        if (this.enabled = enabled) {
            this.onEnable();
        } else {
            this.onDisable();
        }
        return this.enabled;
    }

    /**
     * @return Whether or not this {@link Behavior} is active.
     */
    @Override
    public final boolean isEnabled() {
        return this.enabled;
    }
}
