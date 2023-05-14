package me.thatlukinhasguy.autovpnblocker;

import me.thatlukinhasguy.autovpnblocker.Database.Utils.Database;
import me.thatlukinhasguy.autovpnblocker.Database.Utils.REST;
import me.thatlukinhasguy.autovpnblocker.Utils.BukkitConfig;
import okhttp3.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.bukkit.util.StringUtil;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AntiVPNPlugin extends JavaPlugin implements Listener, CommandExecutor {
    public static AntiVPNPlugin plugin = null;
    private static AntiVPNPlugin instance;
    private Set<String> whitelistedPlayers = new HashSet<>();
    private FileConfiguration config = getConfig();
    private REST rest;
    private Database database;
    private BukkitConfig bukkitConfig;

    public static AntiVPNPlugin getInstance() {
        return instance;
    }

    public static void setInstance(AntiVPNPlugin instance) {
        AntiVPNPlugin.instance = instance;
    }

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("antiproxy").setExecutor(this);
        String kickMessage = getConfig().getString("kick-message");
        try {
            Class.forName("org.sqlite.JDBC");

            this.rest.downloadDatabase(this.bukkitConfig);
            this.database = new Database(this.bukkitConfig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String playerIp = event.getAddress().getHostAddress();

        if (config.getBoolean("whitelist." + event.getName())) {
            getLogger().info("[AutoVPNBlocker] O jogador " + event.getName() + " está na whitelist do AntiVPN.");
            return;
        } else if (config.getBoolean("ipwhitelist." + playerIp)) {
            getLogger().info("[AutoVPNBlocker] O IP " + playerIp + " está na whitelist do AntiVPN.");
            return;
        }

        CompletableFuture<Map<String, Boolean>> future = isUsingVPNAsync(playerIp);
        future.thenAccept(vpnInfo -> {
            if (vpnInfo.get("is_vpn") || vpnInfo.get("is_proxy") || vpnInfo.get("is_bogon") || vpnInfo.get("is_tor") || vpnInfo.get("is_abuser") || vpnInfo.get("is_datacenter")) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, getConfig().getString("kick-message"));
            } else if (vpnInfo.get("mobile") || vpnInfo.get("proxy") || vpnInfo.get("hosting")) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, getConfig().getString("kick-message"));
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("antiproxy")) {
            List<String> completions = new ArrayList<>(), commands = Arrays.asList("reload", "addip", "removeip", "adduser", "removeuser");

            if (args.length == 1) StringUtil.copyPartialMatches(args[0], commands, completions);

            completions.sort(null);

            return completions;
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("antiproxy")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.AQUA + "AntiProxy" + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + "Uso inválido. Para ver os argumentos corretos, digite " + ChatColor.GREEN + "/antiproxy help");
                return true;
            }

            if (args[0].equalsIgnoreCase("adduser")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.AQUA + "AntiProxy" + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + "Uso correto: " + ChatColor.GREEN + "/antiproxy adduser <nome>");
                    return true;
                }
                String playerName = args[1];
                plugin.getConfig().set("whitelist." + playerName, true);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.AQUA + "AntiProxy" + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + "O jogador " + ChatColor.GREEN + playerName + ChatColor.GRAY + " foi adicionado à whitelist do AntiVPN");
                return true;
            }


            if (args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                getConfig();
                sender.sendMessage(ChatColor.AQUA + "AntiProxy" + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + "O plugin foi " + ChatColor.GREEN + " recarregado " + ChatColor.GRAY + " com sucesso");
                return true;
            }

            if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(ChatColor.AQUA + "AntiProxy" + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + "Comandos:");
                sender.sendMessage(ChatColor.GREEN + "/antiproxy reload" + ChatColor.GRAY + " - Recarrega a configuração do plugin");
                sender.sendMessage(ChatColor.GREEN + "/antiproxy adduser <nome> " + ChatColor.GRAY + " - Adiciona um jogador à whitelist do AntiVPN");
                sender.sendMessage(ChatColor.GREEN + "/antiproxy removeuser <nome> " + ChatColor.GRAY + " - Remove um jogador da whitelist do AntiVPN");
                sender.sendMessage(ChatColor.GREEN + "/antiproxy addip <ip> " + ChatColor.GRAY + " - Adiciona um IP à whitelist do AntiVPN");
                sender.sendMessage(ChatColor.GREEN + "/antiproxy removeip <ip> " + ChatColor.GRAY + " - Remove um IP da whitelist do AntiVPN");
                sender.sendMessage(ChatColor.GREEN + "/antiproxy help" + ChatColor.GRAY + " - Mostra essa mensagem");
                return true;
            }

            if (args[0].equalsIgnoreCase("removeuser")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.AQUA + "AntiProxy" + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + "Uso correto: " + ChatColor.GREEN + "/antiproxy removeuser <nome>");
                    return true;
                }
                String playerName = args[1];
                if (plugin.getConfig().contains("whitelist." + playerName)) {
                    plugin.getConfig().set("whitelist." + playerName, null);
                    plugin.saveConfig();
                    sender.sendMessage(ChatColor.AQUA + "AntiProxy" + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + "O jogador " + ChatColor.GREEN + playerName + ChatColor.GRAY + " foi removido da whitelist do AntiVPN");
                } else {
                    sender.sendMessage(ChatColor.AQUA + "AntiProxy" + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + "O jogador " + ChatColor.GREEN + playerName + ChatColor.GRAY + " não está na whitelist do AntiVPN");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("addip")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.AQUA + "AntiProxy" + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + "Uso correto: " + ChatColor.GREEN + "/antiproxy addip <ip>");
                    return true;
                }
                String ipAddress = args[1];
                plugin.getConfig().set("ipwhitelist." + ipAddress, true);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.AQUA + "AntiProxy" + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + "O IP " + ChatColor.GREEN + ipAddress + ChatColor.GRAY + " foi adicionado à whitelist do AntiVPN");
                return true;
            }

            if (args[0].equalsIgnoreCase("removeip")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.AQUA + "AntiProxy" + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + "Uso correto: " + ChatColor.GREEN + "/antiproxy removeip <ip>");
                    return true;
                }
                String ipAddress = args[1];
                if (plugin.getConfig().contains("ipwhitelist." + ipAddress)) {
                    plugin.getConfig().set("ipwhitelist." + ipAddress, null);
                    plugin.saveConfig();
                    sender.sendMessage(ChatColor.AQUA + "AntiProxy" + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + "O IP " + ChatColor.GREEN + ipAddress + ChatColor.GRAY + " foi removido da whitelist do AntiVPN");
                } else {
                    sender.sendMessage(ChatColor.AQUA + "AntiProxy" + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + "O IP " + ChatColor.GREEN + ipAddress + ChatColor.GRAY + " não está na whitelist do AntiVPN");
                }
                return true;
            }


            sender.sendMessage(ChatColor.AQUA + "AntiProxy" + ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + "Uso inválido. Para ver os argumentos corretos, digite " + ChatColor.GREEN + "/antiproxy help");
            return true;
        }
        return false;
    }

    private void set(String whitelistedPlayers, Set<String> whitelistedPlayers1) {
    }

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
            .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .build();

    private static final Map<String, Map<String, Boolean>> cache = new HashMap<>();

    private static final ExecutorService httpExecutor = Executors.newFixedThreadPool(10);

    private CompletableFuture<Map<String, Boolean>> isUsingVPNAsync(String ip) {
        Map<String, Boolean> cachedResult = cache.get(ip);
        if (cachedResult != null) {
            return CompletableFuture.completedFuture(cachedResult);
        }

        HttpUrl url = HttpUrl.parse("https://api.incolumitas.com/").newBuilder()
                .addQueryParameter("q", ip)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();

        return CompletableFuture.supplyAsync(() -> {
            Response response = null;
            Response response1 = null;

            try {
                response = httpClient.newCall(request).execute();

                if (!response.isSuccessful()) {
                    getLogger().warning("Parece que a API do Incolumitas não está funcionando (código de resposta: " + response.code() + ")");
                    return Collections.emptyMap();
                }


                String responseBody = response.body().string();
                JSONObject obj = new JSONObject(responseBody);
                Map<String, Boolean> vpnInfo = new HashMap<>();
                vpnInfo.put("is_tor", obj.getBoolean("is_tor"));
                vpnInfo.put("is_abuser", obj.getBoolean("is_abuser"));
                vpnInfo.put("is_bogon", obj.getBoolean("is_bogon"));
                vpnInfo.put("is_datacenter", obj.getBoolean("is_datacenter"));
                vpnInfo.put("is_proxy", obj.getBoolean("is_proxy"));
                vpnInfo.put("is_vpn", obj.getBoolean("is_vpn"));

                cache.put(ip, vpnInfo);
                return vpnInfo;
            } catch (IOException e) {
                e.fillInStackTrace();
            }
            return null;
        });

    }

    @Override
    public void onDisable() {}

    public BukkitConfig getBukkitConfig() {
        return this.bukkitConfig;
    }

    public REST getRest() {
        return this.rest;
    }

    public Database getDb() {
        return this.database;
    }

}