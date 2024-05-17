package org.simplecyber.deaths;

import java.io.File;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Utils {

    private final JavaPlugin plugin;

    /**
     * Construct utils in the context of a plugin
     * @param plugin The plugin to use
     */
    public Utils(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Translate &-style color codes in a string to internal ones
     * @param text The source string
     * @return The translated string
     */
    public static String translateColors(String text) {
        text = ChatColor.translateAlternateColorCodes('&', text);
        final char COLOR_CHAR = ChatColor.COLOR_CHAR;
        final Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(text);
        StringBuffer buffer = new StringBuffer(text.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
                    );
        }
        return matcher.appendTail(buffer).toString();
    }

    /**
     * Fill in placeholders in a string with values
     * @param text The string to fill
     * @param replacements Any replacements to make
     * @return The filled string
     */
    public static String strFill(String text, Object... replacements) {
        for (int i = 0; i < replacements.length; i++) {
            text = text.replace((CharSequence) ("%"+i), (CharSequence) String.valueOf(replacements[i]));
        }
        return text;
    }
    /**
     * Sanitize and format an entity ID to look grammatically correct
     * @param id The ID to format
     * @return The formatted version
     */
    public static String idToDisplayName(String id) {
        String[] parts = id.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            builder.append(part.substring(0, 1).toUpperCase());
            builder.append(part.substring(1).toLowerCase());
            builder.append(" ");
        }
        return builder.toString().trim();
    }
    /**
     * Send a message to a target with optional string replacements (via `strFill()`)
     * @param target The target
     * @param text The string to send
     * @param replacements Additional replacements
     */
    public void sendMsg(Object target, String text, Object... replacements) {
        text = strFill(text, replacements);
        text = translateColors(text);
        if (target instanceof Player) {
            ((Player) target).sendMessage(text);
        } else if (target instanceof ConsoleCommandSender) {
            ((ConsoleCommandSender) target).sendMessage(text);
        } else if (target instanceof CommandSender) {
            ((CommandSender) target).sendMessage(text);
        } else {
            log("warning", "Tried sending a message to an invalid target!");
            return;
        }
    }
    /**
     * Log to the console with a type
     * @param type The log type
     * @param text The log message
     */
    public void log(String type, String text) {
        Level level;
        switch (type) {
            case "info":
                level = Level.INFO;
                break;
            case "warning":
                level = Level.WARNING;
                break;
            default:
                level = Level.INFO;
                break;
        }
        plugin.getLogger().log(level, text);
    }
    /**
     * Log INFO to the console
     * @param text The log message
     */
    public void log(String text) {
        log("info", text);
    }
    private int lastConfigVersion = 1;
    public FileConfiguration loadConfig(int version) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        int savedVersion = plugin.getConfig().getInt("config_version");
        if (savedVersion > 0 && savedVersion != version) {
            log("Config version mismatch! Refreshing config file...");
            File dataFolder = plugin.getDataFolder();
            File configFile = new File(dataFolder, "config.yml");
            File newFile = new File(dataFolder, strFill("config-%0.yml", System.currentTimeMillis()));
            configFile.renameTo(newFile);
        }
        lastConfigVersion = version;
        plugin.reloadConfig();
        return plugin.getConfig();
    }
    public FileConfiguration reloadConfig() {
        FileConfiguration config = loadConfig(lastConfigVersion);
        log("Config reloaded!");
        return config;
    }
}