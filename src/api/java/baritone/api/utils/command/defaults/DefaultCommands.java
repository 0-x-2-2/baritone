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

package baritone.api.utils.command.defaults;

import baritone.api.utils.command.Command;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

public class DefaultCommands {
    public static final List<Command> commands = Collections.unmodifiableList(asList(
        new HelpCommand(),
        new SetCommand(),
        new CommandAlias(asList("modified", "mod", "baritone", "modifiedsettings"), "List modified settings", "set modified"),
        new CommandAlias("reset", "Reset all settings or just one", "set reset"),
        new ExcCommand(), // TODO: remove this debug command... eventually
        new GoalCommand(),
        new PathCommand(),
        new ProcCommand(),
        new VersionCommand(),
        new RepackCommand(),
        new BuildCommand(),
        new SchematicaCommand(),
        new ComeCommand(),
        new AxisCommand(),
        new CancelCommand(),
        new ForceCancelCommand(),
        new GcCommand(),
        new InvertCommand(),
        new ClearareaCommand(),
        PauseResumeCommands.pauseCommand,
        PauseResumeCommands.resumeCommand,
        PauseResumeCommands.pausedCommand,
        new TunnelCommand(),
        new RenderCommand(),
        new FarmCommand(),
        new ChestsCommand(),
        new FollowCommand(),
        new ExploreFilterCommand(),
        new ReloadAllCommand(),
        new SaveAllCommand(),
        new ExploreCommand(),
        new BlacklistCommand(),
        new FindCommand(),
        new MineCommand(),
        new ClickCommand(),
        new ThisWayCommand(),
        new WaypointsCommand(),
        new CommandAlias("sethome", "Sets your home waypoint", "waypoints save home"),
        new CommandAlias("home", "Set goal to your home waypoint", "waypoints goal home"),
        new SelCommand()
    ));
}
