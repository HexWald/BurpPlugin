package hex.wald;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class BurpPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("BurpPlugin enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        double radius = 10;
        int heard = 0;

        for (Player p : Bukkit.getOnlinePlayers()) {

            if (p.getWorld().equals(player.getWorld())
                    && p.getLocation().distance(player.getLocation()) <= radius) {

                p.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);
                heard++;
            }
        }

        player.sendMessage("§eВы рыгнули. Услышали игроков: §6" + heard);

        return true;
    }
}