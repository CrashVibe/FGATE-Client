package com.litesuggar.fgateclient.listeners;

import com.google.gson.JsonObject;
import com.litesuggar.fgateclient.FGateClient;
import com.litesuggar.fgateclient.service.WebSocketManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class OnJoin implements Listener {
    private final FGateClient plugin;

    public OnJoin(FGateClient plugin) {
        this.plugin = plugin;
    }

    private void sendPlayerJoinEvent(WebSocketManager ws, String playerName, String uuid, long timestamp) {
        JsonObject params = new JsonObject();
        params.addProperty("player", playerName);
        params.addProperty("uuid", uuid);
        params.addProperty("timestamp", timestamp);

        JsonObject notification = new JsonObject();
        notification.addProperty("jsonrpc", "2.0");
        notification.addProperty("method", "player.join");
        notification.add("params", params);

        ws.send(notification);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.foliaLib.getScheduler().runAsync(task -> {
            try {
                WebSocketManager webSocketManager = plugin.getServiceManager().getWebSocketManager();
                if (webSocketManager != null && webSocketManager.isConnected()) {
                    plugin.getLogger().info("发送玩家加入事件: " + event.getPlayer().getName());
                    this.sendPlayerJoinEvent(
                            webSocketManager,
                            event.getPlayer().getName(),
                            event.getPlayer().getUniqueId().toString(),
                            System.currentTimeMillis());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("发送玩家加入事件失败：" + e.getMessage());
            }
        });
    }
}
