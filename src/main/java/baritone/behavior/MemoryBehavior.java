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
import baritone.api.event.events.BlockInteractEvent;
import baritone.api.event.events.PacketEvent;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.type.EventState;
import baritone.cache.ContainerMemory;
import baritone.cache.Waypoint;
import baritone.pathing.movement.CalculationContext;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.server.SPacketCloseWindow;
import net.minecraft.network.play.server.SPacketOpenWindow;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Brady
 * @since 8/6/2018
 */
public final class MemoryBehavior extends Behavior {

    private final List<FutureInventory> futureInventories = new ArrayList<>(); // this is per-bot

    public MemoryBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public synchronized void onPlayerUpdate(PlayerUpdateEvent event) {
        if (event.getState() == EventState.PRE) {
            updateInventory();
        }
    }

    @Override
    public synchronized void onSendPacket(PacketEvent event) {
        Packet p = event.getPacket();

        if (event.getState() == EventState.PRE) {
            if (p instanceof CPacketPlayerTryUseItemOnBlock) {
                CPacketPlayerTryUseItemOnBlock packet = event.cast();

                TileEntity tileEntity = ctx.world().getTileEntity(packet.getPos());
                if (tileEntity != null) {
                    System.out.println(tileEntity.getPos() + " " + packet.getPos());
                    System.out.println(tileEntity);
                }

                // Ensure the TileEntity is a container of some sort
                if (tileEntity instanceof TileEntityLockable) {

                    TileEntityLockable lockable = (TileEntityLockable) tileEntity;
                    int size = lockable.getSizeInventory();
                    BlockPos position = tileEntity.getPos();
                    BlockPos adj = neighboringConnectedBlock(position);
                    System.out.println(position + " " + adj);
                    if (adj != null) {
                        size *= 2; // double chest or double trapped chest
                        if (adj.getX() < position.getX() || adj.getZ() < position.getZ()) {
                            position = adj; // standardize on the lower coordinate, regardless of which side of the large chest we right clicked
                        }
                    }

                    this.futureInventories.add(new FutureInventory(System.nanoTime() / 1000000L, size, lockable.getGuiID(), position));
                }
            }

            if (p instanceof CPacketCloseWindow) {
                updateInventory();
            }
        }
    }

    @Override
    public synchronized void onReceivePacket(PacketEvent event) {
        Packet p = event.getPacket();

        if (event.getState() == EventState.PRE) {
            if (p instanceof SPacketOpenWindow) {
                SPacketOpenWindow packet = event.cast();
                // Remove any entries that were created over a second ago, this should make up for INSANE latency
                futureInventories.removeIf(i -> System.nanoTime() / 1000000L - i.time > 1000);

                System.out.println("Received packet " + packet.getGuiId() + " " + packet.getEntityId() + " " + packet.getSlotCount() + " " + packet.getWindowId());

                futureInventories.stream()
                        .filter(i -> i.type.equals(packet.getGuiId()) && i.slots == packet.getSlotCount())
                        .findFirst().ifPresent(matched -> {
                    // Remove the future inventory
                    futureInventories.remove(matched);

                    // Setup the remembered inventory
                    getCurrentContainer().setup(matched.pos, packet.getWindowId(), packet.getSlotCount());
                });
            }

            if (p instanceof SPacketCloseWindow) {
                updateInventory();
            }
        }
    }

    @Override
    public void onBlockInteract(BlockInteractEvent event) {
        if (event.getType() == BlockInteractEvent.Type.USE && BlockStateInterface.getBlock(ctx, event.getPos()) instanceof BlockBed) {
            baritone.getWorldProvider().getCurrentWorld().getWaypoints().addWaypoint(new Waypoint("bed", Waypoint.Tag.BED, event.getPos()));
        }
    }

    @Override
    public void onPlayerDeath() {
        baritone.getWorldProvider().getCurrentWorld().getWaypoints().addWaypoint(new Waypoint("death", Waypoint.Tag.DEATH, ctx.playerFeet()));
    }


    private void updateInventory() {
        getCurrentContainer().getInventoryFromWindow(ctx.player().openContainer.windowId).ifPresent(inventory -> inventory.updateFromOpenWindow(ctx));
    }

    private ContainerMemory getCurrentContainer() {
        return (ContainerMemory) baritone.getWorldProvider().getCurrentWorld().getContainerMemory();
    }

    private BlockPos neighboringConnectedBlock(BlockPos in) {
        BlockStateInterface bsi = new CalculationContext(baritone).bsi();
        Block block = bsi.get0(in).getBlock();
        if (block != Blocks.TRAPPED_CHEST && block != Blocks.CHEST) {
            return null; // other things that have contents, but can be placed adjacent without combining
        }
        for (int i = 0; i < 4; i++) {
            BlockPos adj = in.offset(EnumFacing.byHorizontalIndex(i));
            if (bsi.get0(adj).getBlock() == block) {
                return adj;
            }
        }
        return null;
    }

    /**
     * An inventory that we are not yet fully aware of, but are expecting to exist at some point in the future.
     */
    private static final class FutureInventory {

        /**
         * The time that we initially expected the inventory to be provided, in milliseconds
         */
        private final long time;

        /**
         * The amount of slots in the inventory
         */
        private final int slots;

        /**
         * The type of inventory
         */
        private final String type;

        /**
         * The position of the inventory container
         */
        private final BlockPos pos;

        private FutureInventory(long time, int slots, String type, BlockPos pos) {
            this.time = time;
            this.slots = slots;
            this.type = type;
            this.pos = pos;
            System.out.println("Future inventory created " + time + " " + slots + " " + type + " " + pos);
        }
    }
}
