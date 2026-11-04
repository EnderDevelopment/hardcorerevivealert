package com.enderdevelopment.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Main extends JavaPlugin {

    // How long a player stays disabled after a hardcore death (milliseconds).
    private long disabledMillis = 24L * 60L * 60L * 1000L; // defaults to 24 hours
    private static final long CHECK_INTERVAL_TICKS = 20L; // check every second

    private final ConcurrentHashMap<UUID, Long> disabledUntil = new ConcurrentHashMap<>();
    private BukkitTask reviveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        disabledMillis = getConfig().getLong("disabled-hours", 24L) * 60L * 60L * 1000L;
        loadDisabledPlayers();

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);

        // Periodically revive players whose 24 hours are up.
        reviveTask = getServer().getScheduler().runTaskTimer(this, this::checkRevives, CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);

        // Apply any still-disabled players who are already online when the plugin loads.
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (isDisabled(uuid)) {
                applyDisabled(player);
            }
        }

        getLogger().info("EndHardcoreRevive enabled.");
    }

    @Override
    public void onDisable() {
        if (reviveTask != null) {
            reviveTask.cancel();
        }
        saveDisabledPlayers();
        getLogger().info("EndHardcoreRevive disabled.");
    }

    /**
     * Called when a player dies in a hardcore world.
     */
    public void onHardcoreDeath(Player player) {
        UUID uuid = player.getUniqueId();
        long until = System.currentTimeMillis() + disabledMillis;
        disabledUntil.put(uuid, until);

        applyDisabled(player);
        broadcastDeathBox(player.getName());
        sendDisabledPoison(player);
        saveDisabledPlayers();
    }

    private void applyDisabled(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(box(
                Component.text("Je bent uitgeschakeld voor 24 uur " + formatRemaining(player.getUniqueId()))
                        .color(NamedTextColor.WHITE)
        ));
    }

    private boolean isDisabled(UUID uuid) {
        Long until = disabledUntil.get(uuid);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            disabledUntil.remove(uuid);
            return false;
        }
        return true;
    }

    private String formatRemaining(UUID uuid) {
        Long until = disabledUntil.get(uuid);
        if (until == null) {
            return "";
        }
        long remaining = until - System.currentTimeMillis();
        long hours = remaining / (60 * 60 * 1000);
        long minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000);
        long seconds = (remaining % (60 * 1000)) / 1000;
        return String.format("(%dh %dm %ds)", hours, minutes, seconds);
    }

    private void checkRevives() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (UUID uuid : disabledUntil.keySet()) {
            Long until = disabledUntil.get(uuid);
            if (until == null) {
                continue;
            }
            if (now >= until) {
                disabledUntil.remove(uuid);
                changed = true;

                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    reviveNow(player);
                }
            }
        }
        if (changed) {
            saveDisabledPlayers();
        }
    }

    /**
     * Revive a player so they can take part in hardcore again.
     */
    public void reviveNow(Player player) {
        disabledUntil.remove(player.getUniqueId());
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.teleport(player.getWorld().getSpawnLocation());

        player.sendMessage(box(
                Component.text("Je bent weer gerevived! Je kunt weer meedoen met hardcore.")
                        .color(NamedTextColor.WHITE)
        ));
        saveDisabledPlayers();
    }

    // ---------------------------------------------------------------------
    // Chat box rendering
    // ---------------------------------------------------------------------

    /**
     * A red box whose content text is white. The block character U+2588 forms
     * the red frame around the white message.
     */
    private Component box(Component content) {
        String top = "\u2588".repeat(16);
        Component red = Component.text(top).color(NamedTextColor.RED);

        Component line = Component.text("\u2588  ").color(NamedTextColor.RED)
                .append(content)
                .append(Component.text("  \u2588").color(NamedTextColor.RED));

        return Component.empty()
                .append(red).appendNewline()
                .append(line).appendNewline()
                .append(red);
    }

    private void broadcastDeathBox(String playerName) {
        Component line = Component.text("hardcore").color(NamedTextColor.WHITE)
                .append(Component.text(" ").color(NamedTextColor.WHITE))
                .append(Component.text("(is uitgeschakeld)").color(NamedTextColor.WHITE));

        Component contentLine = Component.empty()
                .append(Component.text("\u2588  ").color(NamedTextColor.RED))
                .append(line)
                .append(Component.text("  \u2588").color(NamedTextColor.RED));

        Component nameLine = Component.empty()
                .append(Component.text("\u2588  ").color(NamedTextColor.RED))
                .append(Component.text(playerName + " is voor 24 uur uitgeschakeld").color(NamedTextColor.WHITE))
                .append(Component.text("  \u2588").color(NamedTextColor.RED));

        String borderText = "\u2588".repeat(16);
        Component border = Component.text(borderText).color(NamedTextColor.RED);

        Component message = Component.empty()
                .append(border).appendNewline()
                .append(contentLine).appendNewline()
                .append(nameLine).appendNewline()
                .append(border);

        Bukkit.broadcast(message);
    }

    private void sendDisabledPoison(Player player) {
        player.sendMessage(box(
                Component.text("Je bent uitgeschakeld voor 24 uur.")
                        .color(NamedTextColor.WHITE)
        ));
        player.sendMessage(Component.text("Na 24 uur word je automatisch gerevived.").color(NamedTextColor.GRAY));
    }

    // ---------------------------------------------------------------------
    // Persistence (data.yml in the plugin folder)
    // ---------------------------------------------------------------------

    private void loadDisabledPlayers() {
        if (getConfig().getConfigurationSection("players") == null) {
            return;
        }
        for (String key : getConfig().getConfigurationSection("players").getKeys(false)) {
            long until = getConfig().getLong("players." + key + ".until");
            if (until > System.currentTimeMillis()) {
                try {
                    disabledUntil.put(UUID.fromString(key), until);
                } catch (IllegalArgumentException ignored) {
                    // ignore malformed keys
                }
            }
        }
    }

    private void saveDisabledPlayers() {
        getConfig().set("players", null);
        for (UUID uuid : disabledUntil.keySet()) {
            Long until = disabledUntil.get(uuid);
            if (until != null) {
                getConfig().set("players." + uuid + ".until", until);
            }
        }
        saveConfig();
    }
}
