package com.litesuggar.fgateclient.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 配置管理器 - 统一管理所有配置项
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    // WebSocket 配置
    private String websocketUrl;
    private String websocketToken;

    // RCON 配置
    private boolean useBuiltinRcon;
    private String rconHost;
    private int rconPort;
    private String rconPassword;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // 加载 WebSocket 配置
        websocketUrl = config.getString("websocket.url");
        websocketToken = config.getString("websocket.token");

        // 加载 RCON 配置
        useBuiltinRcon = config.getBoolean("rcon.use-builtin", true);
        rconHost = config.getString("rcon.host", "localhost");
        rconPort = config.getInt("rcon.port", 25575);
        rconPassword = config.getString("rcon.password", "");
    }

    public void validateConfig() throws IllegalArgumentException {
        if (websocketUrl == null || websocketUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("WebSocket URL can't be null!");
        }
        if (websocketToken == null || websocketToken.trim().isEmpty()) {
            throw new IllegalArgumentException("WebSocket Token can't be null!");
        }
        if (!useBuiltinRcon && (rconPassword == null || rconPassword.trim().isEmpty())) {
            throw new IllegalArgumentException("Remote RCON token can't be null!");
        }
    }

    // WebSocket 配置的 getter 方法
    public String getWebsocketUrl() {
        return websocketUrl;
    }

    public String getWebsocketToken() {
        return websocketToken;
    }

    // RCON 配置的 getter 方法
    public boolean isUseBuiltinRcon() {
        return useBuiltinRcon;
    }

    public String getRconHost() {
        return rconHost;
    }

    public int getRconPort() {
        return rconPort;
    }

    public String getRconPassword() {
        return rconPassword;
    }

    public boolean isRconConfigured() {
        return rconPassword != null && !rconPassword.isEmpty();
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
