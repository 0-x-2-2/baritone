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

package cabaletta.comms;

import cabaletta.comms.downward.MessageChat;
import cabaletta.comms.downward.MessageClickSlot;
import cabaletta.comms.downward.MessageComputationRequest;
import cabaletta.comms.upward.MessageComputationResponse;
import cabaletta.comms.upward.MessageEchestConfirmed;
import cabaletta.comms.upward.MessageStatus;

public interface IMessageListener {
    default void handle(MessageStatus message) {
        unhandled(message);
    }

    default void handle(MessageChat message) {
        unhandled(message);
    }

    default void handle(MessageComputationRequest message) {
        unhandled(message);
    }

    default void handle(MessageComputationResponse message) {
        unhandled(message);
    }

    default void handle(MessageEchestConfirmed message) {
        unhandled(message);
    }

    default void handle(MessageClickSlot message) {
        unhandled(message);
    }

    default void unhandled(iMessage msg) {
        // can override this to throw UnsupportedOperationException, if you want to make sure you're handling everything
        // default is to silently ignore messages without handlers
    }
}