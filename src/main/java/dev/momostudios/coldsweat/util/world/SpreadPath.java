package dev.momostudios.coldsweat.util.world;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;

public class SpreadPath
{
    Direction direction;
    BlockPos pos;
    int step = 0;
    boolean frozen = false;

    public SpreadPath(BlockPos pos)
    {
        this.pos = pos;
        this.direction = Direction.UP;
    }

    public SpreadPath(BlockPos pos, Direction direction)
    {
        this.direction = direction;
        this.pos = pos;
    }

    public SpreadPath(int x, int y, int z, Direction direction)
    {
        this.direction = direction;
        this.pos = new BlockPos(x, y, z);
    }

    public SpreadPath(int x, int y, int z, Direction direction, int step)
    {
        this.direction = direction;
        this.pos = new BlockPos(x, y, z);
        this.step = step;
    }

    public BlockPos getPos()
    {
        return this.pos;
    }

    public boolean isFrozen()
    {
        return this.frozen;
    }

    public void freeze()
    {
        this.frozen = true;
    }

    public int getX()
    {
        return pos.getX();
    }
    public int getY()
    {
        return pos.getY();
    }
    public int getZ()
    {
        return pos.getZ();
    }

    public SpreadPath offset(Direction dir)
    {
        return new SpreadPath(this.getX() + dir.getXOffset(), this.getY() + dir.getYOffset(), this.getZ() + dir.getZOffset(), dir);
    }

    public SpreadPath offset(int x, int y, int z)
    {
        return new SpreadPath(this.getX() + x, this.getY() + y, this.getZ() + z, this.direction);
    }

    public SpreadPath offset(BlockPos pos)
    {
        return new SpreadPath(this.getX() + pos.getX(), this.getY() + pos.getY(), this.getZ() + pos.getZ(), this.direction, this.step + 1);
    }

    public boolean withinDistance(Vector3i vector, double distance)
    {
        return distanceSq(vector, false) < distance * distance;
    }

    public double distanceSq(double x, double y, double z)
    {
        double d1 = (double) this.getX() - x;
        double d2 = (double) this.getY() - y;
        double d3 = (double) this.getZ() - z;
        return d1 * d1 + d2 * d2 + d3 * d3;
    }

    public double distanceSq(Vector3i pos, boolean useCenter)
    {
        double d0 = useCenter ? 0.5D : 0.0D;
        double d1 = (double) this.getX() + d0 - pos.getX();
        double d2 = (double) this.getY() + d0 - pos.getY();
        double d3 = (double) this.getZ() + d0 - pos.getZ();
        return d1 * d1 + d2 * d2 + d3 * d3;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof SpreadPath)) return false;
        SpreadPath otherPath = (SpreadPath) other;
        return otherPath.getX() == this.getX()
            && otherPath.getY() == this.getY()
            && otherPath.getZ() == this.getZ();
    }

    @Override
    public String toString()
    {
        return "SpreadPath{" + getX() + ", " + getY() + ", " + getZ() + ", " + direction + "}";
    }
}
