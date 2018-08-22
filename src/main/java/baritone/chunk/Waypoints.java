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

package baritone.chunk;

import net.minecraft.util.math.BlockPos;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Waypoints for a world
 *
 * @author leijurv
 */
public class Waypoints {
    private final Path directory;
    private final Map<Waypoint.Tag, Set<Waypoint>> waypoints;

    Waypoints(Path directory) {
        this.directory = directory;
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException ignored) {}
        }
        System.out.println("Would save waypoints to " + directory);
        this.waypoints = new HashMap<>();
        load();
    }

    private void load() {
        for (Waypoint.Tag tag : Waypoint.Tag.values()) {
            load(tag);
        }
    }

    private synchronized void load(Waypoint.Tag tag) {
        waypoints.put(tag, new HashSet<>());

        Path fileName = directory.resolve(tag.name().toLowerCase());
        if (!Files.exists(fileName))
            return;

        try (
                FileInputStream fileIn = new FileInputStream(fileName.toFile());
                BufferedInputStream bufIn = new BufferedInputStream(fileIn);
                DataInputStream in = new DataInputStream(bufIn);
        ) {
            while (true) {
                String name = in.readUTF();
                long creationTimestamp = in.readLong();
                int x = in.readInt();
                int y = in.readInt();
                int z = in.readInt();
                waypoints.get(tag).add(new Waypoint(name, tag, new BlockPos(x, y, z), creationTimestamp));
            }
        } catch (IOException ex) { }
    }

    private synchronized void save(Waypoint.Tag tag) {
        Path fileName = directory.resolve(tag.name().toLowerCase());
        try (
                FileOutputStream fileOut = new FileOutputStream(fileName.toFile());
                BufferedOutputStream bufOut = new BufferedOutputStream(fileOut);
                DataOutputStream out = new DataOutputStream(bufOut);
        ) {
            for (Waypoint waypoint : waypoints.get(tag)) {
                out.writeUTF(waypoint.name);
                out.writeLong(waypoint.creationTimestamp);
                out.writeInt(waypoint.location.getX());
                out.writeInt(waypoint.location.getY());
                out.writeInt(waypoint.location.getZ());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public Set<Waypoint> getByTag(Waypoint.Tag tag) {
        return Collections.unmodifiableSet(waypoints.get(tag));
    }

    public Waypoint getMostRecentByTag(Waypoint.Tag tag) {
        Set<Waypoint> pts = waypoints.get(tag);
        Waypoint best = null;
        for (Waypoint waypoint : pts) {
            if (best == null || waypoint.creationTimestamp > best.creationTimestamp) {
                best = waypoint;
            }
        }
        return best;
    }

    public void addWaypoint(Waypoint waypoint) {
        // no need to check for duplicate, because it's a Set not a List
        waypoints.get(waypoint.tag).add(waypoint);
        save(waypoint.tag);
    }
}
