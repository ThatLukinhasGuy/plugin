package me.thatlukinhasguy.autovpnblocker.Database.BlockTypes;

import me.thatlukinhasguy.autovpnblocker.Database.BlockProvider;
import me.thatlukinhasguy.autovpnblocker.Utils.BukkitConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;
import org.json.JSONObject;
import org.bukkit.ChatColor;

import java.util.Collection;
import java.util.List;

public class BusinessBlock {
    private final JSONObject ipData;

    private BlockProvider blockProvider = null;


    public BusinessBlock(JSONObject ipData, BukkitConfig bukkitConfig) {

        this.ipData = ipData;
        this.blockProvider = blockProvider;
    }

    public boolean blockAddress() {
        return this.blockProvider.isEnabled() && this.ipData.getJSONArray("types").toList().contains(2);
    }

    public void kickPlayer(PlayerLoginEvent event) {
        List<String> list = blockProvider.getMessage();

        String msg = String.join("\n", list);

        msg = msg.replace("{IP}", event.getAddress().getHostAddress());

        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, org.bukkit.ChatColor.translateAlternateColorCodes('&', msg));
    }

    public void alertStaff(PlayerLoginEvent event, Collection players) {
        for (Object online : players) {
            Player plyr = (Player) online;

            if (plyr.hasPermission("yaantivpn.notify")) {
                for (String line : this.blockProvider.getAlert()) {
                    String notifString = ChatColor.translateAlternateColorCodes('&', line);

                    notifString = notifString.replace("{USERNAME}", event.getPlayer().getName());
                    notifString = notifString.replace("{IP}", event.getAddress().getHostAddress());

                    plyr.sendMessage(notifString);
                }
            }
        }
    }
}