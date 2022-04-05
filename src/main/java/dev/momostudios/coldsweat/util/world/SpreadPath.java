package dev.momostudios.coldsweat.util.world;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;

public class SpreadPath
{
    public Direction origin;
    public int x;
    public int y;
    public int z;
    public int step = 0;
    public boolean isFrozen = false;

    public SpreadPath(BlockPos pos)
    {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.origin = Direction.UP;
    }

    public SpreadPath(BlockPos pos, Direction origin)
    {
        this.origin = origin;
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
    }

    public SpreadPath(int x, int y, int z, Direction origin)
    {
        this.origin = origin;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public SpreadPath(int x, int y, int z, Direction origin, int step)
    {
        this.origin = origin;
        this.x = x;
        this.y = y;
        this.z = z;
        this.step = step;
    }

    public int getX()
    {
        return x;
    }
    public int getY()
    {
        return y;
    }
    public int getZ()
    {
        return z;
    }

    public BlockPos getPos()
    {
        return new BlockPos(this.x, this.y, this.z);
    }

    public SpreadPath offset(Direction dir)
    {
        return new SpreadPath(this.x + dir.getXOffset(), this.y + dir.getYOffset(), this.z + dir.getZOffset(), dir, this.step + 1);
    }

    public SpreadPath offset(int x, int y, int z)
    {
        return new SpreadPath(this.x + x, this.y + y, this.z + z, this.origin, this.step + 1);
    }

    public SpreadPath offset(BlockPos pos)
    {
        return new SpreadPath(this.x + pos.getX(), this.y + pos.getY(), this.z + pos.getZ(), this.origin, this.step + 1);
    }

    public boolean withinDistance(Vector3i vector, double distance) {
        return distanceSq(vector, false) < distance * distance;
    }

    public double distanceSq(double x, double y, double z) {
        double d1 = (double)this.getX() - x;
        double d2 = (double)this.getY() - y;
        double d3 = (double)this.getZ() - z;
        return d1 * d1 + d2 * d2 + d3 * d3;
    }

    public double distanceSq(Vector3i pos, boolean useCenter) {
        double d0 = useCenter ? 0.5D : 0.0D;
        double d1 = (double)this.getX() + d0 - pos.getX();
        double d2 = (double)this.getY() + d0 - pos.getY();
        double d3 = (double)this.getZ() + d0 - pos.getZ();
        return d1 * d1 + d2 * d2 + d3 * d3;
    }
}
