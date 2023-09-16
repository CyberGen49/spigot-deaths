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

    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String cmdName = cmd.getName().toLowerCase();
        Runnable sendUsage;
        switch (cmdName) {
            case "cyberdeaths": {
                sendUsage = () -> {
                    utils.sendMsg(sender, "&b%0 &3v%1", getName(), getDescription().getVersion());
                    utils.sendMsg(sender, "&9https://github.com/CyberGen49/spigot-deaths");
                };
                if (args.length == 0) {
                    sendUsage.run();
                    return true;
                }
                switch (args[0].toLowerCase()) {
                    case "reload": {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            if (player.hasPermission("cyberdeaths.admin")) {
                                config = utils.reloadConfig();
                                utils.sendMsg(player, config.getString("messages.reloaded"));
                            } else {
                                sendUsage.run();
                            }
                        } else {
                            config = utils.reloadConfig();
                        }
                        return true;
                    }
                    default: {
                        sendUsage.run();
                        return true;
                    }
                }
            }
            default: return true;
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        switch (cmd.getName().toLowerCase()) {
            case "cyberdeaths": {
                if (args.length == 1 && sender.hasPermission("cyberdeaths.admin")) {
                    options.add("reload");
                }
                break;
            }
        }
        return options;
    }

    private String getDeathMessageType(String key) {
        return config.getString("messages.deaths." + key);
    }

    @EventHandler public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location loc = player.getLocation();
        boolean sendDeathLocation = config.getBoolean("send_death_location");
        boolean usePlayerDisplaynames = config.getBoolean("use_player_displaynames");
        boolean useMobDisplaynames = config.getBoolean("use_mob_displaynames");
        if (sendDeathLocation) {
            World world = loc.getWorld();
            String worldName = world.getName();
            String worldAlias = config.getString("world_name_aliases." + worldName);
            if (worldAlias != null) worldName = worldAlias;
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
            utils.sendMsg(player, config.getString("messages.died_at_coords"), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), worldName);
        }
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        DamageCause cause = lastDamage.getCause();
        String causeString = cause.toString().toLowerCase();
        Entity damager = null;
        EntityType projectileType = null;
        try {
            damager = ((EntityDamageByEntityEvent) lastDamage).getDamager();
            if (damager instanceof Projectile) {
                Projectile projectile = (Projectile) damager;
                projectileType = projectile.getType();
                ProjectileSource shooter = projectile.getShooter();
                if (shooter != null)
                    damager = (Entity) shooter;
            }
        } catch (Exception e) {}
        Material fallingBlockType = null;
        if (cause == DamageCause.FALLING_BLOCK) {
            fallingBlockType = ((FallingBlock) damager).getBlockData().getMaterial();
        }
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
        String[] msgArgs = new String[5];
        msgArgs[0] = (usePlayerDisplaynames) ? player.getDisplayName() : player.getName();
        if (damager != null) {
            // Handle projectile-specific messages
            if (causeString.equals("projectile")) {
                causeString = "projectile_entity";
                String newCause = causeString + "_" + projectileType.toString().toLowerCase();
                if (getDeathMessageType(newCause) != null) {
                    causeString = newCause;
                }
            }
            // Handle mob-specific magic messages
            if (causeString.equals("magic")) {
                causeString = "magic_entity";
            }
            // Handle mob-specific messages
            String mobCause = causeString + "_" + damager.getType().toString().toLowerCase();
            if (getDeathMessageType(mobCause) != null) {
                causeString = mobCause;
            }
            if (damager instanceof Player) {
                msgArgs[1] = (usePlayerDisplaynames) ? ((Player) damager).getDisplayName() : ((Player) damager).getName();
                if (player.equals((Player) damager)) {
                    causeString = "suicide";
                }
            } else {
                String mobName = Utils.idToDisplayName(damager.getType().toString());
                String configuredMobName = config.getString("entity_names." + damager.getType().toString().toLowerCase());
                String mobCustomName = damager.getCustomName();
                if (useMobDisplaynames && mobCustomName != null) {
                    mobName = mobCustomName + "&r";
                } else if (configuredMobName != null) {
                    mobName = configuredMobName + "&r";
                }
                msgArgs[1] = mobName;
            }
            if (damager instanceof LivingEntity) {
                ItemStack item = ((LivingEntity) damager).getEquipment().getItemInMainHand();
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    String itemCustomName = meta.getDisplayName();
                    if (!itemCustomName.equals("")) {
                        String newCause = causeString + "_weapon";
                        if (getDeathMessageType(newCause) != null) {
                            msgArgs[(causeString == "suicide") ? 1 : 2] = itemCustomName + "&r";
                            causeString = newCause;
                        }
                    }
                }
            }
            if (damager instanceof FallingBlock) {
                String newCause = causeString + "_" + fallingBlockType.toString().toLowerCase();
                if (getDeathMessageType(newCause) != null) {
                    causeString = newCause;
                }
            }
        }
        if (cause == DamageCause.CONTACT) {
            for (int i = 0; i < surroundingBlocks.length; i++) {
                if (surroundingBlocks[i] != Material.AIR) {
                    String newCause = causeString + "_" + surroundingBlocks[i].toString().toLowerCase();
                    if (getDeathMessageType(newCause) != null) {
                        causeString = newCause;
                        break;
                    }
                }
            }
        }
        String msg = getDeathMessageType(causeString);
        if (msg == null) {
            msg = config.getString("messages.death_default");
            utils.log("warning", "Using fallback death message");
        }
        event.setDeathMessage(Utils.translateColors(config.getString("messages.death_prefix") + Utils.strFill(msg, msgArgs)));
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