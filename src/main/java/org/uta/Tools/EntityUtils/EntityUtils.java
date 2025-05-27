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
            double length,
            int count,
            LivingEntity exclude,
            List<Class<? extends LivingEntity>> allowedTypes,
            boolean ignoreBlocked
    ) {
        Location eyeLoc = viewer.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();
        World world = viewer.getWorld();

        List<LivingEntity> candidates = new ArrayList<>();

        // 周囲を少し広めに検索（視線の延長線上に限るので横幅を狭くする）
        double searchRadius = 1.0; // 視線の周囲1ブロックだけ探すイメージ
        Collection<Entity> nearby = world.getNearbyEntities(eyeLoc, searchRadius, searchRadius, searchRadius);

        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity)) continue;
            if (entity.equals(viewer)) continue;
            if (entity.equals(exclude)) continue;

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

            // 視線ベクトルとターゲットへのベクトルの角度を計算
            Vector toTarget = target.getEyeLocation().toVector().subtract(eyeLoc.toVector());
            double distance = toTarget.length();
            if (distance > length) continue;

            double dot = toTarget.clone().normalize().dot(direction);

            // 視線にほぼ沿っているかどうか（dot = 1に近いほど真っすぐ）
            // 0.95は約18度以内
            if (dot < 0.95) continue;

            // ブロック遮蔽チェック
            if (!ignoreBlocked) {
                RayTraceResult result = world.rayTraceBlocks(
                        eyeLoc,
                        direction,
                        distance,
                        FluidCollisionMode.NEVER,
                        true
                );
                if (result != null && result.getHitBlock() != null) {
                    // 衝突位置がターゲットよりも近ければ遮蔽されている
                    if (result.getHitPosition().distance(eyeLoc.toVector()) < distance) {
                        continue;
                    }
                }
            }

            candidates.add(target);
        }

        // 距離順にソート（近い順）
        candidates.sort(Comparator.comparingDouble(e -> e.getEyeLocation().distanceSquared(eyeLoc)));

        // 最大count件まで返す
        return candidates.subList(0, Math.min(count, candidates.size()));
    }

}
