package me.nickSoldin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Listener, TabExecutor {

    private boolean distanceEnabled;
    private int radius;
    private boolean worldColorEnabled;
    private ChatColor overworldColor;
    private ChatColor netherColor;
    private ChatColor endColor;
    private int periodicTicks;
    private BukkitTask periodicTask;
    private NickSoldinExpansion papiExpansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("nicksoldin").setExecutor(this);
        getCommand("nicksoldin").setTabCompleter(this);

        // Register PAPI expansion if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiExpansion = new NickSoldinExpansion(this);
            papiExpansion.register();
            getLogger().info("PlaceholderAPI detected, registered placeholders.");
        }

        startPeriodicTask();
        // Apply initial colors & visibility
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (worldColorEnabled) setPlayerNameColor(p);
        }
        fullVisibilityRecalc();
        getLogger().info("nickSoldin enabled!");
    }

    @Override
    public void onDisable() {
        if (papiExpansion != null) {
            try { papiExpansion.unregister(); } catch (Throwable ignored) {}
        }
        if (periodicTask != null) periodicTask.cancel();
        // Ensure everyone is visible if plugin disables
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(target)) continue;
                viewer.showPlayer(this, target);
            }
        }
        getLogger().info("nickSoldin disabled!");
    }

    private void loadConfigValues() {
        distanceEnabled = getConfig().getBoolean("distance-limit.enabled", true);
        radius = Math.max(0, getConfig().getInt("distance-limit.radius", 5));
        worldColorEnabled = getConfig().getBoolean("world-colors.enabled", true);
        periodicTicks = Math.max(0, getConfig().getInt("visibility.periodic-check-ticks", 10));
        try {
            overworldColor = ChatColor.valueOf(getConfig().getString("world-colors.overworld", "WHITE").toUpperCase());
            netherColor = ChatColor.valueOf(getConfig().getString("world-colors.nether", "RED").toUpperCase());
            endColor = ChatColor.valueOf(getConfig().getString("world-colors.end", "LIGHT_PURPLE").toUpperCase());
        } catch (IllegalArgumentException ex) {
            getLogger().warning("Invalid color in config! Falling back to defaults.");
            overworldColor = ChatColor.WHITE;
            netherColor = ChatColor.RED;
            endColor = ChatColor.LIGHT_PURPLE;
        }
    }

    private void startPeriodicTask() {
        if (periodicTask != null) periodicTask.cancel();
        if (periodicTicks > 0) {
            periodicTask = Bukkit.getScheduler().runTaskTimer(this, this::fullVisibilityRecalc, periodicTicks, periodicTicks);
        }
    }

    private void fullVisibilityRecalc() {
        if (!distanceEnabled) {
            // show all
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (viewer.equals(target)) continue;
                    viewer.showPlayer(this, target);
                }
            }
            return;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(target)) continue;
                if (!viewer.getWorld().equals(target.getWorld())) {
                    viewer.hidePlayer(this, target);
                    continue;
                }
                double d = viewer.getLocation().distance(target.getLocation());
                if (d > radius) viewer.hidePlayer(this, target);
                else viewer.showPlayer(this, target);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!distanceEnabled) return;
        Player mover = event.getPlayer();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(mover)) continue;
            if (!viewer.getWorld().equals(mover.getWorld())) {
                viewer.hidePlayer(this, mover);
                continue;
            }
            double d = viewer.getLocation().distance(mover.getLocation());
            if (d > radius) viewer.hidePlayer(this, mover);
            else viewer.showPlayer(this, mover);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Bukkit.getScheduler().runTask(this, this::fullVisibilityRecalc);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (worldColorEnabled) setPlayerNameColor(event.getPlayer());
        Bukkit.getScheduler().runTask(this, this::fullVisibilityRecalc);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (worldColorEnabled) setPlayerNameColor(p);
        Bukkit.getScheduler().runTask(this, this::fullVisibilityRecalc);
    }

    private void setPlayerNameColor(Player player) {
        ChatColor color = getColorForWorld(player.getWorld());
        player.setDisplayName(color + player.getName());
        player.setPlayerListName(color + player.getName());
    }

    ChatColor getColorForWorld(World world) {
        if (world.getEnvironment() == World.Environment.NETHER) return netherColor;
        if (world.getEnvironment() == World.Environment.THE_END) return endColor;
        return overworldColor;
    }

    public boolean isDistanceEnabled() { return distanceEnabled; }
    public boolean isWorldColorEnabled() { return worldColorEnabled; }
    public int getRadius() { return radius; }

    void setDistanceEnabled(boolean enabled) {
        this.distanceEnabled = enabled;
        getConfig().set("distance-limit.enabled", enabled);
        saveConfig();
        fullVisibilityRecalc();
    }

    void setWorldColorEnabled(boolean enabled) {
        this.worldColorEnabled = enabled;
        getConfig().set("world-colors.enabled", enabled);
        saveConfig();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (enabled) setPlayerNameColor(p);
            else {
                // reset to plain name
                p.setDisplayName(p.getName());
                p.setPlayerListName(p.getName());
            }
        }
    }

    void setRadius(int newRadius) {
        this.radius = Math.max(0, newRadius);
        getConfig().set("distance-limit.radius", this.radius);
        saveConfig();
        fullVisibilityRecalc();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("nicksoldin")) return false;

        if (args.length == 0) {
            sender.sendMessage(ChatColor.AQUA + "[nickSoldin] " + ChatColor.WHITE +
                    "radius=" + radius + ", distance=" + distanceEnabled + ", worldcolors=" + worldColorEnabled);
            sender.sendMessage(ChatColor.GRAY + "Usage: /nicksoldin reload|setradius <value>|toggle <distance|worldcolors> <on|off>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("nicksoldin.reload")) {
                sender.sendMessage(ChatColor.RED + "Нет прав (nicksoldin.reload).");
                return true;
            }
            reloadConfig();
            loadConfigValues();
            startPeriodicTask();
            fullVisibilityRecalc();
            sender.sendMessage(ChatColor.GREEN + "Конфиг перезагружен.");
            return true;
        }

        if (args[0].equalsIgnoreCase("setradius")) {
            if (!sender.hasPermission("nicksoldin.setradius")) {
                sender.sendMessage(ChatColor.RED + "Нет прав (nicksoldin.setradius).");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.YELLOW + "Использование: /nicksoldin setradius <число>");
                return true;
            }
            try {
                int value = Integer.parseInt(args[1]);
                setRadius(value);
                sender.sendMessage(ChatColor.GREEN + "Радиус установлен: " + value);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Неверное число: " + args[1]);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("toggle")) {
            if (!sender.hasPermission("nicksoldin.toggle")) {
                sender.sendMessage(ChatColor.RED + "Нет прав (nicksoldin.toggle).");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatColor.YELLOW + "Использование: /nicksoldin toggle <distance|worldcolors> <on|off>");
                return true;
            }
            boolean on = args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("true");
            if (args[1].equalsIgnoreCase("distance")) {
                setDistanceEnabled(on);
                sender.sendMessage(ChatColor.GREEN + "Ограничение видимости: " + (on ? "включено" : "выключено"));
                return true;
            }
            if (args[1].equalsIgnoreCase("worldcolors")) {
                setWorldColorEnabled(on);
                sender.sendMessage(ChatColor.GREEN + "Цвета по мирам: " + (on ? "включены" : "выключены"));
                return true;
            }
            sender.sendMessage(ChatColor.RED + "Неизвестный параметр: " + args[1]);
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("nicksoldin")) return Collections.emptyList();
        if (args.length == 1) return Arrays.asList("reload", "setradius", "toggle");
        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) return Arrays.asList("distance", "worldcolors");
        if (args.length == 3 && args[0].equalsIgnoreCase("toggle")) return Arrays.asList("on", "off");
        return Collections.emptyList();
    }
}
