package baritone.bot.utils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * @author Brady
 * @since 8/1/2018 12:56 AM
 */
public final class Utils {

    public static Rotation calcRotationFromCoords(BlockPos orig, BlockPos dest) {
        return calcRotationFromVec3d(vec3dFromBlockPos(orig), vec3dFromBlockPos(dest));
    }

    /**
     * Calculates rotation to given Vec<sub>dest</sub> from Vec<sub>orig</sub>
     *
     * @param orig
     * @param dest
     * @return Rotation {@link Rotation}
     */
    public static Rotation calcRotationFromVec3d(Vec3d orig, Vec3d dest) {
        double[] delta = { orig.x - dest.x, orig.y - dest.y, orig.z - dest.z };
        double yaw = Math.atan2(delta[0], -delta[2]);
        double dist = Math.sqrt(delta[0] * delta[0] + delta[2] * delta[2]);
        double pitch = Math.atan2(delta[1], dist);
        return new Rotation(
                (float) radToDeg(yaw),
                (float) radToDeg(pitch)
        );
    }

    /**
     * Calculates rotation to given Vec<sub>dest</sub> from Vec<sub>orig</sub>
     *
     * @param orig
     * @param dest
     * @return Rotation {@link Rotation}
     */
    public static Rotation calcRotationFromVec3d(Vec3d orig, Vec3d dest, Rotation current) {
        return wrapAnglesToRelative(current, calcRotationFromVec3d(orig, dest));
    }

    public static Vec3d calcCenterFromCoords(BlockPos orig, World world) {
        IBlockState b = BlockStateInterface.get(orig);
        AxisAlignedBB bbox = b.getBoundingBox(world, orig);
        double xDiff = (bbox.minX + bbox.maxX) / 2;
        double yDiff = (bbox.minY + bbox.maxY) / 2;
        double zDiff = (bbox.minZ + bbox.maxZ) / 2;
        return new Vec3d(orig.getX() + xDiff,
                orig.getY() + yDiff,
                orig.getZ() + zDiff);
    }

    public static Rotation wrapAnglesToRelative(Rotation current, Rotation target) {
        return new Rotation(
                MathHelper.wrapDegrees(target.getFirst() - current.getFirst()) + current.getFirst(),
                MathHelper.wrapDegrees(target.getSecond() - current.getSecond()) + current.getSecond()
        );
    }

    public static Vec3d vec3dFromBlockPos(BlockPos orig) {
        return new Vec3d(orig.getX() + 0.0D, orig.getY() + 0.0D, orig.getZ() + 0.0D);
    }

    public static double distanceToCenter(BlockPos pos, double x, double y, double z) {
        double xdiff = x - (pos.getX() + 0.5D);
        double ydiff = y - (pos.getY() + 0.5D);
        double zdiff = z - (pos.getZ() + 0.5D);
        return Math.sqrt(xdiff * xdiff + ydiff * ydiff + zdiff * zdiff);
    }

    public static double degToRad(double deg) {
        return deg * Math.PI / 180.0;
    }

    public static double radToDeg(double rad) {
        return rad * 180.0 / Math.PI;
    }
}
