package com.glitchedturtle.paintball;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockIterator;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class PaintballPlugin extends JavaPlugin implements Listener {

    private Set<EntityType> enabledTypeSet;
    private Set<Material> blacklistedBlockTypeSet;
    private List<Byte> paintBlockColor;
    private long resetDelay;

    private ArrayDeque<BlockSnapshot> resetQueue = new ArrayDeque<>();

    public void onEnable() {

        this.loadConfiguration();
        Bukkit.getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {

            if(resetQueue.size() == 0)
                return;
            long curTime = System.currentTimeMillis();
            while(!resetQueue.isEmpty() && resetQueue.peek().getResetAt() <= curTime) {

                BlockSnapshot snap = resetQueue.poll();
                if(snap == null)
                    continue;

                Block blk = snap.getLocation().getBlock();

                blk.setType(snap.getMaterial());
                blk.setData(snap.getData());

            }

        }, 5L, 5L);

    }

    @Override
    public void onDisable() {

        while(!resetQueue.isEmpty()) {

            BlockSnapshot snap = resetQueue.poll();
            Block blk = snap.getLocation().getBlock();

            blk.setType(snap.getMaterial());
            blk.setData(snap.getData());

        }

    }

    private void loadConfiguration() {

        if(!new File(this.getDataFolder(), "config.yml").exists()) {
            this.saveResource("config.yml", false);
            this.reloadConfig();
        }

        enabledTypeSet = new HashSet<>();
        blacklistedBlockTypeSet = new HashSet<>();
        paintBlockColor = new ArrayList<>();

        Configuration conf = this.getConfig();
        resetDelay = conf.getLong("reset_delay");

        for(String type : conf.getStringList("enabled_projectile_types")) {

            try {
                enabledTypeSet.add(EntityType.valueOf(type.toUpperCase()));
            } catch(IllegalArgumentException ex) {
                this.getLogger().log(Level.WARNING, "Entity type '{}' does not exist, skipping", type.toUpperCase());
            }

        }

        for(String type : conf.getStringList("blacklisted_block_types")) {

            try {
                blacklistedBlockTypeSet.add(Material.valueOf(type.toUpperCase()));
            } catch(IllegalArgumentException ex) {
                this.getLogger().log(Level.WARNING, "Block material '{}' does not exist, skipping", type.toUpperCase());
            }

        }

        for(byte paintData : conf.getByteList("painted_data_values")) {

            if(paintData < 0 || paintData > 15) {
                this.getLogger().log(Level.WARNING, "Stained clay data value '{}' is outside of the valid range, skipping", paintData);
                continue;
            }

            paintBlockColor.add(paintData);

        }

    }

    private byte randomPaintColor() {

        Random rand = ThreadLocalRandom.current();
        return paintBlockColor.get(rand.nextInt(paintBlockColor.size()));

    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent ev) {

        if(!enabledTypeSet.contains(ev.getEntityType()))
            return;

        // Find the block that the projectile has hit
        Entity ent = ev.getEntity();
        BlockIterator iterator =
                new BlockIterator(ent.getWorld(), ent.getLocation().toVector(), ent.getVelocity().normalize(), 0, 2);

        Block hit = null;
        while(iterator.hasNext()) {
            hit = iterator.next();
            if(hit != null && hit.getType() != Material.AIR)
                break;
        }

        if(hit == null || blacklistedBlockTypeSet.contains(hit.getType()))
            return;
        resetQueue.add(new BlockSnapshot(hit.getLocation(), hit.getType(), hit.getData(), System.currentTimeMillis() + resetDelay));

        hit.setType(Material.STAINED_CLAY);
        hit.setData(this.randomPaintColor());

        hit.getWorld().playSound(hit.getLocation(), Sound.SLIME_WALK, 1, 1);

    }

}
