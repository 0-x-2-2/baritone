package baritone.bot.behavior.impl;

import baritone.bot.pathing.movement.MovementHelper;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.Helper;
import baritone.bot.utils.Rotation;
import baritone.bot.utils.Utils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.*;

import java.util.Optional;

public final class LookBehaviorUtils implements Helper {

    public static final Vec3d[] BLOCK_SIDE_MULTIPLIERS = new Vec3d[]{
            new Vec3d(0, 0.5, 0.5),
            new Vec3d(1, 0.5, 0.5),
            new Vec3d(0.5, 0, 0.5),
            new Vec3d(0.5, 1, 0.5),
            new Vec3d(0.5, 0.5, 0),
            new Vec3d(0.5, 0.5, 1)
    };

    /**
     * Calculates a vector given a rotation array
     *
     * @param rotation {@link LookBehavior#target}
     * @return vector of the rotation
     */
    public static Vec3d calcVec3dFromRotation(Rotation rotation) {
        float f = MathHelper.cos(-rotation.getFirst() * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-rotation.getFirst() * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-rotation.getSecond() * 0.017453292F);
        float f3 = MathHelper.sin(-rotation.getSecond() * 0.017453292F);
        return new Vec3d((double) (f1 * f2), (double) f3, (double) (f * f2));
    }

    public static Optional<Rotation> reachable(BlockPos pos) {
        if (pos.equals(getSelectedBlock().orElse(null))) {
            return Optional.of(new Rotation(mc.player.rotationYaw, mc.player.rotationPitch));
        }
        Optional<Rotation> possibleRotation = reachableCenter(pos);
        if (possibleRotation.isPresent())
            return possibleRotation;

        IBlockState bstate = BlockStateInterface.get(pos);
        AxisAlignedBB bbox = bstate.getBoundingBox(mc.world, pos);
        for (Vec3d vecMult : BLOCK_SIDE_MULTIPLIERS) {
            double xDiff = bbox.minX * vecMult.x + bbox.maxX * (1 - vecMult.x);//lol
            double yDiff = bbox.minY * vecMult.y + bbox.maxY * (1 - vecMult.y);
            double zDiff = bbox.minZ * vecMult.z + bbox.maxZ * (1 - vecMult.z);
            double x = pos.getX() + xDiff;
            double y = pos.getY() + yDiff;
            double z = pos.getZ() + zDiff;
            possibleRotation = reachableRotation(pos, new Vec3d(x, y, z));
            if (possibleRotation.isPresent())
                return possibleRotation;
        }
        return Optional.empty();
    }

    private static RayTraceResult rayTraceTowards(Rotation rotation) {
        double blockReachDistance = (double) mc.playerController.getBlockReachDistance();
        Vec3d vec3 = mc.player.getPositionEyes(1.0F);
        Vec3d vec31 = calcVec3dFromRotation(rotation);
        Vec3d vec32 = vec3.add(vec31.x * blockReachDistance,
                vec31.y * blockReachDistance,
                vec31.z * blockReachDistance);
        return mc.world.rayTraceBlocks(vec3, vec32, false, false, true);
    }

    /**
     * Checks if coordinate is reachable with the given block-face rotation offset
     *
     * @param pos
     * @param offset
     * @return
     */
    protected static Optional<Rotation> reachableRotation(BlockPos pos, Vec3d offset) {
        Rotation rotation = Utils.calcRotationFromVec3d(mc.player.getPositionEyes(1.0F),
                offset);
        RayTraceResult result = rayTraceTowards(rotation);
        if (result != null
                && result.typeOfHit == RayTraceResult.Type.BLOCK
                && result.getBlockPos().equals(pos))
            return Optional.of(rotation);
        return Optional.empty();
    }

    /**
     * Checks if center of block at coordinate is reachable
     *
     * @param pos
     * @return
     */
    protected static Optional<Rotation> reachableCenter(BlockPos pos) {
        Rotation rotation = Utils.calcRotationFromVec3d(mc.player.getPositionEyes(1.0F),
                Utils.calcCenterFromCoords(pos, mc.world));
        RayTraceResult result = rayTraceTowards(rotation);
        if (result != null
                && result.typeOfHit == RayTraceResult.Type.BLOCK
                && result.getBlockPos().equals(pos))
            return Optional.of(rotation);
        return Optional.empty();
    }

    /**
     * The currently highlighted block.
     * Updated once a tick by Minecraft.
     *
     * @return the position of the highlighted block
     */
    public static Optional<BlockPos> getSelectedBlock() {
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
            return Optional.of(mc.objectMouseOver.getBlockPos());
        }
        return Optional.empty();
    }

}
