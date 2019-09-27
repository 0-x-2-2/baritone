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

package baritone.api.utils.command.execution;

import baritone.api.utils.command.Command;
import baritone.api.utils.command.argument.CommandArgument;
import baritone.api.utils.command.exception.CommandException;
import baritone.api.utils.command.exception.CommandUnhandledException;
import baritone.api.utils.command.exception.ICommandException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.manager.ICommandManager;
import com.mojang.realmsclient.util.Pair;

import java.util.List;
import java.util.stream.Stream;

public class CommandExecution {

    /**
     * The command itself
     */
    private final Command command;

    /**
     * The name this command was called with
     */
    public final String label;

    /**
     * The arg consumer
     */
    public final ArgConsumer args;

    public CommandExecution(Command command, String label, ArgConsumer args) {
        this.command = command;
        this.label = label;
        this.args = args;
    }

    public static String getLabel(String string) {
        return string.split("\\s", 2)[0];
    }

    public static Pair<String, List<CommandArgument>> expand(String string, boolean preserveEmptyLast) {
        String label = getLabel(string);
        List<CommandArgument> args = CommandArgument.from(string.substring(label.length()), preserveEmptyLast);
        return Pair.of(label, args);
    }

    public static Pair<String, List<CommandArgument>> expand(String string) {
        return expand(string, false);
    }

    public void execute() {
        try {
            command.execute(this);
        } catch (Throwable t) {
            // Create a handleable exception, wrap if needed
            ICommandException exception = t instanceof ICommandException
                    ? (ICommandException) t
                    : new CommandUnhandledException(t);

            exception.handle(command, args.args);
        }
    }

    public Stream<String> tabComplete() {
        return command.tabComplete(this);
    }

    public static CommandExecution from(ICommandManager manager, String label, ArgConsumer args) {
        Command command = manager.getCommand(label);
        if (command == null) {
            return null;
        }
        return new CommandExecution(
                command,
                label,
                args
        );
    }

    public static CommandExecution from(ICommandManager manager, Pair<String, List<CommandArgument>> pair) {
        return from(manager, pair.first(), new ArgConsumer(manager, pair.second()));
    }

    public static CommandExecution from(ICommandManager manager, String string) {
        return from(manager, expand(string));
    }
}
