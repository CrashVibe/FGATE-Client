package com.litesuggar.fgateclient.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import com.litesuggar.fgateclient.FGateClient;
import com.litesuggar.fgateclient.service.WebSocketManager;

public class OnJoin implements Listener {
    private final FGateClient plugin;

    public OnJoin(FGateClient plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.foliaLib.getScheduler().runAsync(task -> {
            try {
                WebSocketManager webSocketManager = plugin.getServiceManager().getWebSocketManager();
                if (webSocketManager != null && webSocketManager.isConnected()) {
                    plugin.getLogger().info("发送玩家加入事件: " + event.getPlayer().getName());
                    webSocketManager.sendPlayerJoinEvent(
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
