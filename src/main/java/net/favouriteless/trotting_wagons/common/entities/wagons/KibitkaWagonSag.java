package net.favouriteless.trotting_wagons.common.entities.wagons;

import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;

/**
 * Missing-wheel pose for the kibitka. Pitch, roll and drop are solved so every
 * remaining wheel's tread sits on the ground and the frame stops on the first
 * rigid part that meets it. While the tent is opening, the pole bottoms are
 * extra supports: GeckoLib scales {@code Tent_OPEN} around its pivot, and the
 * same smoothstep is applied here so those bottoms stay on the ground.
 *
 * <p>Coordinates are the frame the renderer sags in. GeckoLib mirrors blockbench X
 * when it bakes, then {@code GeoEntityRenderer} turns the model 180° around Y, so
 * visual X stays blockbench X and visual Z is negated blockbench Z. A pixel is 1/16 block.
 */
final class KibitkaWagonSag {

    private static final double PIXEL = 1D / 16D;
    /** Cabin, the point a one-wheel search tries to bring down. */
    private static final double CM_Y = 16D * PIXEL;
    private static final double CM_Z = -3D * PIXEL;
    /** Pole bottoms already sit on the ground. A smaller gap is not a penetration. */
    private static final double GROUND_SLOP = 0.02D;

    private static final double[] POLE_PIVOT = {30D, 15.56791D, -2.98918D};
    /** Bottom corners of the two Tent_OPEN poles, in blockbench pixels. */
    private static final double[][] POLES = {
            {31, -0.09876, 12.34415},
            {34, -0.09876, 12.34415},
            {31, -0.09876, 15.34415},
            {34, -0.09876, 15.34415},
            {31, -0.09876, -11.65585},
            {34, -0.09876, -11.65585},
            {31, -0.09876, -8.65585},
            {34, -0.09876, -8.65585}
    };

    /** Kind ordinal, slot, then x, y, z in blocks. Y is also the tread radius. Kind 0 uses the regular wheel. */
    private static final double[][][] PIVOT = pivots();
    private static final double[][] FRAME = frame();
    private static final double[][] AXLE = axles(false);
    private static final double[][] AXLE_IRON = axles(true);

    private KibitkaWagonSag() {}

    static AbstractWagon.WheelSag pose(int mask, int[] kinds, float tent) {
        mask &= 0x0F;
        double scale = tent < 0.02F ? 0D : tent;
        boolean iron = ironAxles(mask, kinds);
        int count = Integer.bitCount(mask);
        if(count == 0)
            return AbstractWagon.WheelSag.NONE;
        if(count >= 3) {
            if(sharesGround(mask, kinds) && supportMin(0, 0, scale, iron) >= -GROUND_SLOP)
                return AbstractWagon.WheelSag.NONE;
            double[] solved = solveLevel(mask, kinds, scale, iron);
            return new AbstractWagon.WheelSag((float)solved[0], (float)solved[1], solved[2]);
        }
        double[] solved = count == 2 ? solvePair(mask, kinds, scale, iron) : solveFlat(mask, kinds, scale, iron);
        return new AbstractWagon.WheelSag((float)solved[0], (float)solved[1], solved[2]);
    }

    private static double[] solveFlat(int mask, int[] kinds, double tent, boolean iron) {
        double[] wheel = present(mask, kinds);
        double bestScore = Double.POSITIVE_INFINITY;
        double bestPitch = 0;
        double bestRoll = 0;
        for(int ip = -80; ip <= 80; ip++) {
            double pitch = ip * 0.25D;
            for(int ir = -80; ir <= 80; ir++) {
                double roll = ir * 0.25D;
                double score = flatScore(wheel, pitch, roll, tent, iron);
                if(score < bestScore) {
                    bestScore = score;
                    bestPitch = pitch;
                    bestRoll = roll;
                }
            }
        }
        for(int ip = -25; ip <= 25; ip++) {
            double pitch = bestPitch + ip * 0.02D;
            for(int ir = -25; ir <= 25; ir++) {
                double roll = bestRoll + ir * 0.02D;
                double score = flatScore(wheel, pitch, roll, tent, iron);
                if(score < bestScore) {
                    bestScore = score;
                    bestPitch = pitch;
                    bestRoll = roll;
                }
            }
        }
        double ground = wheel == null ? supportMin(bestPitch, bestRoll, tent, iron) : contactY(wheel, bestPitch, bestRoll);
        return new double[] {bestPitch, bestRoll, ground};
    }

    private static double flatScore(double[] wheel, double pitch, double roll, double tent, boolean iron) {
        double ground;
        double penetration;
        if(wheel == null) {
            ground = supportMin(pitch, roll, tent, iron);
            penetration = 0;
        } else {
            ground = contactY(wheel, pitch, roll);
            penetration = Math.max(0, ground - supportMin(pitch, roll, tent, iron));
        }
        return pointY(0, CM_Y, CM_Z, pitch, roll) - ground + 5000D * penetration;
    }

    private static double[] solvePair(int mask, int[] kinds, double tent, boolean iron) {
        double[][] both = new double[2][];
        int found = 0;
        for(int slot = 0; slot < 4; slot++) {
            if((mask & (1 << slot)) != 0)
                both[found++] = pivot(kinds[slot], slot);
        }
        double[] a = both[0];
        double[] b = both[1];
        double dirPitch = b[0] - a[0];
        double dirRoll = b[2] - a[2];
        double length = Math.hypot(dirPitch, dirRoll);
        dirPitch /= length;
        dirRoll /= length;
        double perpPitch = -dirRoll;
        double perpRoll = dirPitch;

        double[] best = null;
        long bestKey = Long.MAX_VALUE;
        for(int sign = -1; sign <= 1; sign += 2) {
            double lo = 0;
            double hi = 40;
            for(int step = 0; step < 40; step++) {
                double mid = (lo + hi) * 0.5D;
                double[] pose = equalized(a, b, dirPitch, dirRoll, perpPitch, perpRoll, sign * mid);
                if(supportMin(pose[0], pose[1], tent, iron) > groundOf(a, b, pose[0], pose[1]))
                    lo = mid;
                else
                    hi = mid;
            }
            double[] pose = equalized(a, b, dirPitch, dirRoll, perpPitch, perpRoll, sign * hi);
            double ground = groundOf(a, b, pose[0], pose[1]);
            long key = Math.round(missingY(mask, kinds, pose[0], pose[1], ground) * 1000D);
            if(best == null || key < bestKey || (key == bestKey && pose[0] < best[0])) {
                bestKey = key;
                best = new double[] {pose[0], pose[1], ground};
            }
        }
        return best;
    }

    /** Keeps three or four wheels on one plane when their treads do not already share it. */
    private static double[] solveLevel(int mask, int[] kinds, double tent, boolean iron) {
        double bestScore = Double.POSITIVE_INFINITY;
        double bestPitch = 0;
        double bestRoll = 0;
        for(int ip = -40; ip <= 40; ip++) {
            double pitch = ip * 0.25D;
            for(int ir = -40; ir <= 40; ir++) {
                double roll = ir * 0.25D;
                double score = levelScore(mask, kinds, pitch, roll, tent, iron);
                if(score < bestScore) {
                    bestScore = score;
                    bestPitch = pitch;
                    bestRoll = roll;
                }
            }
        }
        for(int ip = -20; ip <= 20; ip++) {
            double pitch = bestPitch + ip * 0.02D;
            for(int ir = -20; ir <= 20; ir++) {
                double roll = bestRoll + ir * 0.02D;
                double score = levelScore(mask, kinds, pitch, roll, tent, iron);
                if(score < bestScore) {
                    bestScore = score;
                    bestPitch = pitch;
                    bestRoll = roll;
                }
            }
        }
        return new double[] {bestPitch, bestRoll, meanContact(mask, kinds, bestPitch, bestRoll)};
    }

    private static double levelScore(int mask, int[] kinds, double pitch, double roll, double tent, boolean iron) {
        double mean = 0;
        double square = 0;
        int count = 0;
        for(int slot = 0; slot < 4; slot++) {
            if((mask & (1 << slot)) == 0)
                continue;
            double y = contactY(pivot(kinds[slot], slot), pitch, roll);
            mean += y;
            square += y * y;
            count++;
        }
        mean /= count;
        double spread = square / count - mean * mean;
        double penetration = Math.max(0, mean - supportMin(pitch, roll, tent, iron));
        return spread + 5000D * penetration;
    }

    private static double[] equalized(double[] a, double[] b, double dirPitch, double dirRoll,
                                      double perpPitch, double perpRoll, double magnitude) {
        double along = 0;
        for(int step = 0; step < 6; step++) {
            double gap = gap(a, b, dirPitch, dirRoll, perpPitch, perpRoll, magnitude, along);
            double nudged = gap(a, b, dirPitch, dirRoll, perpPitch, perpRoll, magnitude, along + 0.05D);
            double slope = (nudged - gap) / 0.05D;
            if(Math.abs(slope) < 1.0E-8D)
                break;
            along -= gap / slope;
        }
        return new double[] {
                dirPitch * magnitude + perpPitch * along,
                dirRoll * magnitude + perpRoll * along
        };
    }

    private static double gap(double[] a, double[] b, double dirPitch, double dirRoll,
                              double perpPitch, double perpRoll, double magnitude, double along) {
        double pitch = dirPitch * magnitude + perpPitch * along;
        double roll = dirRoll * magnitude + perpRoll * along;
        return contactY(a, pitch, roll) - contactY(b, pitch, roll);
    }

    private static double groundOf(double[] a, double[] b, double pitch, double roll) {
        return 0.5D * (contactY(a, pitch, roll) + contactY(b, pitch, roll));
    }

    private static double meanContact(int mask, int[] kinds, double pitch, double roll) {
        double total = 0;
        int count = 0;
        for(int slot = 0; slot < 4; slot++) {
            if((mask & (1 << slot)) == 0)
                continue;
            total += contactY(pivot(kinds[slot], slot), pitch, roll);
            count++;
        }
        return total / count;
    }

    /** Average height of the empty wheel stations. Lower means the wagon has settled into the gap. */
    private static double missingY(int mask, int[] kinds, double pitch, double roll, double drop) {
        double total = 0;
        int count = 0;
        for(int slot = 0; slot < 4; slot++) {
            if((mask & (1 << slot)) != 0)
                continue;
            double[] pivot = pivot(kinds[slot], slot);
            total += pointY(pivot[0], 0, pivot[2], pitch, roll) - drop;
            count++;
        }
        return total / count;
    }

    private static boolean sharesGround(int mask, int[] kinds) {
        Double first = null;
        for(int slot = 0; slot < 4; slot++) {
            if((mask & (1 << slot)) == 0)
                continue;
            double y = contactY(pivot(kinds[slot], slot), 0, 0);
            if(first == null)
                first = y;
            else if(Math.abs(y - first) > 1.0E-4D)
                return false;
        }
        return first != null;
    }

    /** Iron axles are shown only when every slot is an iron wheel. */
    private static boolean ironAxles(int mask, int[] kinds) {
        if(mask != 0x0F)
            return false;
        for(int kind : kinds) {
            if(kind != 3)
                return false;
        }
        return true;
    }

    private static double[] present(int mask, int[] kinds) {
        for(int slot = 0; slot < 4; slot++) {
            if((mask & (1 << slot)) != 0)
                return pivot(kinds[slot], slot);
        }
        return null;
    }

    private static double[] pivot(int kind, int slot) {
        if(kind < 1 || kind > 3)
            kind = 1;
        return PIVOT[kind][slot];
    }

    private static double supportMin(double pitch, double roll, double tent, boolean iron) {
        double min = minOf(FRAME, pitch, roll);
        min = Math.min(min, minOf(iron ? AXLE_IRON : AXLE, pitch, roll));
        if(tent <= 0)
            return min;
        for(double[] corner : POLES) {
            double x = POLE_PIVOT[0] + (corner[0] - POLE_PIVOT[0]) * tent;
            double y = POLE_PIVOT[1] + (corner[1] - POLE_PIVOT[1]) * tent;
            double z = POLE_PIVOT[2] + (corner[2] - POLE_PIVOT[2]) * tent;
            min = Math.min(min, pointY(x * PIXEL, y * PIXEL, -z * PIXEL, pitch, roll));
        }
        return min;
    }

    private static double minOf(double[][] points, double pitch, double roll) {
        double min = Double.POSITIVE_INFINITY;
        for(double[] point : points)
            min = Math.min(min, pointY(point[0], point[1], point[2], pitch, roll));
        return min;
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

    private static double pointY(double x, double y, double z, double pitchDeg, double rollDeg) {
        double roll = Math.toRadians(rollDeg);
        double pitch = Math.toRadians(pitchDeg);
        double y1 = x * Math.sin(roll) + y * Math.cos(roll);
        return y1 * Math.cos(pitch) - z * Math.sin(pitch);
    }

    private static double[][][] pivots() {
        double[][][] table = new double[4][4][3];
        table[1] = wheelPixels(
                19.25, 9.5, 17,
                -18.75, 9.5, 17,
                19.75, 9.5, -17,
                -19.75, 9.5, -17);
        table[2] = wheelPixels(
                20.25, 9.5, 17,
                -19.75, 9.5, 17,
                21.75, 9.5, -17,
                -21.75, 9.5, -17);
        table[3] = wheelPixels(
                19.25, 9.5, 17,
                -18.75, 9.5, 17,
                19.75, 9.5, -17,
                -19.75, 9.5, -17);
        table[0] = table[1];
        return table;
    }

    private static double[][] wheelPixels(double... values) {
        double[][] wheels = new double[4][3];
        for(int slot = 0; slot < 4; slot++) {
            wheels[slot][0] = values[slot * 3] * PIXEL;
            wheels[slot][1] = values[slot * 3 + 1] * PIXEL;
            wheels[slot][2] = values[slot * 3 + 2] * PIXEL;
        }
        return wheels;
    }

    private static double[][] frame() {
        double[][] points = new double[24][];
        int cursor = 0;
        cursor = box(points, cursor, -10, 7, 15, 3, 4);
        cursor = box(points, cursor, 7, 7, 15, 3, 4);
        cursor = box(points, cursor, -1.5, 8, -27, 3, 46);
        cursor = box(points, cursor, -1, 7.9, -17, 2, 2);
        cursor = box(points, cursor, -17.5, 12, -25.5, 35, 57);
        box(points, cursor, -9, 10.75, -34, 18, 12);
        return points;
    }

    private static double[][] axles(boolean iron) {
        double[][] points = new double[8][];
        if(iron) {
            box(points, box(points, 0, -20, 7.7, -18, 40, 2), -21, 7.7, 16, 42, 2);
        } else {
            box(points, box(points, 0, -18, 8.7, -18, 37, 2), -20, 8.7, 16, 40, 2);
        }
        return points;
    }

    /** Bottom corners of a blockbench box. Z is stored already negated. */
    private static int box(double[][] points, int cursor, double x, double y, double z, double sizeX, double sizeZ) {
        for(int ix = 0; ix < 2; ix++) {
            for(int iz = 0; iz < 2; iz++) {
                points[cursor++] = new double[] {
                        (x + ix * sizeX) * PIXEL,
                        y * PIXEL,
                        -(z + iz * sizeZ) * PIXEL
                };
            }
        }
        return cursor;
    }

}
