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

package baritone.behavior.impl;

import baritone.Baritone;
import baritone.Settings;
import baritone.behavior.Behavior;
import baritone.event.events.PlayerUpdateEvent;
import baritone.event.events.RelativeMoveEvent;
import baritone.event.events.type.EventState;
import baritone.utils.Rotation;

public class LookBehavior extends Behavior {

    public static final LookBehavior INSTANCE = new LookBehavior();

    private LookBehavior() {}

    /**
     * Target's values are as follows:
     * <p>
     * getFirst() -> yaw
     * getSecond() -> pitch
     */
    private Rotation target;

    /**
     * Whether or not rotations are currently being forced
     */
    private boolean force;

    /**
     * The last player yaw angle. Used when free looking
     *
     * @see Settings#freeLook
     */
    private float lastYaw;

    public void updateTarget(Rotation target, boolean force) {
        this.target = target;
        this.force = force || !Baritone.settings().freeLook.get();
    }

    @Override
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (event.getState() == EventState.PRE && this.target != null && this.force) {
            player().rotationYaw = this.target.getFirst();
            float oldPitch = player().rotationPitch;
            float desiredPitch = this.target.getSecond();
            player().rotationPitch = desiredPitch;
            if (desiredPitch == oldPitch) {
                nudgeToLevel();
            }
            this.target = null;
        }
    }

    @Override
    public void onPlayerRelativeMove(RelativeMoveEvent event) {
        if (this.target != null && !force) {
            switch (event.getState()) {
                case PRE:
                    this.lastYaw = player().rotationYaw;
                    player().rotationYaw = this.target.getFirst();
                    break;
                case POST:
                    player().rotationYaw = this.lastYaw;
                    this.target = null;
                    break;
            }
        }
    }

    private void nudgeToLevel() {
        if (player().rotationPitch < -20) {
            player().rotationPitch++;
        } else if (player().rotationPitch > 10) {
            player().rotationPitch--;
        }
    }
}
