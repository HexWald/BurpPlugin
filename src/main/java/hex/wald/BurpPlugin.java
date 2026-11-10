package hex.wald;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BurpPlugin extends JavaPlugin implements Listener {

    private static final String DOUBLE_SNEAK = "double-sneak";
    private static final String SNEAK_SWAP = "sneak-swap";
    private static final String USE_PERMISSION = "burp.use";
    private static final String RELOAD_PERMISSION = "burp.reload";
    private static final List<String> BINDS = List.of(DOUBLE_SNEAK, SNEAK_SWAP, "off");
    private static final List<String> BIND_MODES = List.of("normal", "quiet");
    private static final long DOUBLE_SNEAK_WINDOW = TimeUnit.MILLISECONDS.toNanos(350);

    private enum BurpMode {
        NORMAL(3), QUIET(1), DEEP(6);

        private final int particles;

        BurpMode(int particles) {
            this.particles = particles;
        }
    }

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> lastSneak = new HashMap<>();

    private NamespacedKey bindKey;
    private NamespacedKey quietBindKey;
    private NamespacedKey muteKey;
    private FileConfiguration settings;
    private BurpBreath breath;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = getConfig();
        bindKey = new NamespacedKey(this, "bind");
        quietBindKey = new NamespacedKey(this, "bind-quiet");
        muteKey = new NamespacedKey(this, "muted");
        breath = new BurpBreath(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::clearExpiredCooldowns, 1200, 1200);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadSettings(sender);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }
        if (!player.hasPermission(USE_PERMISSION)) {
            player.sendMessage("Нет права " + USE_PERMISSION);
            return true;
        }

        if (args.length == 0) {
            burp(player, BurpMode.NORMAL);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("quiet")) {
            burp(player, BurpMode.QUIET);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("deep")) {
            burp(player, BurpMode.DEEP);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("mute")) {
            toggleMute(player);
        } else if (args[0].equalsIgnoreCase("bind")) {
            handleBind(player, args);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean canUse = sender instanceof Player && sender.hasPermission(USE_PERMISSION);
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            if (canUse) {
                commands.addAll(List.of("bind", "quiet", "deep", "mute"));
            }
            if (sender.hasPermission(RELOAD_PERMISSION)) {
                commands.add("reload");
            }
            return commands.stream().filter(subcommand -> subcommand.startsWith(prefix)).toList();
        }
        if (!canUse || args.length < 2 || !args[0].equalsIgnoreCase("bind")) {
            return List.of();
        }
        if (args.length == 2) {
            return BINDS.stream().filter(bind -> bind.startsWith(prefix)).toList();
        }
        if (args.length == 3 && isGesture(args[1].toLowerCase(Locale.ROOT))) {
            return BIND_MODES.stream().filter(mode -> mode.startsWith(prefix)).toList();
        }
        return List.of();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking() || !player.hasPermission(USE_PERMISSION) || !bind(player).equals(DOUBLE_SNEAK)) {
            return;
        }

        UUID id = player.getUniqueId();
        long now = System.nanoTime();
        Long previous = lastSneak.put(id, now);
        if (previous != null && now - previous <= DOUBLE_SNEAK_WINDOW) {
            lastSneak.remove(id);
            burp(player, boundMode(player));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() && player.hasPermission(USE_PERMISSION) && bind(player).equals(SNEAK_SWAP)) {
            event.setCancelled(true);
            burp(player, boundMode(player));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        resetGesture(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        resetGesture(event.getPlayer());
    }

    private String bind(Player player) {
        return player.getPersistentDataContainer().getOrDefault(bindKey, PersistentDataType.STRING, "off");
    }

    private BurpMode boundMode(Player player) {
        return player.getPersistentDataContainer().has(quietBindKey, PersistentDataType.BYTE)
                ? BurpMode.QUIET : BurpMode.NORMAL;
    }

    private boolean isGesture(String bind) {
        return bind.equals(DOUBLE_SNEAK) || bind.equals(SNEAK_SWAP);
    }

    private void handleBind(Player player, String[] args) {
        if (args.length == 1) {
            String bind = bind(player);
            String mode = isGesture(bind) ? " " + boundMode(player).name().toLowerCase(Locale.ROOT) : "";
            player.sendMessage("Текущий бинд: " + bind + mode);
        } else if (args.length <= 3) {
            String bind = args[1].toLowerCase(Locale.ROOT);
            String mode = args.length == 3 ? args[2].toLowerCase(Locale.ROOT) : "normal";
            if ((isGesture(bind) && BIND_MODES.contains(mode)) || (bind.equals("off") && args.length == 2)) {
                setBind(player, bind, mode.equals("quiet"));
                return;
            }
        }
        player.sendMessage("/burp bind <double-sneak|sneak-swap> [normal|quiet] или /burp bind off");
    }

    private void setBind(Player player, String bind, boolean quiet) {
        var data = player.getPersistentDataContainer();
        if (bind.equals("off")) {
            data.remove(bindKey);
        } else {
            data.set(bindKey, PersistentDataType.STRING, bind);
        }
        if (quiet) {
            data.set(quietBindKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            data.remove(quietBindKey);
        }
        lastSneak.remove(player.getUniqueId());
        String message = switch (bind) {
            case DOUBLE_SNEAK -> "Бинд включён: два раза нажми приседание.";
            case SNEAK_SWAP -> "Бинд включён: нажми смену руки, пока приседаешь.";
            default -> "Бинд выключен.";
        };
        player.sendMessage(quiet ? message + " Тихий режим." : message);
    }

    private void reloadSettings(CommandSender sender) {
        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            sender.sendMessage("Нет права " + RELOAD_PERMISSION);
            return;
        }

        YamlConfiguration updated = new YamlConfiguration();
        try {
            updated.load(new File(getDataFolder(), "config.yml"));
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().warning("Could not reload config.yml: " + e.getMessage());
            sender.sendMessage("Не удалось загрузить config.yml. Проверь журнал сервера.");
            return;
        }
        var defaults = settings.getDefaults();
        if (defaults != null) {
            updated.setDefaults(defaults);
        }
        settings = updated;
        sender.sendMessage("Конфиг BurpPlugin обновлён.");
    }

    private void resetGesture(Player player) {
        lastSneak.remove(player.getUniqueId());
        breath.stop(player);
    }

    private void toggleMute(Player player) {
        var data = player.getPersistentDataContainer();
        if (data.has(muteKey, PersistentDataType.BYTE)) {
            data.remove(muteKey);
            player.sendMessage(message("unmuted"));
        } else {
            data.set(muteKey, PersistentDataType.BYTE, (byte) 1);
            player.sendMessage(message("muted"));
        }
    }

    boolean canReceiveBurp(Player listener, Player source) {
        return listener.equals(source) || (listener.canSee(source)
                && !listener.getPersistentDataContainer().has(muteKey, PersistentDataType.BYTE));
    }

    private void burp(Player player, BurpMode mode) {
        long secondsLeft = startCooldown(player.getUniqueId());
        if (secondsLeft > 0) {
            player.sendMessage(message("cooldown").replace("%time%", Long.toString(secondsLeft)));
            return;
        }

        boolean quiet = mode == BurpMode.QUIET;
        double radius = number("radius", 10);
        double radiusMultiplier = quiet ? Math.min(1, number("quiet.radius-multiplier", 0.5)) : 1;
        double soundRange = radiusSquared("sound.radius", radius, radiusMultiplier);
        double chatRange = radiusSquared("messages.radius", radius, radiusMultiplier);
        double breathRange = radiusSquared("breath.radius", radius, radiusMultiplier);
        boolean showBreath = settings.getBoolean("breath.enabled", true);
        double volumeMultiplier = quiet ? Math.min(1, number("quiet.volume-multiplier", 0.35)) : 1;
        float volume = (float) Math.min(Float.MAX_VALUE, number("sound.volume", 1) * volumeMultiplier);
        float pitch = soundPitch(mode);
        Location origin = player.getLocation();
        String chat = message(quiet ? "quiet" : "burp").replace("%player%", player.getName());
        List<Player> viewers = new ArrayList<>();
        int heard = 0;

        for (Player listener : player.getWorld().getPlayers()) {
            boolean self = listener.equals(player);
            if (!canReceiveBurp(listener, player)) {
                continue;
            }

            double distance = listener.getLocation().distanceSquared(origin);
            if (volume > 0 && distance <= soundRange) {
                listener.playSound(player, Sound.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, volume, pitch);
                if (!self) {
                    heard++;
                }
            }
            if (!self && distance <= chatRange) {
                listener.sendMessage(chat);
            }
            if (showBreath && distance <= breathRange) {
                viewers.add(listener);
            }
        }

        player.sendMessage(message("heard").replace("%count%", Integer.toString(heard)));
        if (!viewers.isEmpty()) {
            breath.show(player, viewers, breathRange, mode.particles, settings.getBoolean("breath.green"));
        }
    }

    private float soundPitch(BurpMode mode) {
        double pitch = number("sound.pitch", 1);
        if (mode == BurpMode.DEEP) {
            pitch = number("sound.deep-pitch", 0.65);
        } else if (mode == BurpMode.NORMAL) {
            double variation = Math.min(1.5, number("sound.pitch-variation", 0.05));
            pitch += ThreadLocalRandom.current().nextDouble(-1, 1) * variation;
        }
        return (float) Math.clamp(pitch, 0.5, 2.0);
    }

    private long startCooldown(UUID id) {
        long cooldown = cooldownNanos();
        if (cooldown == 0) {
            return 0;
        }

        long now = System.nanoTime();
        Long previous = cooldowns.get(id);
        if (previous != null) {
            long remaining = cooldown - (now - previous);
            if (remaining > 0) {
                return 1 + TimeUnit.NANOSECONDS.toSeconds(remaining - 1);
            }
        }
        cooldowns.put(id, now);
        return 0;
    }

    private long cooldownNanos() {
        return TimeUnit.SECONDS.toNanos(Math.max(0, settings.getInt("cooldown", 5)));
    }

    private void clearExpiredCooldowns() {
        long now = System.nanoTime();
        long cooldown = cooldownNanos();
        cooldowns.values().removeIf(start -> now - start >= cooldown);
    }

    private double radiusSquared(String path, double fallback, double multiplier) {
        double radius = number(path, fallback) * multiplier;
        return radius * radius;
    }

    private double number(String path, double fallback) {
        double value = settings.getDouble(path, fallback);
        return Double.isFinite(value) && value >= 0 ? value : fallback;
    }

    private String message(String key) {
        String text = settings.getString("messages." + key);
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }
}
