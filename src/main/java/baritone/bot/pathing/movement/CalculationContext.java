/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.pathing.movement;

import baritone.bot.utils.ToolSet;

/**
 * @author Brady
 * @since 8/7/2018 4:30 PM
 */
public class CalculationContext {

    private final ToolSet toolSet;

    public CalculationContext() {
        this(new ToolSet());
    }

    public CalculationContext(ToolSet toolSet) {
        this.toolSet = toolSet;
    }

    public ToolSet getToolSet() {
        return this.toolSet;
    }
}
