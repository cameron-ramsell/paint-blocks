package com.glitchedturtle.paintball;

import org.bukkit.Location;
import org.bukkit.Material;

public class BlockSnapshot {

    private Location location;

    private Material material;
    private byte data;

    private long resetAt;

    public BlockSnapshot(Location location, Material material, byte data, long resetAt) {
        this.location = location;
        this.material = material;
        this.data = data;
        this.resetAt = resetAt;
    }

    public Location getLocation() {
        return location;
    }

    public Material getMaterial() {
        return material;
    }

    public byte getData() {
        return data;
    }

    public long getResetAt() {
        return resetAt;
    }

}
