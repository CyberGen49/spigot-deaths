package org.simplecyber.deaths;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

public class Plugin extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private Utils utils = new Utils(this);

    // On command execution
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Get command name
        String cmdName = cmd.getName().toLowerCase();
        // Initialize runnable for sending usage
        Runnable sendUsage;
        switch (cmdName) {
            case "cyberdeaths": {
                // Set usage runnable
                sendUsage = () -> {
                    utils.sendMsg(sender, "&b%0 &3v%1", getName(), getDescription().getVersion());
                    utils.sendMsg(sender, "&9https://github.com/CyberGen49/spigot-deaths");
                };
                // Send usage if no args were passed
                if (args.length == 0) {
                    sendUsage.run();
                    return true;
                }
                switch (args[0].toLowerCase()) {
                    // Handle reload subcommand
                    case "reload": {
                        // If run by the player, make sure they have permission to reload
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            if (player.hasPermission("cyberdeaths.admin")) {
                                config = utils.reloadConfig();
                                utils.sendMsg(player, config.getString("messages.reloaded"));
                            } else {
                                sendUsage.run();
                            }
                        // Otherwise reload regardless
                        } else {
                            config = utils.reloadConfig();
                        }
                        return true;
                    }
                    // For all other subcommands, send usage
                    default: {
                        sendUsage.run();
                        return true;
                    }
                }
            }
            // For all other commands don't do anything
            default: return true;
        }
    }

    // Add command tab completion
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        // Initialize tab complete options list
        List<String> options = new ArrayList<>();
        switch (cmd.getName().toLowerCase()) {
            // If the cyberdeaths command
            case "cyberdeaths": {
                // If the player has permission to reload, add reload option
                if (args.length == 1 && sender.hasPermission("cyberdeaths.admin")) {
                    options.add("reload");
                }
                break;
            }
        }
        return options;
    }

    // Function to get death message from config
    private String getDeathMsgString(String key) {
        return config.getString("messages.deaths." + key);
    }

    // Player death event
    @EventHandler public void onPlayerDeath(PlayerDeathEvent event) {
        // Get calling player and location
        Player player = event.getEntity();
        Location loc = player.getLocation();
        // Get config options
        boolean sendDeathLocation = config.getBoolean("send_death_location");
        boolean usePlayerDisplaynames = config.getBoolean("use_player_displaynames");
        boolean useMobDisplaynames = config.getBoolean("use_mob_displaynames");
        // If we should show the player their death location...
        if (sendDeathLocation) {
            // Get the world name
            World world = loc.getWorld();
            String worldName = world.getName();
            // Check for world name aliases
            String worldAlias = config.getString("world_name_aliases." + worldName);
            if (worldAlias != null) worldName = worldAlias;
            // Colorize world name
            switch (world.getEnvironment()) {
                case NORMAL:
                    worldName = "&a" + worldName; break;
                case NETHER:
                    worldName = "&c" + worldName; break;
                case THE_END:
                    worldName = "&d" + worldName; break;
                default:
                    worldName = "&7" + worldName; break;
            }
            // Send the player their death location
            utils.sendMsg(player, config.getString("messages.died_at_coords"), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), worldName);
        }
        // Get the player's last damage cause
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        DamageCause cause = lastDamage.getCause();
        String causeString = cause.toString().toLowerCase();
        // Initialize additional variables for damage source
        Entity damager = null;
        EntityType projectileType = null;
        Material fallingBlockType = null;
        try {
            // If the damager is a projectile, get the shooter if it exists
            damager = ((EntityDamageByEntityEvent) lastDamage).getDamager();
            if (damager instanceof Projectile) {
                Projectile projectile = (Projectile) damager;
                projectileType = projectile.getType();
                ProjectileSource shooter = projectile.getShooter();
                if (shooter != null)
                    damager = (Entity) shooter;
            }
        } catch (Exception e) {}
        // If the damage was caused by a falling block, get the block's material
        if (cause == DamageCause.FALLING_BLOCK) {
            fallingBlockType = ((FallingBlock) damager).getBlockData().getMaterial();
        }
        // Create an array of the player's adjacent blocks
        Material[] surroundingBlocks = new Material[]{
            loc.getBlock().getRelative(0, -1, 0).getType(),
            loc.getBlock().getRelative(0, 0, 0).getType(),
            loc.getBlock().getRelative(-1, 0, -1).getType(),
            loc.getBlock().getRelative(0, 0, -1).getType(),
            loc.getBlock().getRelative(1, 0, -1).getType(),
            loc.getBlock().getRelative(-1, 0, 0).getType(),
            loc.getBlock().getRelative(1, 0, 0).getType(),
            loc.getBlock().getRelative(-1, 0, 1).getType(),
            loc.getBlock().getRelative(0, 0, 1).getType(),
            loc.getBlock().getRelative(1, 0, 1).getType(),
            loc.getBlock().getRelative(-1, 1, -1).getType(),
            loc.getBlock().getRelative(0, 1, -1).getType(),
            loc.getBlock().getRelative(1, 1, -1).getType(),
            loc.getBlock().getRelative(-1, 1, 0).getType(),
            loc.getBlock().getRelative(0, 1, 0).getType(),
            loc.getBlock().getRelative(1, 1, 0).getType(),
            loc.getBlock().getRelative(-1, 1, 1).getType(),
            loc.getBlock().getRelative(0, 1, 1).getType(),
            loc.getBlock().getRelative(1, 1, 1).getType(),
            loc.getBlock().getRelative(0, 2, 0).getType(),
        };
        // Initialize message replacement params
        String[] msgArgs = new String[5];
        msgArgs[0] = (usePlayerDisplaynames) ? player.getDisplayName() : player.getName();
        // If there was a damager in this event
        if (damager != null) {
            // Handle projectile-specific messages
            if (causeString.equals("projectile")) {
                causeString = "projectile_entity";
                String newCause = causeString + "_" + projectileType.toString().toLowerCase();
                if (getDeathMsgString(newCause) != null) {
                    causeString = newCause;
                }
            }
            // Handle mob-specific magic messages
            if (causeString.equals("magic")) {
                causeString = "magic_entity";
            }
            // Handle mob-specific messages
            String mobCause = causeString + "_" + damager.getType().toString().toLowerCase();
            if (getDeathMsgString(mobCause) != null) {
                causeString = mobCause;
            }
            // If the damager was a player...
            if (damager instanceof Player) {
                msgArgs[1] = (usePlayerDisplaynames) ? ((Player) damager).getDisplayName() : ((Player) damager).getName();
                // If the player damaged themselves
                if (player.equals((Player) damager)) {
                    causeString = "suicide";
                }
            // Otherwise it was a mob...
            } else {
                // Get mob display name, either automatically, from config, or custom to the mob
                String damagerTypeId = damager.getType().toString();
                String mobName = Utils.idToDisplayName(damagerTypeId);
                String configuredMobName = config.getString("entity_names." + damagerTypeId.toLowerCase());
                String mobCustomName = damager.getCustomName();
                // Set final mob name accordingly
                if (useMobDisplaynames && mobCustomName != null) {
                    mobName = mobCustomName + "&r";
                } else if (configuredMobName != null) {
                    mobName = configuredMobName + "&r";
                }
                msgArgs[1] = mobName;
            }
            // If the damager is a living entity
            if (damager instanceof LivingEntity) {
                // Get and save held item custom name if applicable
                ItemStack item = ((LivingEntity) damager).getEquipment().getItemInMainHand();
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    String itemCustomName = meta.getDisplayName();
                    if (!itemCustomName.equals("")) {
                        String newCause = causeString + "_weapon";
                        if (getDeathMsgString(newCause) != null) {
                            msgArgs[(causeString == "suicide") ? 1 : 2] = itemCustomName + "&r";
                            causeString = newCause;
                        }
                    }
                }
            }
            // If the damager was a falling block
            if (damager instanceof FallingBlock) {
                // Append falling block ID to cause
                String newCause = causeString + "_" + fallingBlockType.toString().toLowerCase();
                // Get configured message if it exists
                if (getDeathMsgString(newCause) != null) {
                    causeString = newCause;
                }
            }
        }
        // If damage was caused by block contact
        if (cause == DamageCause.CONTACT) {
            // Loop through surrounding blocks
            for (int i = 0; i < surroundingBlocks.length; i++) {
                // If block isn't air
                if (surroundingBlocks[i] != Material.AIR) {
                    // Append block ID to cause
                    String newCause = causeString + "_" + surroundingBlocks[i].toString().toLowerCase();
                    // Get configured message if it exists
                    if (getDeathMsgString(newCause) != null) {
                        causeString = newCause;
                        break;
                    }
                }
            }
        }
        // Get the final death message string from the determined cause
        String msg = getDeathMsgString(causeString);
        // Warn on missing message
        if (msg == null) {
            msg = config.getString("messages.death_default");
            utils.log("warning", "Using fallback death message");
        }
        // Format and set the displayed death message
        event.setDeathMessage(Utils.translateColors(config.getString("messages.death_prefix") + Utils.strFill(msg, (Object[])msgArgs)));
    }

    @Override public void onEnable() {
        config = utils.loadConfig(1);
        getServer().getPluginManager().registerEvents(this, this);
        utils.log("Loaded!");
    }
    @Override public void onDisable() {
        utils.log("See ya!");
    }
}