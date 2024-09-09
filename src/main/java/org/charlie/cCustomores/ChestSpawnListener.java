package org.charlie.cCustomores;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ChestSpawnListener implements Listener {

    private final CCustomores plugin;

    public ChestSpawnListener(CCustomores plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        int chunkX = event.getPlayer().getLocation().getChunk().getX();
        int chunkZ = event.getPlayer().getLocation().getChunk().getZ();

        String oreKey = plugin.getChestConfig().getConfigurationSection("ores").getKeys(false).iterator().next();
        int lightLevel = plugin.getChestConfig().getInt("ores." + oreKey + ".light_level", 8);

        plugin.spawnOreChest(event.getPlayer().getWorld(), chunkX, chunkZ, oreKey, lightLevel);
    }
}
