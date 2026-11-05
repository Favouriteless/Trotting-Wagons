package net.favouriteless.trotting_wagons.common.entities.base;

import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon.WheelSag;
import net.favouriteless.trotting_wagons.common.entities.wagons.AnimalWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.ArmoredWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.BigCartWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.BarrelWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.ConestogaWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.DebugWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.LogWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.PassengerWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.RoyalWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.WareWagon;

/**
 * Two opposite wheels and the empty rear foot on the ground, rear down.
 * Numbers are model pixels: four hubs (x, y, z), then the two rear feet, positive X first.
 * Hub Y is the tread radius. Sag Z is the negated model Z.
 */
final class DiagonalSag {

    private static final double PIXEL = 1D / 16D;

    private DiagonalSag() {}

    static WheelSag pose(AbstractWagon wagon, int mask) {
        double[] spec = spec(wagon);
        if(spec == null)
            return null;
        int front;
        int rear;
        int foot;
        if((mask & 0b1001) == 0b1001) {
            front = 0;
            rear = 3;
            foot = 0;
        } else if((mask & 0b0110) == 0b0110) {
            front = 1;
            rear = 2;
            foot = 1;
        } else
            return null;
        double[] a = hub(spec, front);
        double[] b = hub(spec, rear);
        double[] corner = foot(spec, foot);
        double best = Double.POSITIVE_INFINITY;
        double pitch = 0;
        double roll = 0;
        for(int ip = -220; ip <= 10; ip++) {
            double tryPitch = ip * 0.1D;
            for(int ir = -220; ir <= 220; ir++) {
                double tryRoll = ir * 0.1D;
                double score = score(a, b, corner, tryPitch, tryRoll);
                if(score < best) {
                    best = score;
                    pitch = tryPitch;
                    roll = tryRoll;
                }
            }
        }
        for(int ip = -25; ip <= 25; ip++) {
            double tryPitch = pitch + ip * 0.01D;
            for(int ir = -25; ir <= 25; ir++) {
                double tryRoll = roll + ir * 0.01D;
                double score = score(a, b, corner, tryPitch, tryRoll);
                if(score < best) {
                    best = score;
                    pitch = tryPitch;
                    roll = tryRoll;
                }
            }
        }
        return new WheelSag((float)pitch, (float)roll, contactY(a, pitch, roll));
    }

    private static double score(double[] front, double[] rear, double[] foot, double pitch, double roll) {
        double ground = contactY(front, pitch, roll);
        double gap = ground - contactY(rear, pitch, roll);
        double settle = ground - pointY(foot[0], foot[1], foot[2], pitch, roll);
        return gap * gap + settle * settle;
    }

    private static double[] hub(double[] spec, int slot) {
        return point(spec, slot * 3);
    }

    private static double[] foot(double[] spec, int side) {
        return point(spec, 12 + side * 3);
    }

    private static double[] point(double[] spec, int index) {
        return new double[] {spec[index] * PIXEL, spec[index + 1] * PIXEL, -spec[index + 2] * PIXEL};
    }

    private static double pointY(double x, double y, double z, double pitchDeg, double rollDeg) {
        double roll = Math.toRadians(rollDeg);
        double pitch = Math.toRadians(pitchDeg);
        double y1 = x * Math.sin(roll) + y * Math.cos(roll);
        return y1 * Math.cos(pitch) - z * Math.sin(pitch);
    }

    private static double contactY(double[] wheel, double pitchDeg, double rollDeg) {
        double roll = Math.toRadians(rollDeg);
        double pitch = Math.toRadians(pitchDeg);
        double rollSin = Math.sin(roll);
        double rollCos = Math.cos(roll);
        double pitchSin = Math.sin(pitch);
        double pitchCos = Math.cos(pitch);
        double y1 = wheel[0] * rollSin + wheel[1] * rollCos;
        double centerY = y1 * pitchCos - wheel[2] * pitchSin;
        double dirY = rollSin * pitchCos;
        double dot = -dirY;
        double downY = -1D - dirY * dot;
        double downX = -rollCos * dot;
        double downZ = -rollSin * pitchSin * dot;
        double mag = Math.sqrt(downX * downX + downY * downY + downZ * downZ);
        return centerY + wheel[1] * downY / mag;
    }

    /** Four hubs, then the positive-X rear foot and the negative-X rear foot. */
    private static double[] spec(AbstractWagon wagon) {
        if(wagon instanceof BigCartWagon || wagon instanceof DebugWagon)
            return new double[] {
                    24.25, 9.5, -21, -23.75, 9.5, -21, 24.75, 9.5, 27, -24.75, 9.5, 27,
                    10, 6.75, 29, -10, 6.75, 29};
        if(wagon instanceof AnimalWagon)
            return new double[] {
                    19.25, 9.5, -17, -18.75, 9.5, -17, 19.75, 9.5, 29, -19.75, 9.5, 29,
                    10, 7, 31, -10, 7, 31};
        if(wagon instanceof ArmoredWagon)
            return new double[] {
                    16, 8.5, -17, -16, 8.5, -17, 16, 8.5, 17, -16, 8.5, 17,
                    10, 6, 19, -10, 6, 19};
        if(wagon instanceof ConestogaWagon)
            return new double[] {
                    16, 7.15385, -16, -16, 7.15385, -16, 16, 8.5, 17, -16, 8.5, 17,
                    10, 6, 19, -10, 6, 19};
        if(wagon instanceof PassengerWagon)
            return new double[] {
                    19.25, 9.5, -17, -18.75, 9.5, -17, 19.75, 9.5, 29, -19.75, 9.5, 29,
                    7, 4.5, 49.75, -7, 4.5, 49.75};
        if(wagon instanceof RoyalWagon)
            return new double[] {
                    17.75, 10.5, -21, -17.75, 10.5, -21, 17.75, 10.5, 21, -17.75, 10.5, 21,
                    10, 6.75, 19, -10, 6.75, 19};
        if(wagon instanceof WareWagon)
            return new double[] {
                    19.25, 9.5, -17, -18.75, 9.5, -17, 19.75, 9.5, 21, -19.75, 9.5, 21,
                    12, 6, 32, -4, 6, 32};
        if(wagon instanceof LogWagon)
            return new double[] {
                    19.25, 9.5, -17, -18.75, 9.5, -17, 19.75, 9.5, 21, -19.75, 9.5, 21,
                    10, 7, 23, -10, 7, 23};
        if(wagon instanceof BarrelWagon)
            return new double[] {
                    19.25, 9.5, -17, -18.75, 9.5, -17, 19.75, 9.5, 17, -19.75, 9.5, 17,
                    10, 7, 19, -10, 7, 19};
        return null;
    }

}
