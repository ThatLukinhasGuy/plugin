package me.thatlukinhasguy.autovpnblocker.DBListener;

import me.thatlukinhasguy.autovpnblocker.AntiVPNPlugin;
import me.thatlukinhasguy.autovpnblocker.Database.BlockTypes.BusinessBlock;
import me.thatlukinhasguy.autovpnblocker.Database.BlockTypes.CloudGamingBlock;
import me.thatlukinhasguy.autovpnblocker.Database.BlockTypes.VPNBlock;
import me.thatlukinhasguy.autovpnblocker.Database.BlockTypes.HostingBlock;
import me.thatlukinhasguy.autovpnblocker.Database.BlacklistProvider;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerLoginEvent;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.List;

import static org.bukkit.Bukkit.getLogger;

public class LoginListener {
    private final AntiVPNPlugin bukkit = AntiVPNPlugin.getInstance();

    private FileConfiguration loadedConfig = null;

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) throws SQLException {
        Player player = event.getPlayer();

        String ipAddress = event.getAddress().getHostAddress();

        String playerName = event.getPlayer().getName();

        JSONObject ipData = this.bukkit.getDb().checkIp(event.getAddress().getHostAddress());

        BusinessBlock businessBlock = new BusinessBlock(ipData, this.bukkit.getBukkitConfig());

        if (this.bukkit.getConfig().getBoolean("ipwhitelist." + ipAddress)) {
            getLogger().info("[YAAntiVPN] The IP " + ipAddress + " is in the AntiVPN whitelist.");
            return;
        } else if (this.bukkit.getConfig().getBoolean("whitelist." + playerName)) {
            getLogger().info("[YAAntiVPN] The player " + playerName + " is in the AntiVPN whitelist.");
            return;
        }

        if (businessBlock.blockAddress()) {
            businessBlock.kickPlayer(event);
            businessBlock.alertStaff(event, this.bukkit.getServer().getOnlinePlayers());

            return;
        } else if (this.loadedConfig.getBoolean("Database.Enabled")) {
            return;
        }

        CloudGamingBlock cloudGamingBlock = new CloudGamingBlock(ipData, this.bukkit.getBukkitConfig());

        if (cloudGamingBlock.blockAddress()) {
            businessBlock.kickPlayer(event);
            businessBlock.alertStaff(event, this.bukkit.getServer().getOnlinePlayers());

            return;
        } else if (this.loadedConfig.getBoolean("Database.Enabled")) {
            return;
        }

        HostingBlock hostingBlock = new HostingBlock(ipData, this.bukkit.getBukkitConfig());

        if (hostingBlock.blockAddress()) {
            businessBlock.kickPlayer(event);
            businessBlock.alertStaff(event, this.bukkit.getServer().getOnlinePlayers());

            return;
        } else if (this.loadedConfig.getBoolean("Database.Enabled")) {
            return;
        }

        VPNBlock vpnBlock = new VPNBlock(ipData, this.bukkit.getBukkitConfig());

        if (vpnBlock.blockAddress()) {
            businessBlock.kickPlayer(event);
            businessBlock.alertStaff(event, this.bukkit.getServer().getOnlinePlayers());

            return;
        } else if (this.loadedConfig.getBoolean("Database.Enabled")) {
            return;
        }

        BlacklistProvider countryBlacklist = this.bukkit.getBukkitConfig().getCountryBlacklist();

        if (countryBlacklist.getList().contains(ipData.getString("country"))) {
            List<String> list = countryBlacklist.getMessage();

            String msg = String.join("\n", list);

            msg = msg.replace("{COUNTRY}", ipData.getString("country"));

            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.translateAlternateColorCodes('&', msg));

            for (Player online : this.bukkit.getServer().getOnlinePlayers()) {
                if (online.hasPermission("yaantivpn.notify")) {
                    for (String line : countryBlacklist.getAlert()) {
                        String notifString = ChatColor.translateAlternateColorCodes('&', line);

                        notifString = notifString.replace("{USERNAME}", player.getName());
                        notifString = notifString.replace("{COUNTRY}", ipData.getString("country"));

                        online.sendMessage(notifString);
                    }
                }
            }

            return;
        } else if (this.loadedConfig.getBoolean("Database.Enabled")) {
            return;
        }

        BlacklistProvider ispBlacklist = this.bukkit.getBukkitConfig().getIspBlacklist();

        if (ispBlacklist.getList().contains(ipData.getString("isp"))) {
            List<String> list = ispBlacklist.getMessage();

            String msg = String.join("\n", list);

            msg = msg.replace("{ISP}", ipData.getString("isp"));

            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.translateAlternateColorCodes('&', msg));

            for (Player online : this.bukkit.getServer().getOnlinePlayers()) {
                if (online.hasPermission("yaantivpn.notify")) {
                    for (String line : ispBlacklist.getAlert()) {
                        String notifString = ChatColor.translateAlternateColorCodes('&', line);

                        notifString = notifString.replace("{USERNAME}", player.getName());
                        notifString = notifString.replace("{ISP}", ipData.getString("isp"));

                        online.sendMessage(notifString);
                    }
                }
            }
        }
    }
}
