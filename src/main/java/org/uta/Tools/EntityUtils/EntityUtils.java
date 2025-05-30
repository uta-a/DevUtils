package org.uta.Tools.EntityUtils;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class EntityUtils {

    public static List<LivingEntity> getNearestLivingEntities(
            Location center,
            double radius,
            int count,
            LivingEntity exclude, // ← 除外したい対象1体
            List<Class<? extends LivingEntity>> allowedTypes // ← Optional 引数
    ) {
        World world = center.getWorld();
        if (world == null) return Collections.emptyList();

        Collection<Entity> nearbyEntities = world.getNearbyEntities(center, radius, radius, radius);

        return nearbyEntities.stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .filter(le -> {
                    if (le.equals(exclude)) return false; // ← 除外対象
                    if (allowedTypes == null || allowedTypes.isEmpty()) return true;

                    for (Class<? extends LivingEntity> allowed : allowedTypes) {
                        if (allowed.isInstance(le)) return true;
                    }
                    return false;
                })
                .sorted(Comparator.comparingDouble(le -> le.getLocation().distanceSquared(center)))
                .limit(count)
                .collect(Collectors.toList());
    }

    public static List<LivingEntity> getEntitiesInSight(
            LivingEntity viewer,
            double radius,
            double fov,
            int count,
            LivingEntity exclude, // ← 除外したい対象1体
            List<Class<? extends LivingEntity>> allowedTypes, // null で全許可
            boolean ignoreBlocked
    ) {
        Location eyeLoc = viewer.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();
        World world = viewer.getWorld();

        double fovRadians = Math.toRadians(fov);
        double cosThreshold = Math.cos(fovRadians / 2.0);
        List<LivingEntity> candidates = new ArrayList<>();

        for (Entity entity : world.getNearbyEntities(eyeLoc, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity)) continue;
            if (entity.equals(viewer)) continue;
            if (exclude != null && entity.equals(exclude)) continue; // ← 除外

            LivingEntity target = (LivingEntity) entity;

            // 許可リストチェック
            if (allowedTypes != null && !allowedTypes.isEmpty()) {
                boolean allowed = false;
                for (Class<? extends LivingEntity> type : allowedTypes) {
                    if (type.isInstance(target)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) continue;
            }

            // ターゲットの中心へ向かうベクトル
            Location targetLoc = target.getLocation().add(0, target.getHeight() / 2.0, 0);
            Vector toTarget = targetLoc.toVector().subtract(eyeLoc.toVector());
            double dot = toTarget.clone().normalize().dot(direction);

            if (dot <= cosThreshold || toTarget.lengthSquared() > radius * radius) continue;

            if (!ignoreBlocked) {
                RayTraceResult result = world.rayTraceBlocks(
                        eyeLoc,
                        toTarget.normalize(),
                        toTarget.length(),
                        FluidCollisionMode.NEVER,
                        true
                );

                if (result != null && result.getHitBlock() != null) continue;
            }

            candidates.add(target);
        }

        candidates.sort(Comparator.comparingDouble(e ->
                e.getLocation().distanceSquared(viewer.getLocation()))
        );

        return candidates.subList(0, Math.min(count, candidates.size()));
    }

    public static List<LivingEntity> getEntitiesOnViewLine(
            LivingEntity viewer,
            double length,      // 視線の長さ（進む距離）
            double maxDistance, // 視線からの最大横ズレ（範囲）
            int maxCount,
            LivingEntity exclude,
            List<Class<? extends LivingEntity>> allowedTypes,
            boolean ignoreBlocked
    ) {
        Location eyeLoc = viewer.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();
        World world = viewer.getWorld();

        List<LivingEntity> found = new ArrayList<>();
        double step = 0.2;  // 間隔

        Vector originVec = eyeLoc.toVector();

        for (double travelled = 0; travelled <= length; travelled += step) {
            Vector currentPoint = originVec.clone().add(direction.clone().multiply(travelled));
            Location checkLoc = currentPoint.toLocation(world);

            // ここでmaxDistanceを使って範囲内のエンティティを探す
            Collection<Entity> nearby = world.getNearbyEntities(checkLoc, maxDistance, maxDistance, maxDistance);

            for (Entity entity : nearby) {
                if (!(entity instanceof LivingEntity)) continue;
                if (entity.equals(viewer)) continue;
                if (exclude != null && entity.equals(exclude)) continue;

                LivingEntity target = (LivingEntity) entity;

                // 許可タイプチェック
                if (allowedTypes != null && !allowedTypes.isEmpty()) {
                    boolean allowed = false;
                    for (Class<? extends LivingEntity> type : allowedTypes) {
                        if (type.isInstance(target)) {
                            allowed = true;
                            break;
                        }
                    }
                    if (!allowed) continue;
                }

                // 重複除外
                if (found.contains(target)) continue;

                // 視線ベクトルからターゲットへの横距離を計算（厳密に範囲内か判定）
                Vector toTarget = target.getEyeLocation().toVector().subtract(originVec);
                // 視線方向への投影距離
                double projectionLength = toTarget.dot(direction);
                if (projectionLength < 0 || projectionLength > length) continue; // 視線の後ろや範囲外

                // 視線方向に沿った点からターゲットまでの距離
                Vector projectedPoint = originVec.clone().add(direction.clone().multiply(projectionLength));
                double lateralDistance = target.getEyeLocation().toVector().distance(projectedPoint);
                if (lateralDistance > maxDistance) continue;

                // ブロック遮蔽チェック
                if (!ignoreBlocked) {
                    RayTraceResult result = world.rayTraceBlocks(
                            eyeLoc,
                            direction,
                            projectionLength,
                            FluidCollisionMode.NEVER,
                            true
                    );
                    if (result != null && result.getHitBlock() != null) {
                        double hitDistance = result.getHitPosition().distance(originVec);
                        if (hitDistance < projectionLength) {
                            continue; // 遮蔽されている
                        }
                    }
                }
                found.add(target);

                if (found.size() >= maxCount) {
                    return found;
                }
            }
        }

        return found;
    }


}
