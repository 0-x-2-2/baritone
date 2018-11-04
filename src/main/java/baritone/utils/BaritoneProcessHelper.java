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

package baritone.utils;

import baritone.Baritone;
import baritone.api.process.IBaritoneProcess;

public abstract class BaritoneProcessHelper implements IBaritoneProcess, Helper {
    public static final double DEFAULT_PRIORITY = 0;

    protected final Baritone baritone;
    private final double priority;

    public BaritoneProcessHelper(Baritone baritone) {
        this(baritone, DEFAULT_PRIORITY);
    }

    public BaritoneProcessHelper(Baritone baritone, double priority) {
        this.baritone = baritone;
        this.priority = priority;
        baritone.getPathingControlManager().registerProcess(this);
    }

    @Override
    public Baritone associatedWith() {
        return baritone;
    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public double priority() {
        return priority;
    }
}
