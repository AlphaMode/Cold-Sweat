package dev.momostudios.coldsweat.util.math;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import net.minecraft.entity.Entity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

public class CSMath
{
    /**
     * Converts a double temperature to a different unit. If {@code from} and {@code to} are the same, returns {@code value}.<br>
     * @param value The temperature to convert.
     * @param from The unit to convert from.
     * @param to The unit to convert to.
     * @param absolute Used when dealing with ambient temperatures with Minecraft units.
     * @return The converted temperature.
     */
    public static double convertUnits(double value, Temperature.Units from, Temperature.Units to, boolean absolute)
    {
        switch (from)
        {
            case C:
                switch (to)
                {
                    case C: return value;
                    case F: return value * 1.8 + 32d;
                    case MC: return value / 23.333333333d;
                }
            case F:
                switch (to)
                {
                    case C: return (value - 32) / 1.8;
                    case F: return value;
                    case MC: return (value - (absolute ? 32d : 0d)) / 42d;
                }
            case MC:
                switch (to)
                {
                    case C: return value * 23.333333333d;
                    case F: return value * 42d + (absolute ? 32d : 0d);
                    case MC: return value;
                }
            default: return value;
        }
    }

    public static float toRadians(float input) {
        return input * (float) (Math.PI / 180);
    }

    public static float toDegrees(float input) {
        return input * (float) (180 / Math.PI);
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : value > max ? max : value;
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : value > max ? max : value;
    }

    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    public static boolean isBetween(double value, double min, double max) {
        return value > min && value < max;
    }

    /**
     * Returns a number between the two given values {@code pointA} and {@code pointB}, based on factor.<br>
     * If {@code factor} = 0, returns {@code pointA}. If {@code factor} = {@code range}, returns {@code pointB}.<br>
     * @param pointA The first value.
     * @param pointB The second value.
     * @param factor The "progress" between pointA and pointB.
     * @param rangeMin The minimum of the range of values over which to interpolate.
     * @param rangeMax The maximum of the range of values over which to interpolate.
     * @return The interpolated value.
     */
    public static double blend(double pointA, double pointB, double factor, double rangeMin, double rangeMax)
    {
        if (factor <= rangeMin) return pointA;
        if (factor >= rangeMax) return pointB;
        return ((1 / (rangeMax - rangeMin)) * (factor - rangeMin)) * (pointB - pointA) + pointA;
    }

    public static double getDistance(Entity entity, Vector3d pos)
    {
        return getDistance(entity, pos.x, pos.y, pos.z);
    }

    public static double getDistance(Entity entity, double x, double y, double z)
    {
        double xDistance = Math.abs(entity.getPosX() - x);
        double yDistance = Math.abs(entity.getPosY() + entity.getHeight() / 2 - y);
        double zDistance = Math.abs(entity.getPosZ() - z);
        return Math.sqrt(xDistance * xDistance + yDistance * yDistance + zDistance * zDistance);
    }

    public static double average(Number... values)
    {
        double sum = 0;
        for (Number value : values)
        {
            sum += value.doubleValue();
        }
        return sum / values.length;
    }

    /**
     * Takes an average of the two values, with weight<br>
     * The weight of an item should NEVER be 0.<br>
     * @param val1 The first value.
     * @param val2 The second value.
     * @param weight1 The weight of the first value.
     * @param weight2 The weight of the second value.
     * @return The weighted average.
     */
    public static double weightedAverage(double val1, double val2, double weight1, double weight2)
    {
        return (val1 * weight1 + val2 * weight2) / (weight1 + weight2);
    }

    /**
     * Takes an average of all the values in the given map, with weight<br>
     * The weight of an item should NEVER be 0.<br>
     * <br>
     * @param values The map of values to average (value, weight).
     * @return The average of the values in the given array.
     */
    public static double weightedAverage(Map<Double, Double> values)
    {
        double sum = 0;
        double weightSum = 0;
        for (Map.Entry<Double, Double> entry : values.entrySet())
        {
            sum += entry.getKey() * entry.getValue();
            weightSum += entry.getValue();
        }
        return sum / weightSum;
    }

    public static Direction getDirectionFromVector(double x, double y, double z)
    {
        Direction direction = Direction.NORTH;
        double f = Float.MIN_VALUE;

        for(Direction direction1 : Direction.values())
        {
            double f1 = x * direction1.getXOffset() + y * direction1.getYOffset() + z * direction1.getZOffset();

            if (f1 > f)
            {
                f = f1;
                direction = direction1;
            }
        }

        return direction;
    }

    /**
     * Lambda-based "for each" loop with break functionality
     * @param collection The collection to iterate through
     * @param consumer A consumer containing the element of the current iteration and the streamer itself
     */
    public static <T> void breakableForEach(Collection<T> collection, BiConsumer<T, InterruptableStreamer<T>> consumer)
    {
        new InterruptableStreamer<T>(collection).run(consumer);
    }

    public static void tryCatch(Runnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (Throwable ignored) {}
    }

    /**
     * @return 1 if the given value is positive, -1 if it is negative, 0 if it is 0.
     */
    public static int getSign(Number value)
    {
        if (value.intValue() == 0) return 0;
        return value.doubleValue() > 0 ? 1 : -1;
    }

    /**
     * Returns 1 if the given value is above the range, -1 if it is below the range, and 0 if it is within the range.
     */
    public static int getSignForRange(double value, double min, double max)
    {
        return value > max ? 1 : value < min ? -1 : 0;
    }

    public static double crop(double value, int sigFigs)
    {
        return (int) Math.floor(value * Math.pow(10.0, sigFigs)) / Math.pow(10.0, sigFigs);
    }

    /**
     * @return The value that is farther from 0.
     */
    public static double getMostExtreme(double value1, double value2)
    {
        return Math.abs(value1) > Math.abs(value2) ? value1 : value2;
    }

    /**
     * @return The value that is closer to 0.
     */
    public static double getLeastExtreme(double value1, double value2)
    {
        return Math.abs(value1) < Math.abs(value2) ? value1 : value2;
    }

    public static double distance(Vector3i pos1, Vector3i pos2)
    {
        return Math.sqrt(pos1.distanceSq(pos2));
    }

    public static Vector3d getMiddle(BlockPos pos)
    {
        return new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public static Vector3d add(Vector3d vec3d, Vector3i vec3i)
    {
        return vec3d.add(vec3i.getX(), vec3i.getY(), vec3i.getZ());
    }
}
