package hex.wald;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class BurpBreath {

    private static final Particle.DustTransition WHITE = new Particle.DustTransition(
            Color.fromRGB(210, 222, 229), Color.fromRGB(244, 247, 250), 0.9f);
    private static final Particle.DustTransition GREEN = new Particle.DustTransition(
            Color.fromRGB(105, 189, 112), Color.fromRGB(220, 241, 223), 0.9f);

    private final BurpPlugin plugin;
    private final Map<UUID, BukkitTask> active = new HashMap<>();

    BurpBreath(BurpPlugin plugin) {
        this.plugin = plugin;
    }

    void show(Player player, List<Player> listeners, double radiusSquared, int particles, boolean green) {
        stop(player);
        World world = player.getWorld();
        Particle.DustTransition color = green ? GREEN : WHITE;

        BukkitTask task = new BukkitRunnable() {
            private int puffs = 4;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !player.getWorld().equals(world)) {
                    stop(player);
                    return;
                }

                Location origin = player.getLocation();
                Location mouth = player.getEyeLocation();
                mouth.add(mouth.getDirection().multiply(0.4)).subtract(0, 0.2, 0);

                for (Player listener : listeners) {
                    if (!listener.isOnline() || !listener.getWorld().equals(world) || !plugin.canReceiveBurp(listener, player)
                            || listener.getLocation().distanceSquared(origin) > radiusSquared) {
                        continue;
                    }

                    listener.spawnParticle(Particle.DUST_COLOR_TRANSITION, mouth, particles,
                            0.07, 0.04, 0.07, 0, color);
                }

                if (--puffs == 0) {
                    stop(player);
                }
            }
        }.runTaskTimer(plugin, 0, 2);
        active.put(player.getUniqueId(), task);
    }

    void stop(Player player) {
        BukkitTask task = active.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
}
