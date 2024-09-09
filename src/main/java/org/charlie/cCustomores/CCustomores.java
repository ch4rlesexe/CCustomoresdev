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
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class CCustomores extends JavaPlugin implements Listener {

    private final Set<String> spawnedChunks = new HashSet<>();  // Track chunks where a chest has been spawned
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration chestConfig;  // Configuration for chests
    private Map<Player, Map<String, Boolean>> playerOreEffects = new HashMap<>();  // Tracks whether player has ore effects applied for each ore

    @Override
    public void onEnable() {
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
        if (label.equalsIgnoreCase("ccustomore")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("ccustomores.reload")) {
                    reloadPlugin(sender);
                } else {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                }
                return true;
            } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
                giveCustomOre(sender, args[1], args[2], Integer.parseInt(args[3]));
                return true;
            }
        }
        sender.sendMessage(ChatColor.RED + "Usage: /ccustomore <give/reload> [orename] [player] [amount]");
        return false;
    }

    private void reloadPlugin(CommandSender sender) {
        reloadConfig();
        loadMessages();
        loadOresConfig();
        sender.sendMessage(ChatColor.GREEN + "Configuration and messages reloaded.");
    }

    private void giveCustomOre(CommandSender sender, String oreName, String playerName, int amount) {
        Player target = Bukkit.getPlayer(playerName);
        if (target != null) {
            ItemStack oreItem = createOreItem(oreName);
            oreItem.setAmount(amount);
            target.getInventory().addItem(oreItem);
            sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " of " + oreName + " to " + playerName);
        } else {
            sender.sendMessage(ChatColor.RED + "Player not found.");
        }
    }

    // Listen for when a chunk is loaded
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        String chunkKey = chunk.getX() + "," + chunk.getZ();

        if (!spawnedChunks.contains(chunkKey)) {
            World world = chunk.getWorld();
            Random random = new Random();

            for (String oreKey : chestConfig.getConfigurationSection("ores").getKeys(false)) {
                int spawnChance = chestConfig.getInt("ores." + oreKey + ".spawn_chance", 2);
                int lightLevel = chestConfig.getInt("ores." + oreKey + ".light_level", 8);

                if (random.nextInt(spawnChance) == 0) {
                    int chunkX = chunk.getX();
                    int chunkZ = chunk.getZ();
                    spawnOreChest(world, chunkX, chunkZ, oreKey, lightLevel);
                    spawnedChunks.add(chunkKey);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        applyOreEffects(player);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        applyOreEffects(player);
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        applyOreEffects(player);
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        applyOreEffects(player);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyOreEffects(player);
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        applyOreEffects(player);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        // Check if the entity is a player
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            applyOreEffects(player);
        }
    }




    private void applyOreEffects(Player player) {

        for (String oreKey : chestConfig.getConfigurationSection("ores").getKeys(false)) {
            boolean hasOre = false;

            // Check if the player has the ore in their inventory
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null) {

                    if (item.getType() == Material.getMaterial(chestConfig.getString("ores." + oreKey + ".type", "IRON_INGOT"))
                            && item.getItemMeta() != null
                            && item.getItemMeta().hasDisplayName()
                            && item.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', chestConfig.getString("ores." + oreKey + ".display_name")))) {
                        hasOre = true;
                        break;
                    }
                }
            }

            Map<String, Boolean> playerEffects = playerOreEffects.getOrDefault(player, new HashMap<>());

            if (hasOre) {
                if (!playerEffects.getOrDefault(oreKey, false)) {
                    if (chestConfig.contains("ores." + oreKey + ".potion_effects")) {
                        for (Map<?, ?> effectData : chestConfig.getMapList("ores." + oreKey + ".potion_effects")) {
                            String effectName = (String) effectData.get("effect");
                            Object levelObj = effectData.get("level");
                            int effectLevel = (levelObj instanceof Integer) ? (Integer) levelObj : 1;

                            PotionEffectType effectType = PotionEffectType.getByName(effectName);
                            if (effectType != null) {
                                player.addPotionEffect(new PotionEffect(effectType, Integer.MAX_VALUE, effectLevel - 1, true, false));
                            }
                        }
                        player.sendMessage(getMessage("ore_effect_applied").replace("{ore}", oreKey));
                        playerEffects.put(oreKey, true);
                    }
                }
            } else {
                if (playerEffects.getOrDefault(oreKey, false)) {
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
                    playerEffects.put(oreKey, false);
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

        for (y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
            Block block = world.getBlockAt(x, y, z);
            Block aboveBlock = block.getRelative(0, 1, 0);

            if (block.getType() == Material.AIR && aboveBlock.getType() == Material.AIR && block.getRelative(0, -1, 0).getType().isSolid()) {
                if (aboveBlock.getLightFromSky() <= lightLevel) {
                    block.setType(Material.CHEST);

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
                    return;
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
