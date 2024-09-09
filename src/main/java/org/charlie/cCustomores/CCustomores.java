package org.charlie.cCustomores;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class CCustomores extends JavaPlugin implements Listener {

    private final Set<String> spawnedChunks = new HashSet<>();  // Track chunks where a chest has been spawned
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration chestConfig;  // Configuration for chests
    private Map<Player, Map<String, Boolean>> playerOreEffects = new HashMap<>();  // Tracks whether player has ore effects applied for each ore

    @Override
    public void onEnable() {
        // Load config and messages
        saveDefaultConfig();
        config = getConfig();
        loadMessages();
        loadOresConfig();  // Load ores.yml

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("CCustomores plugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CCustomores plugin disabled.");
    }

    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void loadOresConfig() {
        File chestFile = new File(getDataFolder(), "ores.yml");
        if (!chestFile.exists()) {
            saveResource("ores.yml", false);
        }
        chestConfig = YamlConfiguration.loadConfiguration(chestFile);
    }

    private String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', messages.getString(key, ""));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("ccustomores")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("ccustomores.reload")) {
                    reloadConfig();
                    config = getConfig();
                    loadMessages();
                    loadOresConfig();  // Reload chests config on reload
                    sender.sendMessage(ChatColor.GREEN + "CCustomores configuration and messages reloaded.");
                } else {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                }
                return true;
            }
        }
        return false;
    }

    // Listen for when a chunk is loaded
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        String chunkKey = chunk.getX() + "," + chunk.getZ();

        // Only attempt to spawn if the chunk is new (hasn't been loaded before) and no chest has been spawned in this chunk
        if (!spawnedChunks.contains(chunkKey)) {
            World world = chunk.getWorld();
            Random random = new Random();

            // For each ore, check spawn chances and light levels from ores.yml
            for (String oreKey : chestConfig.getConfigurationSection("ores").getKeys(false)) {
                int spawnChance = chestConfig.getInt("ores." + oreKey + ".spawn_chance", 2);
                int lightLevel = chestConfig.getInt("ores." + oreKey + ".light_level", 8);

                // Run the spawn chance and attempt to spawn the chest
                if (random.nextInt(spawnChance) == 0) {
                    int chunkX = chunk.getX();
                    int chunkZ = chunk.getZ();
                    spawnOreChest(world, chunkX, chunkZ, oreKey, lightLevel);
                    spawnedChunks.add(chunkKey);  // Mark the chunk as having a chest spawned
                    break;  // Stop after one chest is spawned for the chunk
                }
            }
        }
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerOreEffects.remove(event.getPlayer());  // Reset ore effects tracking
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        applyOreEffects((Player) event.getWhoClicked());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        applyOreEffects((Player) event.getPlayer());
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        applyOreEffects(event.getPlayer());
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        applyOreEffects(event.getPlayer());
    }

    private void applyOreEffects(Player player) {
        for (String oreKey : chestConfig.getConfigurationSection("ores").getKeys(false)) {
            boolean hasOre = false;

            // Check if the player has the ore in their inventory
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.getMaterial(chestConfig.getString("ores." + oreKey + ".type", "IRON_INGOT"))
                        && item.getItemMeta() != null
                        && chestConfig.getString("ores." + oreKey + ".display_name", "§fAmberite").equals(item.getItemMeta().getDisplayName())) {
                    hasOre = true;
                    break;
                }
            }

            Map<String, Boolean> playerEffects = playerOreEffects.getOrDefault(player, new HashMap<>());

            if (hasOre) {
                // If the player has the ore, apply all effects from ores.yml if not already applied
                if (!playerEffects.getOrDefault(oreKey, false)) {
                    // Get the list of effects from ores.yml
                    if (chestConfig.contains("ores." + oreKey + ".potion_effects")) {
                        for (Map<?, ?> effectData : chestConfig.getMapList("ores." + oreKey + ".potion_effects")) {
                            String effectName = (String) effectData.get("effect");

                            // Get the level or default to 1 if not present
                            Integer effectLevel = (Integer) effectData.get("level");
                            if (effectLevel == null) {
                                effectLevel = 1; // Default level is 1
                            }

                            PotionEffectType effectType = PotionEffectType.getByName(effectName);
                            if (effectType != null) {
                                // Apply the potion effect
                                player.addPotionEffect(new PotionEffect(effectType, Integer.MAX_VALUE, effectLevel - 1, true, false));
                            }
                        }
                        player.sendMessage(getMessage("ore_effect_applied").replace("{ore}", oreKey));
                        playerEffects.put(oreKey, true);  // Mark that the player has the effect applied
                    }
                }
            } else {
                // Remove the effects if the player no longer has the ore
                if (playerEffects.getOrDefault(oreKey, false)) {
                    // Get the list of effects from ores.yml
                    if (chestConfig.contains("ores." + oreKey + ".potion_effects")) {
                        for (Map<?, ?> effectData : chestConfig.getMapList("ores." + oreKey + ".potion_effects")) {
                            String effectName = (String) effectData.get("effect");

                            PotionEffectType effectType = PotionEffectType.getByName(effectName);
                            if (effectType != null) {
                                player.removePotionEffect(effectType);
                            }
                        }
                        player.sendMessage(getMessage("ore_effect_removed").replace("{ore}", oreKey));
                    }
                    playerEffects.put(oreKey, false);  // Mark that the effect has been removed
                }
            }

            playerOreEffects.put(player, playerEffects);
        }
    }



    public void spawnOreChest(World world, int chunkX, int chunkZ, String oreKey, int lightLevel) {
        Random random = new Random();
        int x = chunkX * 16 + random.nextInt(16);
        int z = chunkZ * 16 + random.nextInt(16);
        int y;

        // Start checking from y=0 upwards, looking for cave locations
        for (y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
            Block block = world.getBlockAt(x, y, z);
            Block aboveBlock = block.getRelative(0, 1, 0);

            // Check for air block with solid block below it and ensure it's not exposed to the sky
            if (block.getType() == Material.AIR && aboveBlock.getType() == Material.AIR && block.getRelative(0, -1, 0).getType().isSolid()) {
                if (aboveBlock.getLightFromSky() <= lightLevel) {  // Use ore-specific light level setting
                    // Place chest here
                    block.setType(Material.CHEST);

                    // Add the ore item to the chest
                    BlockState state = block.getState();
                    if (state instanceof Chest) {
                        Chest chest = (Chest) state;
                        Inventory inventory = chest.getInventory();
                        ItemStack oreItem = createOreItem(oreKey);
                        inventory.addItem(oreItem);
                        getLogger().info(oreKey + " added to chest at: " + x + ", " + y + ", " + z);
                        String message = getMessage("chest_spawned")
                                .replace("{x}", String.valueOf(x))
                                .replace("{y}", String.valueOf(y))
                                .replace("{z}", String.valueOf(z));

                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.isOp()) {
                                player.sendMessage(message);
                            }
                        }
                    } else {
                        getLogger().warning("Failed to create chest at: " + x + ", " + y + ", " + z);
                    }
                    return;  // Stop further scanning once chest is placed
                }
            }
        }
    }


    private ItemStack createOreItem(String oreKey) {
        Material material = Material.getMaterial(chestConfig.getString("ores." + oreKey + ".type", "IRON_INGOT"));
        String displayName = ChatColor.translateAlternateColorCodes('&', chestConfig.getString("ores." + oreKey + ".display_name", "§fAmberite"));

        ItemStack oreItem = new ItemStack(material);
        ItemMeta meta = oreItem.getItemMeta();
        meta.setDisplayName(displayName);
        oreItem.setItemMeta(meta);
        return oreItem;
    }


    public FileConfiguration getChestConfig() {
        return chestConfig;
    }
}
