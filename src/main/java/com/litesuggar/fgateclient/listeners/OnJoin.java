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

    private void sendPlayerJoinEvent(WebSocketManager ws, String playerName, String uuid, String ip, long timestamp) {
        JsonObject params = new JsonObject();
        params.addProperty("player", playerName);
        params.addProperty("uuid", uuid);
        params.addProperty("ip", ip);
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
                    plugin.getLogger().info("Sending player join event: " + event.getPlayer().getName());

                    String playerIP = event.getPlayer().getAddress() != null
                            ? event.getPlayer().getAddress().getAddress().getHostAddress()
                            : null;

                    this.sendPlayerJoinEvent(
                            webSocketManager,
                            event.getPlayer().getName(),
                            event.getPlayer().getUniqueId().toString(),
                            playerIP,
                            System.currentTimeMillis());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send player join event：" + e.getMessage());
            }
        });
    }
}
