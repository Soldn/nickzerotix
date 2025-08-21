package me.nickSoldin;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NickSoldinExpansion extends PlaceholderExpansion {

    private final Main plugin;

    public NickSoldinExpansion(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nicksoldin";
    }

    @Override
    public @NotNull String getAuthor() {
        return "nickSoldin";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offline, @NotNull String params) {
        String p = params.toLowerCase();
        switch (p) {
            case "radius":
                return String.valueOf(plugin.getRadius());
            case "distance_enabled":
                return String.valueOf(plugin.isDistanceEnabled());
            case "worldcolors_enabled":
                return String.valueOf(plugin.isWorldColorEnabled());
            case "world_color":
                Player player = offline != null ? offline.getPlayer() : null;
                if (player != null && player.isOnline()) {
                    return plugin.getColorForWorld(player.getWorld()).name();
                }
                return "UNKNOWN";
            default:
                return null;
        }
    }
}
