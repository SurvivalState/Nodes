package dev.heypr.nodes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class Node {

    private final String id;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private Material material;
    private float respawnSeconds;
    private boolean broken;
    private long brokenAt;

    public Node(String id, String world, int x, int y, int z, Material material, float respawnSeconds) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.material = material;
        this.respawnSeconds = Math.max(respawnSeconds, 0.05f);
    }

    public static Node at(String id, Location location, Material material, float respawnSeconds) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location has no world");
        }
        return new Node(id, world.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), material, respawnSeconds);
    }

    public void markBroken() {
        markBroken(System.currentTimeMillis());
    }

    public void markBroken(long at) {
        broken = true;
        brokenAt = at;
    }

    public void markRestored() {
        broken = false;
        brokenAt = 0L;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void setRespawnSeconds(float seconds) {
        this.respawnSeconds = Math.max(seconds, 0.05f);
    }

    public float remainingSeconds() {
        if (!broken) {
            return 0f;
        }
        return (float) Math.max(respawnSeconds - (System.currentTimeMillis() - brokenAt) / 1000.0, 0.0);
    }

    public int secondsLeft() {
        return (int) Math.ceil(remainingSeconds());
    }

    public float progress() {
        if (!broken) {
            return 1f;
        }
        return (float) Math.clamp((System.currentTimeMillis() - brokenAt) / 1000.0 / respawnSeconds, 0.0, 1.0);
    }

    public World getWorld() {
        return Bukkit.getWorld(world);
    }

    public Location getLocation() {
        World w = getWorld();
        return w == null ? null : new Location(w, x, y, z);
    }

    public Location getCenter() {
        World w = getWorld();
        return w == null ? null : new Location(w, x + 0.5, y + 0.5, z + 0.5);
    }

    public Block getBlock() {
        World w = getWorld();
        return w == null ? null : w.getBlockAt(x, y, z);
    }

    public boolean isChunkLoaded() {
        World w = getWorld();
        return w != null && w.isChunkLoaded(x >> 4, z >> 4);
    }

    public String getId() {
        return id;
    }

    public String getWorldName() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Material getMaterial() {
        return material;
    }

    public float getRespawnSeconds() {
        return respawnSeconds;
    }

    public boolean isBroken() {
        return broken;
    }

    public long getBrokenAt() {
        return brokenAt;
    }
}
