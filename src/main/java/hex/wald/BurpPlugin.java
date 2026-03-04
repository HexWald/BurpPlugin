package hex.wald;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class BurpPlugin extends JavaPlugin {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {

        saveDefaultConfig();

        getLogger().info("BurpPlugin enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        int cooldown = getConfig().getInt("cooldown");

        if (cooldowns.containsKey(player.getUniqueId())) {

            long secondsLeft = ((cooldowns.get(player.getUniqueId()) / 1000) + cooldown) - (System.currentTimeMillis() / 1000);

            if (secondsLeft > 0) {

                String msg = getConfig().getString("messages.cooldown")
                        .replace("%time%", String.valueOf(secondsLeft));

                player.sendMessage(color(msg));
                return true;
            }
        }

        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        double radius = getConfig().getDouble("radius");
        float volume = (float) getConfig().getDouble("sound.volume");
        float pitch = (float) getConfig().getDouble("sound.pitch");

        int heard = 0;

        for (Player p : Bukkit.getOnlinePlayers()) {

            if (p.getWorld().equals(player.getWorld())
                    && p.getLocation().distance(player.getLocation()) <= radius) {

                p.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, volume, pitch);
                heard++;
            }
        }

        String chatMsg = getConfig().getString("messages.burp")
                .replace("%player%", player.getName());

        Bukkit.broadcastMessage(color(chatMsg));

        String heardMsg = getConfig().getString("messages.heard")
                .replace("%count%", String.valueOf(heard));

        player.sendMessage(color(heardMsg));

        return true;
    }

    private String color(String text) {
        return text.replace("&", "§");
    }
}