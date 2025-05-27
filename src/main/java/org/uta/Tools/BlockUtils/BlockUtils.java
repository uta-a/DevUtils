package org.uta.Tools.BlockUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class BlockUtils {

    public static List<BlockDataSnapshot> scanSphere(Location center, double radius) {
        List<BlockDataSnapshot> snapshots = new ArrayList<>();
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int r = (int) radius;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z <= radius * radius) {
                        Location loc = new Location(world, cx + x, cy + y, cz + z);
                        Block block = loc.getBlock();
                        snapshots.add(new BlockDataSnapshot(loc, block.getType()));
                    }
                }
            }
        }

        return snapshots;
    }

    public static class BlockDataSnapshot {
        public final Location location;
        public final Material type;

        public BlockDataSnapshot(Location location, Material type) {
            this.location = location;
            this.type = type;
        }
    }

    public static void replaceBlocks(List<BlockDataSnapshot> snapshots, Material newMaterial) {
        for (BlockDataSnapshot snapshot : snapshots) {
            snapshot.location.getBlock().setType(newMaterial);
        }
    }
}
