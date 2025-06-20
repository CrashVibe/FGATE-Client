package com.crashvibe.fgateclient.listeners;

import com.google.gson.JsonObject;
import com.crashvibe.fgateclient.FGateClient;
import com.crashvibe.fgateclient.config.ConfigManager;
import com.crashvibe.fgateclient.manager.ServiceManager;
import com.crashvibe.fgateclient.service.WebSocketManager;
import com.crashvibe.fgateclient.utils.I18n;
import com.crashvibe.fgateclient.utils.TextUtil;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.HashMap;
import java.util.Map;

public class OnJoin implements Listener {
    private final FGateClient plugin;

    public OnJoin(FGateClient plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        ServiceManager serviceManager = plugin.getServiceManager();
        if (serviceManager == null) {
            // 如果 ServiceManager 还没有初始化，允许玩家进入
            return;
        }

        WebSocketManager webSocketManager = serviceManager.getWebSocketManager();
        ConfigManager configManager = serviceManager.getConfigManager();
        I18n i18n = serviceManager.getI18n();

        boolean allowJoinWithoutWebSocket = configManager.isAllowJoinWithoutWebSocket();
        String playerName = event.getPlayer().getName();
        String playerIP = event.getAddress() != null ? event.getAddress().getHostAddress() : null;
        String uuid = event.getPlayer().getUniqueId().toString();
        long timestamp = System.currentTimeMillis();

        if (webSocketManager == null || !webSocketManager.isConnected()) {
            if (!allowJoinWithoutWebSocket) {
                Map<String, String> params = new HashMap<>();
                params.put("player", playerName);

                String msg = i18n.format("not_ready", params, event.getPlayer());
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text(msg));

                plugin.getLogger().warning(i18n.format("websocket_denied", params, event.getPlayer()));
            }
            return;
        }

        // 构建参数并发送请求
        JsonObject paramsJson = new JsonObject();
        paramsJson.addProperty("player", playerName);
        paramsJson.addProperty("uuid", uuid);
        paramsJson.addProperty("ip", playerIP);
        paramsJson.addProperty("timestamp", timestamp);

        JsonObject response = null;
        try {
            // 检查连接状态
            if (!webSocketManager.isConnected()) {
                plugin.getLogger().warning("WebSocket not connected when trying to send player.join for " + playerName);
                return;
            }
            response = webSocketManager.sendRequestAsync("player.join", paramsJson).get(5,
                    java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send player.join request for " + playerName + ": " + e.getMessage());
        }

        // 处理响应
        if (response != null && response.has("result")) {
            JsonObject result = response.getAsJsonObject("result");
            String action = result.has("action") ? result.get("action").getAsString() : "ignore";

            if ("kick".equalsIgnoreCase(action)) {
                String reason = result.has("reason") ? result.get("reason").getAsString()
                        : i18n.get("kick_reason", event.getPlayer());

                Map<String, String> logParams = new HashMap<>();
                logParams.put("player", playerName);
                logParams.put("reason", reason);

                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, TextUtil.parseText(reason));
                plugin.getLogger().info(i18n.format("player_kicked", logParams, event.getPlayer()));
            }

        } else if (response != null && response.has("error")) {
            String errorMsg = response.get("error").toString();
            Map<String, String> errParams = new HashMap<>();
            errParams.put("error", errorMsg);

            plugin.getLogger().warning(i18n.format("player_join_error", errParams, event.getPlayer()));
        } else if (response == null) {
            // WebSocket 请求超时或失败
            plugin.getLogger().warning("Player join request timeout or failed for player: " + playerName);
        }
    }
}
