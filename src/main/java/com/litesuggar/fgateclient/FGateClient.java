package com.litesuggar.fgateclient;

import com.litesuggar.fgateclient.listeners.OnJoin;
import com.litesuggar.fgateclient.utils.EventUtil;
import com.tcoded.folialib.FoliaLib;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class FGateClient extends JavaPlugin implements Listener {

    private WebSocketManager webSocketManager;
    private RconManager rconManager;
    private FileConfiguration config;
    public final FoliaLib foliaLib = new FoliaLib(this);;
    public static final Logger LOGGER = LogManager.getLogger();
    private static FGateClient instance;

    @Override
    public void onLoad() {
        instance = this;
        LOGGER.info("FGateClient plugin is loading...");
    }

    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();
        config = getConfig();

        new Metrics(this, 26085);
        LOGGER.info("bStats Hook Enabled!");

        rconManager = new RconManager(this); // 初始化 RCON 管理器

        webSocketManager = new WebSocketManager(this, rconManager); // 初始化WebSocket管理器

        getServer().getPluginManager().registerEvents(this, this); // 注册事件监听器

        initListeners();

        // 异步连接WebSocket
        foliaLib.getScheduler().runAsync(task -> {
                try {
                    webSocketManager.connect();
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to connect to remote server", e);
                }
            }
        );

        getLogger().info("RemoteClient plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (webSocketManager != null) {
            webSocketManager.disconnect();
        }
        if (rconManager != null) {
            rconManager.close();
        }
        getLogger().info("RemoteClient plugin disabled!");
    }

    public RconManager getRconManager() {
        return rconManager;
    }

    public WebSocketManager getWebSocketManager() {
        return webSocketManager;
    }

    public void kickPlayer(String playerIdentifier, String reason) {
        foliaLib.getScheduler().runAsync(task-> {
            var player = Bukkit.getPlayer(playerIdentifier);
            if (player == null) {
                // 尝试通过UUID查找
                try {
                    var uuid = java.util.UUID.fromString(playerIdentifier);
                    player = Bukkit.getPlayer(uuid);
                } catch (IllegalArgumentException ignored) {
                    // 不是有效的UUID，可能是玩家名
                }
            }

        if (player != null && player.isOnline()) {
            // 在主线程执行踢出操作
            org.bukkit.entity.Player finalPlayer = player;
            foliaLib.getScheduler().runNextTick(kickTask -> {
                finalPlayer.kick(net.kyori.adventure.text.Component.text(reason));
                getLogger().info("Kicked player " + finalPlayer.getName() + " with reason: " + reason);
            });
        } else {
            getLogger().warning("Could not find online player: " + playerIdentifier);
        }
        });
    }

   @Override
   public @NotNull FileConfiguration getConfig() {
       if (config == null) {
           config = super.getConfig();
       }
       return config;
   }

    private void initListeners() {
        EventUtil.registerEvents(
                new OnJoin(this)
        );
    }

    public static FGateClient getInstance() {
        return instance;
    }

}