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

package baritone.bot.pathing.calc.openset;

import baritone.bot.pathing.calc.PathNode;

/**
 * A linked list implementation of an open set. This is the original implementation from MineBot.
 * It has incredibly fast insert performance, at the cost of O(n) removeLowest.
 *
 * @author leijurv
 */
public class LinkedListOpenSet implements IOpenSet {
    private Node first = null;

    @Override
    public boolean isEmpty() {
        return first == null;
    }

    @Override
    public void insert(PathNode pathNode) {
        Node node = new Node();
        node.val = pathNode;
        node.nextOpen = first;
        first = node;
    }

    @Override
    public void update(PathNode node) {

    }

    @Override
    public PathNode removeLowest() {
        if (first == null) {
            return null;
        }
        Node current = first.nextOpen;
        if (current == null) {
            Node n = first;
            first = null;
            return n.val;
        }
        Node previous = first;
        double bestValue = first.val.combinedCost;
        Node bestNode = first;
        Node beforeBest = null;
        while (current != null) {
            double comp = current.val.combinedCost;
            if (comp < bestValue) {
                bestValue = comp;
                bestNode = current;
                beforeBest = previous;
            }
            previous = current;
            current = current.nextOpen;
        }
        if (beforeBest == null) {
            first = first.nextOpen;
            bestNode.nextOpen = null;
            return bestNode.val;
        }
        beforeBest.nextOpen = bestNode.nextOpen;
        bestNode.nextOpen = null;
        return bestNode.val;
    }

    public static class Node { //wrapper with next
        Node nextOpen;
        PathNode val;
    }
}
