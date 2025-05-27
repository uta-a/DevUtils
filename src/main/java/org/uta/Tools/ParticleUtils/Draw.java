package org.uta.Tools.ParticleUtils;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class Draw {

    public static void circle(Location center, double radius, Particle particle, int density, int countPerPoint) {
        if (density <= 0) density = 1;
        if (countPerPoint <= 0) countPerPoint = 1;

        World world = center.getWorld();

        for (int i = 0; i < density; i++) {
            double angle = 2 * Math.PI * i / density;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            double y = center.getY();

            Location particleLoc = new Location(world, x, y, z);
            world.spawnParticle(particle, particleLoc, countPerPoint, 0, 0, 0, 0);
        }
    }

    public static void sphere(Location center, double radius, Particle particle, int density, int countPerPoint) {
        if (density <= 0) density = 1;
        if (countPerPoint <= 0) countPerPoint = 1;

        World world = center.getWorld();

        for (int i = 0; i < density; i++) {
            double theta = Math.acos(1 - 2.0 * i / density); // 緯度方向（0〜π）
            double phi = Math.PI * (1 + Math.sqrt(5)) * i;   // 経度方向（黄金角）

            double x = radius * Math.sin(theta) * Math.cos(phi);
            double y = radius * Math.cos(theta);
            double z = radius * Math.sin(theta) * Math.sin(phi);

            Location particleLoc = center.clone().add(x, y, z);
            world.spawnParticle(particle, particleLoc, countPerPoint, 0, 0, 0, 0);
        }
    }

    public static void line(Location start, Vector direction, double length, Particle particle, double step, int countPerPoint, boolean ignoreBlocked) {
        if (step <= 0) step = 0.1;
        if (countPerPoint <= 0) countPerPoint = 1;

        World world = start.getWorld();
        Vector unitDir = direction.clone().normalize();
        int steps = (int) (length / step);

        for (int i = 0; i <= steps; i++) {
            Vector offset = unitDir.clone().multiply(i * step);
            Location loc = start.clone().add(offset);

            // ブロック衝突チェック
            if (!ignoreBlocked) {
                RayTraceResult result = world.rayTraceBlocks(start, unitDir, i * step, FluidCollisionMode.NEVER, true);
                if (result != null && result.getHitBlock() != null) {
                    // 衝突したブロック位置を超えたら終了
                    if (result.getHitPosition().distance(start.toVector()) <= offset.length()) {
                        break;
                    }
                }
            }

            world.spawnParticle(particle, loc, countPerPoint, 0, 0, 0, 0);
        }
    }

}
